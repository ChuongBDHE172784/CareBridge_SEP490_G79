# DATABASE REDESIGN PHASE 1 — REVISED TARGET

> Trạng thái: database design only. Không sửa application code, không tạo/chạy migration, không kết nối database và không chạy test.

## Revised Target Summary

- Live baseline: **127** base tables.
- Canonical core target: **70** base tables.
- Approved Release-1 extensions: **3** base tables.
- Effective deployed target: **73** base tables.
- Core redesign reduction: **57** tables; effective deployed reduction: **54** tables.
- Business/application tables: **72** — 69 core + 3 approved extensions.
- Technical tables: **1** — `flyway_schema_history`.
- Live merge sources: **74**.
- Live drop candidates pending evidence: **25**.
- Tables kept by identity: **28**.
- Tables newly created or rebuilt: **42 core + 3 approved extensions**.

Target 70 core thay thế target 65 trước đây. `care_items` và `legacy_archived_records` bị loại khỏi core target; chúng được thay bằng sáu bảng theo owner/lifecycle riêng. `triage_session_evidence` được thêm để giữ claim/citation có thể query và audit. Release 1 bổ sung đúng ba extension đã duyệt cho consultation/handoff; vì vậy inventory triển khai là **70 core + 3 extensions**, không phải 70 bảng tổng cộng.

## Exact 70 Core Tables

### Person / Care Subject — 2

1. `persons`
2. `care_subjects`

### Account / Auth — 6

3. `users`
4. `user_identities`
5. `auth_sessions`
6. `auth_revocations`
7. `auth_challenges`
8. `account_deletion_requests`

### Mother Journey — 4

9. `mother_journeys`
10. `mother_journey_events`
11. `maternal_observations`
12. `maternal_exercise_sessions`

### Baby Care — 5

13. `care_logs`
14. `growth_measurements`
15. `development_milestones`
16. `vaccination_records`
17. `vaccination_schedules`

### Community — 4

18. `community_profiles`
19. `community_topics`
20. `community_content`
21. `community_interactions`

### Expert — 7

22. `professional_profiles`
23. `specialties`
24. `professional_specialties`
25. `expert_credentials`
26. `expert_availability`
27. `expert_location_shares`
28. `expert_contribution_events`

### AI Triage / Knowledge — 6

29. `triage_sessions`
30. `triage_session_evidence`
32. `health_context_memories`
33. `knowledge_sources`
34. `knowledge_source_reviews`

### Health Records / Files / Devices — 5

35. `health_records`
36. `attachments`
37. `health_record_attachments`
38. `device_connections`
39. `health_observations`

### Family / Care Plan — 6

40. `care_groups`
41. `care_group_members`
42. `scheduled_care_items`
43. `family_tasks`
44. `preparation_checklist_items`
45. `care_item_templates`

### Verified Content / Moderation — 5

46. `content_items`
47. `content_item_topics`
48. `content_item_sources`
49. `moderation_cases`
50. `moderation_events`

### Notification — 2

51. `notification_records`
52. `device_tokens`

### Safety — 5

53. `safety_configs`
54. `safety_monitoring_sessions`
55. `safety_events`
56. `safety_event_actions`
57. `emergency_contacts`

### Facility / Nearby Care — 4

58. `administrative_areas`
59. `care_facilities`
60. `nearby_support_requests`
61. `nearby_support_responses`

### Audit / Security / Consent / Config — 4

62. `audit_events`
63. `security_events`
64. `data_permissions`
65. `system_configurations`

### Expense / Domain Archives / Infrastructure — 5

66. `expense_entries`
67. `archived_consultation_records`
68. `archived_realtime_records`
69. `archived_partner_records`
70. `flyway_schema_history`

## Approved Release-1 Extensions — 3

71. `expert_consultation_requests`
72. `consultation_context_shares`
73. `consultation_context_citations`

Ba bảng này là extension có chủ đích cho Epic 6: request lifecycle, consented context snapshot và approved citation snapshot. Chúng không thay thế hoặc đổi số thứ tự của 70 core tables; mọi inventory gate phải kiểm tra riêng **70 core + 3 extensions**.

## Revised Canonical Models

