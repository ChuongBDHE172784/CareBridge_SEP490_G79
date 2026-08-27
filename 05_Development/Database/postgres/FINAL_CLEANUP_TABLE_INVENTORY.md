---
title: Final Cleanup Repository and Live Table Inventory
baseline_commit: 2b33b582
reconciliation_baseline_commit: 0e36f6ee
live_audit_date: 2026-07-22
status: evidence-baseline
---

# Final Cleanup Repository and Live Table Inventory

## Count reconciliation at HEAD

Counting is restricted to schema `public` and relation kinds `r` (ordinary
base table) and `p` (partitioned table). Views, materialized views, foreign
tables, sequences, indexes, and TOAST relations are excluded.
`flyway_schema_history` is included exactly once.

| Name set | Count | Derivation |
| --- | ---: | --- |
| Live current | 127 | Exact `Live rows != absent` names below; read-only live audit |
| Clean bootstrap HEAD | 99 | 113 pre-Final `Repo = Yes` names minus 14 Final Cleanup tables present on clean bootstrap |
| Expected live after all pending migrations | 104 | 127 - 10 Batch 1-5 drops + 4 Batch 4-5 creates - 17 Final Cleanup drops |

The clean 99 consists of 98 domain tables plus `flyway_schema_history`.
Therefore 100 is a double-counting artifact: it adds Flyway history to a total
that already includes it. The live-after-upgrade total is five higher than
clean bootstrap because `baby_journey_link_cleanup_summary`, `districts`,
`provinces`, `specialties`, and `wards` are retained live-only tables.

Exact set differences by table name:

- `live_current - clean_bootstrap` (32): `baby_journey_link_cleanup_summary`,
  `commission_config`, `commission_records`, `consultation_disputes`,
  `consultation_messages`, `consultation_requests`, `contribution_attachments`,
  `districts`, `emergency_events`, `expert_identity_verifications`,
  `expert_reviews`, `expert_verification_documents`, `hospitals`,
  `impact_assessment_ratings`, `medical_contributions`, `notifications`,
  `partner_expert_links`, `partner_services`, `payment_transactions`,
  `provinces`, `refund_records`, `roles`, `safety_alerts`, `safety_events`,
  `safety_monitoring_settings`, `settlement_records`, `specialties`,
  `sponsored_campaigns`, `triage_answers`, `triage_assessments`, `user_roles`,
  `wards`.
- `clean_bootstrap - live_current` (4): `care_facility_legacy_ids`,
  `emergency_alert_attempts`, `emergency_alert_deliveries`,
  `safety_event_responses`.
- `expected_live_after_upgrade - clean_bootstrap` (5):
  `baby_journey_link_cleanup_summary`, `districts`, `provinces`, `specialties`,
  `wards`.
- `clean_bootstrap - expected_live_after_upgrade`: empty.

The inventory rows below remain the 131-name evidence union captured before
Final Cleanup. `Repo` is the 113-table post-Batch-5/pre-Final snapshot, not the
99-table final HEAD set; the derivation above prevents those two snapshots from
being conflated.

## Decision baseline

Inventory này là union lịch sử của **131** tên bảng: **127** bảng live hiện tại và **113** bảng clean-bootstrap tại snapshot sau Batch 5/trước Final Cleanup (112 domain + `flyway_schema_history`). Clean-bootstrap tại HEAD sau Final Cleanup là **99** bảng như phần reconciliation phía trên. Có 4 bảng repository-only do Batch 4–5 chưa deploy và 18 bảng live-only/pre-Batch drift. Live audit dùng JDBC `REPEATABLE READ, READ ONLY`, rollback rõ ràng; fingerprint `4e414b1bbc88a5fef9bbea73e83d7bcac2d2d6cf10a70cb0d23c216e462d5a7c` không đổi.

- Approved Final Cleanup: 17 bảng, tất cả live rows = 0.
- Blocked: 9 bảng có data/dependency; bảng thứ chín là `health_summaries` vì UC-88 đang dùng booking dependency sai dù bản thân bảng phải giữ Release 1.
- Reconciled counts: live 127 → 121 sau Batch 1–5 → 104 sau Final Cleanup; clean bootstrap snapshot 113 → HEAD 99. Tất cả các số này đều tính `flyway_schema_history` đúng một lần.
- Catalog: toàn bộ 127 live tables có RLS policy count = 0. View/matview/trigger/routine/cross-schema scan không tìm hidden dependency vào approved 17. Các lexical match `roles` trong system/realtime function đã được refute bằng catalog dependency.

