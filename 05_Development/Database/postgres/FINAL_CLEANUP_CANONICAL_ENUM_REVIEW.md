---
title: Final Cleanup Canonical Models and Enum/Multi-select Review
baseline_commit: 2b33b582
status: deferred-structural-review
---

# Final Cleanup Canonical Models and Enum/Multi-select Review

## Guardrail

Final Cleanup chỉ ghi nhận representation hiện tại và hướng canonical. Không thực hiện broad enum/lookup/mapping refactor trong batch này. Repository migrations dùng `varchar` + `CHECK`; các PostgreSQL native enum/array còn ở live là Hibernate/live-only drift và không được coi là authority.

## Canonical model map

| Domain | Canonical model | Persistence authority | Final Cleanup decision |
|---|---|---|---|
| Accounts / role | Một account, một scalar effective role | `users.role`, `user_identities`, `user_sessions`, security/audit tables | Giữ; `roles/user_roles` đã được Batch 2 loại bỏ |
| Notification | Durable notification + preferences | `notification_records`, `notification_preferences`, `device_tokens` | Giữ; `notifications` đã được Batch 1 loại bỏ |
| Triage | Intake aggregate + structured extraction | `intake_sessions`, `structured_intake_data`, `health_memory_entries` | Giữ; catalog cảnh báo y tế quản trị đã retire, sàn an toàn runtime không phụ thuộc database |
| Safety | IMU session/event + canonical config, responses and deliveries | `imu_monitoring_sessions`, `imu_safety_events`, `safety_monitoring_config`, `safety_event_responses`, `emergency_alert_deliveries`, `emergency_alert_attempts` | Giữ; legacy safety đã được Batch 4 loại bỏ |
| Facility | Verified/searchable facility aggregate + durable legacy mapping | `care_facilities`, `care_facility_legacy_ids` | Giữ; `hospitals` được Batch 5 canonicalize |
| Expert | Profile, credentials, availability, location share, contribution reputation | `expert_profiles`, `expert_credentials`, `expert_availability`, `expert_location_shares`, `contribution_points`, `specialties` | Giữ; dormant verification tables thuộc approved cleanup |
| Community | Question/answer interactions + governed topic tree | `community_topics`, `community_questions`, `community_answers`, likes/bookmarks/follows/mutes/profiles | Giữ |
| Consent | Purpose/scope/expiry grant, separate from delegated family permission | `consent_grants`, `data_permissions`, privacy and audit records | Giữ; consent must replace UC-88 booking workaround |
| Health records | Record/file/summary with source and consent-controlled sharing | `health_records`, `health_record_files`, `health_summaries`, `uploaded_files` | Giữ; `health_summaries` is dependency-blocked, never a cleanup target |
| Reminders / care plan | Reminder/checklist/task records | `reminders`, `care_tasks`, checklist tables | Giữ; consolidate duplicate Java `CareTask` models later, not DB tables |
| Family sync | Care group/member/task/alert with scoped permissions | `care_groups`, `care_group_members`, `care_tasks`, `family_alert_log` | Giữ |
| Content | Versioned content, source evidence and moderation | `content_items`, `content_sources`, `content_reports`, `moderation_actions`, evidence-source tables | Giữ |
| Devices | OAuth connection + imported observations | `health_device_connections`, `device_measurements` | Giữ; JSON scope serialization correction deferred |

## Enum, lookup and multi-select review