### Person, account and professional identity

- `persons` chứa thông tin con người dùng chung; không chứa password, provider identity hoặc role.
- `care_subjects` tham chiếu `persons` và giữ subject type, owner, journey link cùng baby-specific attributes. Baby không trở thành user account.
- `users.person_id` là unique FK. Private account settings có thể dùng validated `settings_jsonb`; consent không nằm trong JSONB này.
- `community_profiles` vẫn là public identity boundary riêng với private person/account profile.
- `professional_profiles`, `expert_credentials` và `professional_specialties` giữ lifecycle chuyên môn/verification độc lập. Credential history không bị gộp vào `users`.

### Domain-specific immutable archives

Không còn `legacy_archived_records`. Ba archive tables là:

- `archived_consultation_records`: booking, price, consultation session và expert consultation price.
- `archived_realtime_records`: conversation, message và call.
- `archived_partner_records`: partner organization data ngoài Release 1.

Mỗi archive table có tối thiểu: `archive_id`, `legacy_table`, `legacy_id`, `owner_user_id` nullable, `payload_jsonb`, `original_created_at`, `archived_at`, `retention_until` nullable, `archive_reason`, `source_schema_version`, `checksum`. `payload_jsonb` là immutable sau archive. Domain-specific access policy và retention áp dụng riêng cho từng table.

### Care plan lifecycle separation

`care_items` bị loại bỏ.

`scheduled_care_items` có: `care_item_id`, `owner_user_id`, `care_subject_id`, `item_type`, `title`, `scheduled_at`, `recurrence_rule`, `snoozed_until`, `completed_at`, `skipped_at`, `status`, `source_reference_type`, `source_reference_id`, `vaccination_record_id`, `created_at`, `updated_at`. `vaccination_record_id` là nullable FK thật; vaccination reminder phải có `vaccination_record_id` hoặc `care_subject_id` theo CHECK constraint.

`family_tasks` có: `task_id`, `care_group_id`, `creator_user_id`, `assignee_user_id`, `care_subject_id`, `title`, `description`, `due_at`, `status`, `completed_at`, `cancelled_at`, `created_at`, `updated_at`.

`preparation_checklist_items` có: `checklist_item_id`, `owner_user_id`, `mother_journey_id`, `template_entry_id`, `title`, `display_order`, `status`, `due_at`, `completed_at`, `created_at`, `updated_at`.

### Checklist template and posture configuration

Chọn self-reference model để giữ target ở 70 tables. `care_item_templates` lưu từng root, entry và posture config thành row riêng, không lưu danh sách entry trong JSONB.

Các cột tối thiểu: `template_id`, `parent_template_id` nullable self-FK, `entry_type`, `title`, `description`, `display_order`, `stage`, `is_active`, `version`, `effective_from`, `effective_to`, `configuration_jsonb`, `configuration_hash`.

- `entry_type` phân biệt `TEMPLATE_ROOT`, `CHECKLIST_ENTRY`, `EXERCISE_TEMPLATE`, `POSTURE_CONFIG`.
- Mỗi entry có stable `template_id`, parent FK và display order.
- Posture config là versioned row; không update đè version đã được session sử dụng.
- `maternal_exercise_sessions` giữ FK tới exact exercise template/config version đã sử dụng.

### Triage session and evidence

`triage_sessions` giữ scalar: `stage`, profile-context ID, `risk_level`, `status`, `emergency`, `disclaimer_version`, `completed_at`. Conversation/result JSONB phải có `schema_version`, `content_hash` và immutable completion snapshot.

`triage_session_evidence` có: `evidence_id`, `triage_session_id`, `evidence_type`, `claim_code`, `claim_text`, `knowledge_source_id`, `citation_url`, `citation_domain`, `source_version`, `source_snapshot_jsonb`, `content_hash`, `created_at`.

Constraints: FK tới `triage_sessions`; nullable FK tới `knowledge_sources`; index `triage_session_id`; unique `(triage_session_id, evidence_type, content_hash)`; không UPDATE/DELETE evidence sau khi session hoàn tất.

### Maternal observations

`maternal_exercise_sessions` chỉ lưu runtime session. Exercise safety answer/block và posture feedback là từng row riêng trong `maternal_observations`.