## Classification rules

Mỗi row có đúng một taxonomy: `KEEP_RELEASE1`, `KEEP_CROSS_CUTTING`, `DROP_OUT_OF_SCOPE`, `DROP_UNUSED_SCHEMA`, `PARTIAL_RUNTIME`, `NEEDS_REVIEW`, `BLOCKED_BY_DATA`, hoặc `BLOCKED_BY_DEPENDENCY`. `Repo` phản ánh schema sau Batch 1–5 ở HEAD; `Live rows` phản ánh live chưa deploy các batch đó.

## Full inventory

| Table | Domain / UC | Classification | Repo | Live rows | FK out/in | RLS | Runtime evidence | Retention / decision |
|---|---|---|:---:|---:|:---:|---:|---|---|
| `account_deletion_requests` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `audit_logs` | cross-cutting; CC-01 | KEEP_CROSS_CUTTING | Yes | 4605 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `baby_daily_logs` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 17 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `baby_journey_link_cleanup_summary` | baby; MF-03 / UC-32–45 | KEEP_CROSS_CUTTING | No | 1 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `baby_link_submissions` | retired baby/Mother Journey command | REMOVE_RETIRED | No | 3 (legacy snapshot) | 0/0 | 0 | Không còn entity/repository/service runtime | Không giữ operational table; chỉ bảo toàn audit lịch sử đã tối thiểu hóa |
| `baby_profiles` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 14 | 2/11 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `care_facilities` | nearby/emergency; MF-07 / UC-77–82 | KEEP_RELEASE1 | Yes | 5 | 1/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `care_facility_legacy_ids` | nearby/emergency; MF-07 / UC-77–82 | KEEP_RELEASE1 | Yes | absent | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `care_group_members` | family; MF-10 / UC-94–101 | KEEP_RELEASE1 | Yes | 8 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `care_groups` | family; MF-10 / UC-94–101 | KEEP_RELEASE1 | Yes | 6 | 3/2 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `care_tasks` | reminder/care-plan; MF-09 / UC-89–93 | KEEP_RELEASE1 | Yes | 0 | 3/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `checklist_items` | reminder/care-plan; MF-09 / UC-89–93 | KEEP_RELEASE1 | Yes | 1 | 1/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `checklist_templates` | reminder/care-plan; MF-09 / UC-89–93 | KEEP_RELEASE1 | Yes | 1 | 0/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `commission_config` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 1/0 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `commission_records` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 1/1 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `community_answer_likes` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 8 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_answers` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 23 | 1/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_bookmarks` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 6 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_profiles` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 5 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_question_likes` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 10 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_questions` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 16 | 1/4 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `community_topics` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 15 | 1/3 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `consent_grants` | shared; cross-domain | KEEP_CROSS_CUTTING | Yes | 2 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `consultation_bookings` | V2 consultation/payment; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 1 | 4/4 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `consultation_disputes` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 3/1 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `consultation_messages` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 2/0 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `consultation_price_bands` | V2 consultation/payment; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 1 | 1/1 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `consultation_requests` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 0/0 | 0 | V2 runtime hiện có; phải xóa ở task code sau | 0 rows; không có retention obligation đã tìm thấy |
| `consultation_sessions` | V2 consultation/payment; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 1 | 1/1 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `content_items` | content/moderation; MF-04, MF-11 / UC-54–59,102–108 | KEEP_RELEASE1 | Yes | 24 | 0/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `content_reports` | content/moderation; MF-04, MF-11 / UC-54–59,102–108 | KEEP_RELEASE1 | Yes | 10 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `content_sources` | content/moderation; MF-04, MF-11 / UC-54–59,102–108 | KEEP_RELEASE1 | Yes | 23 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `contribution_attachments` | expert; MF-05 / UC-60–71 | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Không có Release-1 runtime; dormant/entity-only | 0 rows; không có retention obligation đã tìm thấy |
| `contribution_points` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `conversation_calls` | V2 realtime; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 43 | 2/0 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `data_permissions` | cross-cutting; CC-01 | PARTIAL_RUNTIME | Yes | 0 | 0/0 | 0 | Release-1 flow tồn tại nhưng coverage/consent enforcement chưa hoàn chỉnh | Release-1 operational/history |
| `development_milestones` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `device_measurements` | shared; cross-domain | KEEP_RELEASE1 | Yes | 0 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `device_tokens` | shared; cross-domain | KEEP_RELEASE1 | Yes | 16 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `direct_conversations` | V2 realtime; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 5 | 2/2 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `direct_messages` | V2 realtime; MF-15 / V2 | BLOCKED_BY_DATA | Yes | 32 | 2/0 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `districts` | reference/geography; MF-05/MF-07 lookup | KEEP_RELEASE1 | No | 399 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `emergency_alert_attempts` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | absent | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `emergency_alert_deliveries` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | absent | 2/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `emergency_contacts` | shared; cross-domain | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `emergency_events` | shared; cross-domain | DROP_UNUSED_SCHEMA | No | 0 | 2/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `emergency_map_handoffs` | nearby/emergency; MF-07 / UC-77–82 | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `emergency_sessions` | nearby/emergency; MF-07 / UC-77–82 | KEEP_RELEASE1 | Yes | 4 | 2/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `evidence_source_review_log` | triage/evidence; MF-06 / UC-72–76 | KEEP_CROSS_CUTTING | Yes | 0 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `evidence_sources` | triage/evidence; MF-06 / UC-72–76 | KEEP_RELEASE1 | Yes | 10 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `exercise_safety_checks` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 0 | 3/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `exercise_sessions` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 0 | 4/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expenses` | expense; MF-12 / UC-109–111 | KEEP_RELEASE1 | Yes | 0 | 3/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expert_availability` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | Yes | 2 | 0/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expert_consultation_prices` | expert; MF-05 / UC-60–71 | BLOCKED_BY_DATA | Yes | 1 | 1/1 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `expert_credentials` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | Yes | 3 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expert_identity_verifications` | expert; MF-05 / UC-60–71 | DROP_UNUSED_SCHEMA | No | 0 | 2/0 | 0 | Không có Release-1 runtime; dormant/entity-only | 0 rows; không có retention obligation đã tìm thấy |
| `expert_location_shares` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expert_profiles` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | Yes | 2 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `expert_reviews` | expert; MF-05 / UC-60–71 | DROP_OUT_OF_SCOPE | Yes | 0 | 2/0 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `expert_verification_documents` | expert; MF-05 / UC-60–71 | DROP_UNUSED_SCHEMA | Yes | 0 | 1/0 | 0 | Không có Release-1 runtime; dormant/entity-only | 0 rows; không có retention obligation đã tìm thấy |
| `family_alert_log` | nearby/emergency; MF-07 / UC-77–82 | KEEP_RELEASE1 | Yes | 3 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `flyway_schema_history` | cross-cutting; CC-01 | KEEP_CROSS_CUTTING | Yes | 119 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `growth_measurements` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 5 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `health_device_connections` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | KEEP_RELEASE1 | Yes | 0 | 1/2 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `health_memory_entries` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `health_record_files` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `health_records` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | KEEP_RELEASE1 | Yes | 0 | 3/2 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `health_summaries` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | BLOCKED_BY_DEPENDENCY | Yes | 0 | 3/1 | 0 | UC-88 đang phụ thuộc booking sai; phải thay bằng consent trước | Giữ đến khi UC-88 consent cutover hoàn tất |
| `hospitals` | reference/geography; MF-05/MF-07 lookup | DROP_UNUSED_SCHEMA | No | 20 | 0/1 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `impact_assessment_ratings` | shared; cross-domain | DROP_UNUSED_SCHEMA | Yes | 0 | 0/0 | 0 | Không có Release-1 runtime; dormant/entity-only | 0 rows; không có retention obligation đã tìm thấy |
| `imu_monitoring_sessions` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | 2 | 2/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `imu_safety_events` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | 1 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `intake_sessions` | triage/evidence; MF-06 / UC-72–76 | KEEP_RELEASE1 | Yes | 24 | 3/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `location_snapshots` | shared; cross-domain | KEEP_RELEASE1 | Yes | 0 | 1/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `maternal_health_metrics` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 57 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `medical_contributions` | shared; cross-domain | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Không có Release-1 runtime; dormant/entity-only | 0 rows; không có retention obligation đã tìm thấy |
| `moderation_actions` | content/moderation; MF-04, MF-11 / UC-54–59,102–108 | KEEP_RELEASE1 | Yes | 14 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `mother_baseline_contexts` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 2 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `mother_journey_transitions` | mother; MF-02 / UC-19–31 | KEEP_CROSS_CUTTING | Yes | 22 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `mother_journeys` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 23 | 1/11 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `nearby_support_requests` | nearby/emergency; MF-07 / UC-77–82 | PARTIAL_RUNTIME | Yes | 0 | 0/1 | 0 | Release-1 flow tồn tại nhưng coverage/consent enforcement chưa hoàn chỉnh | Release-1 operational/history |
| `nearby_support_responses` | nearby/emergency; MF-07 / UC-77–82 | PARTIAL_RUNTIME | Yes | 0 | 1/0 | 0 | Release-1 flow tồn tại nhưng coverage/consent enforcement chưa hoàn chỉnh | Release-1 operational/history |
| `notification_preferences` | shared; cross-domain | KEEP_RELEASE1 | Yes | 0 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `notification_records` | shared; cross-domain | KEEP_RELEASE1 | Yes | 30 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `notifications` | shared; cross-domain | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `otp_verifications` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 66 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `partner_expert_links` | V2 partner; MF-16 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 2/0 | 0 | V2 runtime hiện có; phải xóa ở task code sau | 0 rows; không có retention obligation đã tìm thấy |
| `partner_organizations` | V2 partner; MF-16 / V2 | BLOCKED_BY_DATA | Yes | 1 | 0/4 | 0 | V2 runtime/data cluster; giữ recoverable | Giữ data; cần archive/owner decision |
| `partner_services` | V2 partner; MF-16 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 1/0 | 0 | V2 runtime hiện có; phải xóa ở task code sau | 0 rows; không có retention obligation đã tìm thấy |
| `password_reset_tokens` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 1 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `payment_transactions` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 2/2 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `postpartum_logs` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 1 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `posture_analysis_configs` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 0 | 2/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `posture_feedback_events` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `pregnancy_exercises` | mother; MF-02 / UC-19–31 | KEEP_RELEASE1 | Yes | 0 | 1/3 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `pregnancy_outcome_evidence` | mother; MF-02 / UC-19–31 | KEEP_CROSS_CUTTING | Yes | 2 | 0/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `privacy_settings` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 1 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `provinces` | reference/geography; MF-05/MF-07 lookup | KEEP_RELEASE1 | No | 36 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `question_notification_mutes` | community; MF-04 / UC-46–59 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| Admin medical warning catalog | retired from triage administration | DROP_V7 | No | 0 | 0/0 | 0 | Removed with its controller/service/repository stack | Emergency routing remains repository-free in runtime policy |
| `refresh_tokens` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 923 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `refund_records` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 3/0 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `reminders` | reminder/care-plan; MF-09 / UC-89–93 | KEEP_RELEASE1 | Yes | 20 | 3/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `roles` | identity; MF-01 / UC-01–18 | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `safety_alerts` | safety; MF-14 / UC-116–121 | DROP_UNUSED_SCHEMA | No | 0 | 3/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `safety_event_responses` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | absent | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `safety_events` | safety; MF-14 / UC-116–121 | DROP_UNUSED_SCHEMA | No | 0 | 3/1 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `safety_monitoring_config` | safety; MF-14 / UC-116–121 | KEEP_RELEASE1 | Yes | 1 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `safety_monitoring_settings` | safety; MF-14 / UC-116–121 | DROP_UNUSED_SCHEMA | No | 0 | 2/1 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `security_event_notes` | cross-cutting; CC-01 | KEEP_CROSS_CUTTING | Yes | 0 | 2/0 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `security_events` | cross-cutting; CC-01 | KEEP_CROSS_CUTTING | Yes | 0 | 1/1 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `settlement_records` | V2 consultation/payment; MF-15 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 1/0 | 0 | Schema/V2 dormant; không có Release-1 flow | 0 rows; không có retention obligation đã tìm thấy |
| `specialties` | expert; MF-05 / UC-60–71 | KEEP_RELEASE1 | No | 8 | 0/1 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `sponsored_campaigns` | V2 partner; MF-16 / V2 | DROP_OUT_OF_SCOPE | Yes | 0 | 2/0 | 0 | V2 runtime hiện có; phải xóa ở task code sau | 0 rows; không có retention obligation đã tìm thấy |
| `structured_intake_data` | triage/evidence; MF-06 / UC-72–76 | KEEP_RELEASE1 | Yes | 20 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `system_configurations` | cross-cutting; CC-01 | KEEP_RELEASE1 | Yes | 1 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `token_blacklist` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 102 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `triage_answers` | triage/evidence; MF-06 / UC-72–76 | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `triage_assessments` | triage/evidence; MF-06 / UC-72–76 | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `uploaded_files` | health-record/device; MF-08, MF-13 / UC-83–88,112–115 | KEEP_CROSS_CUTTING | Yes | 5 | 0/4 | 0 | Retention/audit/security/evidence authority | Bắt buộc giữ cho audit/consent/security/forensic |
| `user_checklist_items` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 1 | 4/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `user_identities` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 12 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `user_profiles` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 2 | 1/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `user_roles` | identity; MF-01 / UC-01–18 | DROP_UNUSED_SCHEMA | No | 0 | 0/0 | 0 | Đã có Batch 1–5 migration canonical hóa | 0 rows; không có retention obligation đã tìm thấy |
| `user_sessions` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 695 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `user_topic_follows` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `users` | identity; MF-01 / UC-01–18 | KEEP_RELEASE1 | Yes | 52 | 0/64 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `vaccination_records` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 0 | 2/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `vaccination_reference_schedules` | baby; MF-03 / UC-32–45 | KEEP_RELEASE1 | Yes | 7 | 0/0 | 0 | Entity/repository/service hoặc collection/runtime Release-1 | Release-1 operational/history |
| `wards` | reference/geography; MF-05/MF-07 lookup | NEEDS_REVIEW | No | 0 | 0/0 | 0 | Live-only reference; chưa có runtime authority | Giữ fail-closed cho tới khi xác nhận ownership |