| Field | Current model | Recommendation | Action |
|---|---|---|---|
| Role | Scalar Java/client enum + `users.role` varchar/check | KEEP_ENUM; review/deprecate `PARTNER` only after blocked V2 data decision | Deferred |
| Notification type | Scalar enum on durable notification model; legacy V2 values remain in code vocabulary | NEEDS_REVIEW; retain Release-1 account/reminder/community/family/safety types, remove V2 values only with runtime cleanup | Deferred |
| Consent data type / purpose | Scalar codes per grant | KEEP_ENUM | Now: preserve |
| Consent scope | Security-sensitive free-text/code mix | NEEDS_REVIEW, governed vocabulary before normalization | Deferred |
| Expert specialties | Scalar text + `specialty_id`; empty live array drift | CONVERT_TO_LOOKUP/MAPPING only if product confirms multi-specialty; current Release 1 directory is scalar | Deferred |
| Expert service/support scope | Availability/profile fields without governed many-to-many authority | CONVERT_TO_MAPPING after UC-64/65 cardinality design | Deferred |
| Community topics | Governed hierarchical lookup | CONVERT_TO_LOOKUP (already represented by `community_topics`) | Now: keep |
| Content categories | Topic/category concepts partially shared | NEEDS_REVIEW; do not invent a second taxonomy in cleanup | Deferred |
| Content stage/topic mappings | Predominantly scalar/CSV compatibility | CONVERT_TO_MAPPING with explicit stage translation | Deferred |
| Reminder type | Scalar backend/Dart enums with vocabulary drift | KEEP_ENUM and align clients | Deferred |
| Maternal metric type | Scalar code per observation | KEEP_ENUM and align clients | Deferred |
| Baby daily log type | Scalar enum; backend supports more values than Flutter | KEEP_ENUM; align 7 backend vs 4 Flutter values | Deferred |
| Health-record tags | SRS UC-84 requires tags but no canonical persistence contract exists | CONVERT_TO_MAPPING after requirements/API design | Deferred |
| Device observation type | Scalar metric vocabulary | KEEP_ENUM; share canonical observation codes where semantics match | Deferred |
| Device OAuth scopes | JSONB column, writer historically produced CSV-like content | KEEP_JSONB as a real JSON array with provider validation | Deferred correctness fix |
| Evidence domains | Managed natural-key records | CONVERT_TO_LOOKUP (existing `evidence_sources`) | Now: keep |
| Evidence applicable stages | CSV/text versus list-consuming clients | CONVERT_TO_MAPPING (`evidence_source_stages`) with explicit stage translation | Deferred |
| Emergency recipients | Contact/family recipients represented through separate records and broad delivery behavior | CONVERT_TO_MAPPING with explicit selected recipients and expiry | Deferred |
| Facility type | Scalar varchar/check and Java enum | KEEP_ENUM after canonical normalization | Now: preserve |
| Facility level | Free-text/reference value | NEEDS_REVIEW; governed lookup if filtering/administration is required | Deferred |
| Facility ownership | Scalar constrained vocabulary (`PUBLIC`, `MILITARY`) | NEEDS_REVIEW before adding further ownership values | Deferred |
| Facility source / verification | Scalar closed lifecycle | KEEP_ENUM | Now: preserve |
| Triage stage / risk / status | Scalar closed workflow values | KEEP_ENUM; maintain explicit cross-domain stage mapping | Now: preserve |
| Safety event type / status / response | Scalar closed workflow, response still partly string-based | KEEP_ENUM; make response enum in a focused later change | Deferred |
| Family permission flags | Compatibility JSON/document plus explicit boolean semantics | KEEP_JSONB temporarily; normalize security decisions only behind dual-read/write | Deferred |
| Structured symptoms and provider payloads | Whole-document JSONB | KEEP_JSONB with shape validation | Now: preserve |

## Scope evidence

- Release 1 is MF-01 through MF-14; MF-15 paid consultation/realtime/payment and MF-16 partner/sponsored workflows are V2.
- UC-81/82 explicitly retain nearby support requests/responses and expert nearby availability.
- UC-88 explicitly retains health-summary/record sharing under active consent; booking is not its canonical authority.
- UC-116–121 retain emergency recipients and safety-event evidence even where no direct CRUD endpoint exists.

## Deferred risks

- Nearby support is `PARTIAL_RUNTIME`: mother client flow, eligibility/service-area enforcement and consent ownership require hardening; the tables must not be removed.
- Emergency-recipient behavior currently does not fully match selected-recipient intent.
- Health-record tags have SRS intent but no canonical runtime persistence.
- Large enum convergence during destructive cleanup would enlarge rollback risk; use separate additive/dual-read stories.

## Evidence paths

- `02_Requirements/SRS/3_Functional_Specification_Detailed_Scope_121UC.md`
- `_bmad-output/implementation-artifacts/investigations/release-1-database-audit-investigation.md`
- `05_Development/Database/postgres/FINAL_CLEANUP_TABLE_INVENTORY.md`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260722020300__consolidate_notification_records.sql` through `V20260722020700__consolidate_nearby_care_facilities.sql`