Các cột bắt buộc: `observation_id`, `observation_type`, `mother_journey_id`, `exercise_session_id`, `numeric_value`, `secondary_numeric_value`, `unit`, `text_value`, `severity`, `observed_at`, `payload_jsonb`, `schema_version`, `source_type`, `created_at`.

Các type tối thiểu: `EXERCISE_SAFETY_ANSWER`, `EXERCISE_SAFETY_BLOCK`, `POSTURE_FEEDBACK`, `POSTPARTUM_LOG`, `MATERNAL_METRIC`. Type, numeric values, unit, severity, time và session/journey FKs là scalar/indexed. JSONB chỉ chứa type-specific payload có schema version.

### Auth session canonical model

`auth_sessions` có: `session_id`, `user_id`, `token_family_id`, `device_identifier`, `device_name`, `refresh_token_hash`, `issued_at`, `expires_at`, `last_used_at`, `rotated_at`, `revoked_at`, `revoke_reason`, `status`, `created_ip_hash`, `user_agent_hash`.

Required rules: không lưu raw token; unique token-family/device rule; revoke từng device; old token hash được append vào `auth_revocations` khi rotate; token reuse tạo revocation với `detected_reuse = true`.

`auth_revocations` có: `revocation_id`, `user_id`, `session_id`, `token_family_id`, `token_hash`, `reason`, `revoked_at`, `expires_at`, `detected_reuse`, `metadata_jsonb`. Table append-only; token hash có partial unique index khi không null.

### Data permission append-only model

`data_permissions` có: `permission_id`, `permission_series_id`, `version_number`, `supersedes_permission_id`, `owner_user_id`, `grantee_user_id`, `scope_type`, `scope_reference_id`, `purpose`, `granted_at`, `expires_at`, `revoked_at`, `revoked_by`, `status`, `policy_version`, `consent_evidence_key`, `created_at`.

Không update đè grant cũ. Scope/expiry/revoke thay đổi bằng version mới. Bắt buộc unique `(permission_series_id, version_number)` và self-FK `supersedes_permission_id`. Scope, purpose, expiry và revoke là scalar. Consent evidence/history không bị nhét toàn bộ vào JSONB.

### Safety action idempotency

`safety_event_actions` lưu mỗi response, delivery, attempt, family alert và map handoff thành một row riêng. Các field query/idempotency gồm `safety_event_id`, `action_type`, `recipient_user_id`, `device_identifier`, `notification_record_id`, `attempt_number`, `idempotency_key`, `response_type`, `delivery_status`, `created_at`.

Required constraints/indexes:

1. Một terminal response trên mỗi safety event.
2. Unique delivery `(safety_event_id, recipient_user_id, device_identifier)`.
3. Unique `(safety_event_id, attempt_number)` hoặc unique idempotency key.
4. Successful delivery row không được gửi lại cùng device.
5. Delivery status, attempt number và response type là scalar.
6. Action history immutable.
7. Index theo event, recipient, status và created time.

HEAD-only `emergency_alert_attempts`, `emergency_alert_deliveries`, `safety_event_responses` được map thành individual action rows có FK thật; không lưu danh sách trong JSONB.

### Audit and security

`audit_events`, `moderation_events`, `mother_journey_events`, `security_events` child notes và archive payloads là append-only. Data-access audit dùng scalar event category, actor, subject, resource, purpose, decision và timestamp; không ẩn các field query bắt buộc trong JSONB.

## Revised Merge Matrix

Matrix này bao phủ chính xác 74 live MERGE sources. Không còn `INVALID_MERGE`.