## Approved 17 and dependency order

1. Leaves/independent: `consultation_requests`, `consultation_messages`, `expert_reviews`, `refund_records`, `settlement_records`, `commission_config`, `partner_expert_links`, `partner_services`, `sponsored_campaigns`, `impact_assessment_ratings`, `contribution_attachments`, `expert_identity_verifications`, `expert_verification_documents`, `medical_contributions`.
2. Released parents: `consultation_disputes`, `commission_records`.
3. Final finance parent: `payment_transactions`.

Ba approved tables live-only (`contribution_attachments`, `expert_identity_verifications`, `medical_contributions`) phải chấp nhận known clean-bootstrap absence nhưng vẫn fail nếu present-and-nonempty hoặc có unknown dependency. Không dùng `CASCADE`.

## Blocked evidence

| Table | Live evidence | Block reason |
|---|---:|---|
| `consultation_bookings` | 1, CONFIRMED | Data + UC-88 dependency cần cutover consent |
| `consultation_sessions` | 1, WAITING | Nonterminal data |
| `consultation_price_bands` | 1, ACTIVE | Active pricing reference |
| `expert_consultation_prices` | 1, ACTIVE | Active price + booking FK |
| `direct_conversations` | 5, ACTIVE | Active realtime history |
| `direct_messages` | 32 | Conversation history/retention chưa duyệt |
| `conversation_calls` | 43 | 28 ENDED, 10 CANCELLED, 4 MISSED, 1 DECLINED; retention chưa duyệt |
| `partner_organizations` | 1, PENDING_APPROVAL | Nonterminal + retained `care_facilities.partner_id` FK |
| `health_summaries` | 0 | KEEP Release 1 UC-87/88; current booking FK phải được thay bằng consent, không drop |

## Evidence paths

- `_bmad-output/implementation-artifacts/spec-final-cleanup-release1-schema.md`
- `_bmad-output/implementation-artifacts/investigations/release-1-database-audit-investigation.md`
- `_bmad-output/implementation-artifacts/investigations/final-cleanup-schema-live-audit-investigation.md`
- `02_Requirements/SRS/3_Functional_Specification_Detailed_Scope_121UC.md`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260722020300__consolidate_notification_records.sql` through `V20260722021000__remove_remaining_unused_release1_schema.sql`