| Old tables | Revised target | Mapping and required control | Risk |
|---|---|---|---|
| `audit_logs`, `baby_journey_link_cleanup_summary` | `audit_events` | Preserve IDs, actor/entity, before/after payload, checksum; append-only | LOW |
| `baby_daily_logs` | `care_logs` | Typed log with scalar subject/type/time and versioned payload | LOW |
| `baby_link_submissions` | `audit_events` only | Retired command source: preserve sanitized historical evidence only; do not recreate a current subject-to-journey relation | LOW |
| `baby_profiles` | `persons`, `care_subjects` | Split human identity and baby-care attributes; never create account | LOW |
| `care_tasks` | `family_tasks` | Preserve group, creator, assignee, due/status/completion | LOW |
| `reminders` | `scheduled_care_items` | Preserve recurrence, snooze, completion/skip and source FK | MEDIUM |
| `user_checklist_items` | `preparation_checklist_items` | Preserve owner, journey, stable template-entry FK and order | LOW |
| `checklist_templates`, `checklist_items` | `care_item_templates` | Root/entry rows with stable ID, self-FK and display order; no entry-list JSONB | MEDIUM |
| `community_answer_likes`, `community_question_likes`, `community_bookmarks`, `user_topic_follows`, `question_notification_mutes` | `community_interactions` | Real content/topic FKs, exactly-one-target CHECK, unique actor/type/target | MEDIUM |
| `community_questions`, `community_answers` | `community_content` | QUESTION/ANSWER rows with parent FK; preserve moderation state | LOW |
| `consent_grants` | `data_permissions` | Versioned append-only grant series with scalar expiry/revoke/scope | HIGH |
| `consultation_bookings`, `consultation_price_bands`, `consultation_sessions`, `expert_consultation_prices` | `archived_consultation_records` | FK-ordered immutable archive; scalar owner/source/retention/checksum | HIGH |
| `direct_conversations`, `direct_messages`, `conversation_calls` | `archived_realtime_records` | Preserve parent references, sender, idempotency, timing and checksum | HIGH |
| `partner_organizations` | `archived_partner_records` | Immutable partner archive; facility facts copy only after verified match | HIGH |
| `content_reports`, `moderation_actions` | `moderation_cases`, `moderation_events` | Case state separate from append-only actions | LOW |
| `content_sources`, `evidence_sources`, `evidence_source_review_log` | `knowledge_sources`, `content_item_sources`, `knowledge_source_reviews` | Preserve source master, content relation and review history separately | MEDIUM |
| `contribution_points` | `expert_contribution_events` | Append-only point/reason/source ledger | LOW |
| `device_measurements` | `health_observations` | Scalar metric/type/unit/time/quality; raw provider payload versioned | LOW |
| `districts`, `provinces`, `wards` | `administrative_areas` | Typed adjacency hierarchy with stable legacy codes | LOW |
| `emergency_map_handoffs` | `safety_event_actions` | Individual MAP_HANDOFF action with facility FK and scalar status | MEDIUM |
| `emergency_sessions`, `imu_safety_events`, legacy `safety_events` | rebuilt `safety_events` | Preserve incident trigger, sensor source, location, status and source ID | HIGH |
| `family_alert_log` | `safety_event_actions` | Individual FAMILY_ALERT action with event FK and recipient count | MEDIUM |
| `imu_monitoring_sessions` | `safety_monitoring_sessions` | Preserve sensitivity/status/start/end/actor | LOW |
| `safety_monitoring_config` | `safety_configs` | Preserve owner/updater and scalar detection/alert settings | LOW |
| `exercise_safety_checks` | `maternal_observations` | One row per answer/block; session FK nullable so blocked preflight survives | HIGH |
| `exercise_sessions` | `maternal_exercise_sessions` | Runtime session with exact exercise/config-version FKs | LOW |
| `posture_feedback_events` | `maternal_observations` | One indexed POSTURE_FEEDBACK row per event; no JSON array | HIGH |
| `pregnancy_exercises`, `posture_analysis_configs` | `care_item_templates` | Versioned template/config rows; immutable used versions; session FK | HIGH |
| `expenses` | `expense_entries` | Preserve owner/subject, amount, currency and date | LOW |
| `expert_profiles` | `professional_profiles`, `professional_specialties` | Preserve verification; multi-select uses FK mapping | MEDIUM |
| `health_device_connections` | `device_connections` | Preserve provider, consent, token reference, scopes and status | LOW |
| `health_memory_entries` | `health_context_memories` | Preserve subject/session, expiry and deletion policy | LOW |
| `health_record_files` | `health_record_attachments` | Preserve display order and unique record/attachment pair | LOW |
| `health_summaries` | `health_records` | SUMMARY subtype with period/result/generator fields; UC-88 permission cutover gate | HIGH |
| `hospitals` | `care_facilities` | Verified upsert with exact legacy-ID reconciliation | MEDIUM |
| `intake_sessions`, `structured_intake_data` | `triage_sessions`, `triage_session_evidence` | Scalar safety/context fields; versioned immutable result; queryable claim/citation rows | HIGH |
| `maternal_health_metrics`, `postpartum_logs` | `maternal_observations` | Scalar type/value/unit/time; schema-versioned type payload | HIGH |
| `mother_baseline_contexts`, `mother_journey_transitions`, `pregnancy_outcome_evidence` | `mother_journeys`, `mother_journey_events` | Latest aggregate snapshot plus immutable versioned events | MEDIUM |
| `notification_preferences` | `users.settings_jsonb` | Validated per-type settings only; consent remains separate | MEDIUM |
| legacy `notifications` | `notification_records` | Exact ID/status/delivery/read reconciliation before retirement | LOW |
| `otp_verifications`, `password_reset_tokens` | `auth_challenges` | Typed hashed challenge with attempts/expiry/used state | LOW |
| `privacy_settings` | `users.settings_jsonb` | Validated private settings; consent history stays in permissions | MEDIUM |
| `refresh_tokens`, `user_sessions` | `auth_sessions`, `auth_revocations` | Token family/device session; rotation history and reuse detection append-only | HIGH |
| `security_event_notes` | `security_events` | Child NOTE events with parent/author/time; append-only | LOW |
| `token_blacklist` | `auth_revocations` | Indexed token hash/expiry/reason; optional session link only when proven | LOW |
| `uploaded_files` | `attachments` | Preserve owner, storage key, MIME, size, status and uniqueness | LOW |
| `user_profiles` | `persons`, `users` | Fail-fast profile conflict resolution; demographics vs account settings | LOW |
| `vaccination_reference_schedules` | `vaccination_schedules` | Preserve vaccine/dose/offset/description and stable key | LOW |

## Pending Drop Evidence Matrix

Không bảng nào dưới đây là `DROP_FINAL`. Tất cả ở trạng thái `DROP_CANDIDATE_PENDING_EVIDENCE`.

| Table | Runtime scan required | FK scan required | Retention decision | Current status |
|---|---|---|---|---|
| `commission_config` | YES | YES | Confirm V2 config discard | DROP_CANDIDATE_PENDING_EVIDENCE |
| `commission_records` | YES | YES | Confirm no financial retention | DROP_CANDIDATE_PENDING_EVIDENCE |
| `consultation_disputes` | YES | YES | Confirm dispute retention/legal hold | DROP_CANDIDATE_PENDING_EVIDENCE |
| `consultation_messages` | YES | YES | Confirm communication retention | DROP_CANDIDATE_PENDING_EVIDENCE |
| `consultation_requests` | YES | YES | Merge to consultation archive if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `contribution_attachments` | YES | YES | Merge to attachments if consumer exists | DROP_CANDIDATE_PENDING_EVIDENCE |
| `emergency_events` | YES | YES | Merge to canonical safety events if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `expert_identity_verifications` | YES | YES | Preserve verification/audit if required | DROP_CANDIDATE_PENDING_EVIDENCE |
| `expert_reviews` | YES | YES | Confirm V2 review retention | DROP_CANDIDATE_PENDING_EVIDENCE |
| `expert_verification_documents` | YES | YES | Merge to credentials/attachments if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `impact_assessment_ratings` | YES | YES | Merge to content interaction if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `location_snapshots` | YES | YES | Merge to owning safety/nearby aggregate if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `medical_contributions` | YES | YES | Merge to content/expert history if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `partner_expert_links` | YES | YES | Archive partner linkage if retention applies | DROP_CANDIDATE_PENDING_EVIDENCE |
| `partner_services` | YES | YES | Archive partner service if retention applies | DROP_CANDIDATE_PENDING_EVIDENCE |
| `payment_transactions` | YES | YES | Confirm financial retention/legal hold | DROP_CANDIDATE_PENDING_EVIDENCE |
| `refund_records` | YES | YES | Confirm financial retention/legal hold | DROP_CANDIDATE_PENDING_EVIDENCE |
| `roles` | YES | YES | Preserve RBAC history in audit events | DROP_CANDIDATE_PENDING_EVIDENCE |
| `safety_alerts` | YES | YES | Merge to safety actions if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `safety_monitoring_settings` | YES | YES | Merge to safety configs if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `settlement_records` | YES | YES | Confirm financial retention/legal hold | DROP_CANDIDATE_PENDING_EVIDENCE |
| `sponsored_campaigns` | YES | YES | Archive partner campaign if retention applies | DROP_CANDIDATE_PENDING_EVIDENCE |
| `triage_answers` | YES | YES | Merge to triage session snapshot if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `triage_assessments` | YES | YES | Merge to triage session/result if referenced | DROP_CANDIDATE_PENDING_EVIDENCE |
| `user_roles` | YES | YES | Preserve assignment history in audit events | DROP_CANDIDATE_PENDING_EVIDENCE |

HEAD-only `care_facility_legacy_ids` cũng là pending-evidence compatibility table nhưng không nằm trong 25 live candidates. Chỉ retire sau khi mọi legacy ID đã reconcile và runtime consumer bằng 0.

## Net Reduction

### Tables kept — 28

`account_deletion_requests`, `care_facilities`, `care_group_members`, `care_groups`, `community_profiles`, `community_topics`, `content_items`, `data_permissions`, `development_milestones`, `device_tokens`, `emergency_contacts`, `expert_availability`, `expert_credentials`, `expert_location_shares`, `flyway_schema_history`, `growth_measurements`, `health_records`, `mother_journeys`, `nearby_support_requests`, `nearby_support_responses`, `notification_records`, `security_events`, `specialties`, `system_configurations`, `user_identities`, `users`, `vaccination_records`.

### Tables merged — 74 live sources

Mọi source được liệt kê trong Revised Merge Matrix. Source chỉ được retire sau deterministic mapping, row reconciliation, orphan/constraint validation và runtime cutover.

### Tables pending drop — 25 live candidates

Không candidate nào được tính là safe-to-drop trước khi đủ runtime scan, catalog/FK scan và retention decision.

### Tables created or rebuilt — 42

`persons`, `care_subjects`, `auth_sessions`, `auth_revocations`, `auth_challenges`, `mother_journey_events`, `maternal_observations`, `maternal_exercise_sessions`, `care_logs`, `vaccination_schedules`, `community_content`, `community_interactions`, `professional_profiles`, `professional_specialties`, `expert_contribution_events`, `triage_sessions`, `triage_session_evidence`, `health_context_memories`, `knowledge_sources`, `knowledge_source_reviews`, `attachments`, `health_record_attachments`, `device_connections`, `health_observations`, `scheduled_care_items`, `family_tasks`, `preparation_checklist_items`, `care_item_templates`, `content_item_topics`, `content_item_sources`, `moderation_cases`, `moderation_events`, `safety_configs`, `safety_monitoring_sessions`, `safety_events`, `safety_event_actions`, `administrative_areas`, `audit_events`, `expense_entries`, `archived_consultation_records`, `archived_realtime_records`, `archived_partner_records`.

Final-count formula after all merge and drop evidence gates pass:

`127 live + 42 created/rebuilt - 74 merged sources retired - 25 approved drop candidates = 70`.

Compatibility windows may temporarily exceed 70 while old and canonical tables coexist. No `DROP CASCADE`; no old table is retired before code cutover and evidence gates.

## Approval Gate

- Revised target count: **70 core + 3 approved Release-1 extensions**.
- Duplicate target names: **0**.
- Invalid merges remaining: **0**.
- HIGH-risk merges without explicit mapping/control: **0**.
- Pending live drop candidates: **25**.
- Application/source/migration changes in this phase: **0**.

**APPROVED_TARGET_SCHEMA**

Approved on **2026-07-22**. Scope: core target schema design with **70 base tables**; the separately approved Release-1 inventory adds the 3 extensions listed above.

Approval authorizes only the next database-design/migration-planning stage. It does not authorize application code changes, live database access or migration execution.
