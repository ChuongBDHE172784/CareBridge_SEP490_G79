# CareBridge Data Dictionary Summary

**Source**: `02_Design/Database/CareBridge_ERD_Description_and_Data_Dictionary_Updated.docx.md`  
**Logical Model**: `02_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml`  
**Version**: 2026-06-12  
**Entities**: 67 across 11 domains

---

## Entity Count by Domain

| Domain | Entity Count | Entities |
|---------|--------------|----------|
| **Identity & Access** | 9 | `roles`, `users`, `user_roles`, `user_sessions`, `community_profiles`, `notification_preferences`, `notifications`, `data_permissions`, `audit_logs` |
| **Care Journey** | 4 | `mother_journeys`, `maternal_health_metrics`, `postpartum_logs` |
| **Baby Care** | 5 | `baby_profiles`, `baby_daily_logs`, `development_milestones`, `growth_measurements`, `vaccination_records` |
| **Health Records** | 3 | `health_records`, `health_summaries` |
| **Care Coordination** | 5 | `care_groups`, `care_group_members`, `care_tasks`, `reminders`, `expenses` |
| **Community & Content** | 8 | `community_topics`, `community_questions`, `community_answers`, `content_items`, `checklist_templates`, `checklist_items`, `content_reports`, `moderation_actions` |
| **Expert** | 6 | `expert_profiles`, `expert_credentials`, `expert_availability`, `expert_location_shares`, `expert_consultation_prices`, `contribution_points` |
| **Consultation** | 10 | `consultation_bookings`, `consultation_sessions`, `consultation_messages`, `payment_transactions`, `commission_records`, `expert_reviews`, `consultation_price_bands`, `consultation_disputes`, `refund_records`, `settlement_records` |
| **AI & Safety** | 5 | `triage_assessments`, `triage_answers`, `safety_monitoring_settings`, `safety_events`, `safety_alerts` |
| **Partner & Location** | 7 | `partner_organizations`, `partner_expert_links`, `partner_services`, `sponsored_campaigns`, `care_facilities`, `emergency_events`, `location_snapshots` |
| **Exercise & Posture** | 5 | `pregnancy_exercises`, `exercise_safety_checks`, `exercise_sessions`, `posture_analysis_configs`, `posture_feedback_events` |
| **Device** | 2 | `health_device_connections`, `device_measurements` |
| **Total** | **67** | |

---

## Entity Reference Quick Guide

### Identity & Access (9 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `roles` | `role_id` | RBAC roles: USER, MOTHER, FAMILY_MEMBER, EXPERT, MODERATOR, CONTENT_ADMIN, ADMIN, PARTNER | - |
| `users` | `user_id` | Core user accounts (authentication identity) | - |
| `user_roles` | `user_role_id` | Many-to-many user-role mapping | `user_id`, `role_id` |
| `user_sessions` | `session_id` | Login sessions and refresh tokens | `user_id` |
| `community_profiles` | `community_profile_id` | Public community display profile | `user_id` |
| `notification_preferences` | `preference_id` | Channel and type preferences | `user_id` |
| `notifications` | `notification_id` | System notifications | `recipient_user_id` |
| `data_permissions` | `permission_id` | Data sharing grants with scope/expiry | `owner_user_id`, `grantee_user_id` |
| `audit_logs` | `audit_log_id` | Immutable action audit trail | `actor_user_id` |

### Care Journey (4 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `mother_journeys` | `journey_id` | Mother's pregnancy/postpartum journey | `owner_user_id` |
| `maternal_health_metrics` | `metric_id` | Maternal health measurements | `journey_id` |
| `postpartum_logs` | `postpartum_log_id` | Postpartum recovery tracking | `journey_id` |

### Baby Care (5 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `baby_profiles` | `baby_id` | Baby profile and demographics | `owner_user_id`, `related_journey_id` |
| `baby_daily_logs` | `baby_log_id` | Daily baby care logs (feeding, sleep) | `baby_id`, `recorded_by` |
| `development_milestones` | `milestone_id` | Developmental achievements | `baby_id`, `recorded_by` |
| `growth_measurements` | `growth_measurement_id` | Weight, height, head circumference | `baby_id` |
| `vaccination_records` | `vaccination_record_id` | Vaccination schedule and status | `baby_id`, `proof_record_id` |

### Health Records (3 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `health_records` | `health_record_id` | Uploaded health documents/files | `owner_user_id`, `journey_id`, `baby_id` |
| `health_summaries` | `summary_id` | Time-period health summaries (JSON) | `owner_user_id`, `journey_id`, `baby_id` |

### Care Coordination (5 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `care_groups` | `care_group_id` | Care team groups for journey/baby | `owner_user_id`, `journey_id`, `baby_id` |
| `care_group_members` | `care_group_member_id` | Group membership with roles | `care_group_id`, `user_id` |
| `care_tasks` | `care_task_id` | Assigned care tasks with due dates | `care_group_id`, `assigned_by`, `assigned_to` |
| `reminders` | `reminder_id` | Reminders with recurrence rules | `owner_user_id`, `journey_id`, `baby_id` |
| `expenses` | `expense_id` | Care-related expense tracking | `owner_user_id`, `journey_id`, `baby_id` |

### Community & Content (8 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `community_topics` | `topic_id` | Category topics with risk levels | - |
| `community_questions` | `question_id` | User questions (can be anonymous) | `author_user_id`, `topic_id` |
| `community_answers` | `answer_id` | Community or expert answers | `question_id`, `author_user_id`, `expert_profile_id` |
| `content_items` | `content_item_id` | Approved articles, FAQ (versioned) | `topic_id`, `author_user_id` |
| `checklist_templates` | `checklist_template_id` | Stage-based checklist templates | `content_item_id` |
| `checklist_items` | `checklist_item_id` | Items within checklist templates | `checklist_template_id` |
| `content_reports` | `report_id` | User reports for moderation | `reporter_user_id` |
| `moderation_actions` | `moderation_action_id` | Moderator actions on reports/content | `report_id`, `moderator_user_id` |

### Expert (6 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `expert_profiles` | `expert_profile_id` | Expert professional profile | `user_id`, `verified_by` |
| `expert_credentials` | `credential_id` | Uploaded credential documents | `expert_profile_id` |
| `expert_availability` | `availability_id` | Available time slots | `expert_profile_id` |
| `expert_location_shares` | `location_share_id` | Expert location sharing with expiry | `expert_profile_id` |
| `expert_consultation_prices` | `expert_price_id` | Expert-specific pricing within bands | `expert_profile_id`, `price_band_id` |
| `contribution_points` | `point_record_id` | User/expert points from activities | `user_id` |

### Consultation (10 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `consultation_bookings` | `booking_id` | Booking request with price snapshot | `requester_user_id`, `expert_profile_id`, `availability_id`, `expert_price_id` |
| `consultation_sessions` | `session_id` | Active consultation session | `booking_id` |
| `consultation_messages` | `message_id` | Chat messages in session | `session_id`, `sender_user_id` |
| `payment_transactions` | `payment_id` | Payment records with gateway details | `booking_id`, `payer_user_id` |
| `commission_records` | `commission_id` | Expert commission calculation | `payment_id`, `expert_profile_id` |
| `expert_reviews` | `review_id` | User reviews for experts | `booking_id`, `reviewer_user_id`, `expert_profile_id` |
| `consultation_price_bands` | `price_band_id` | System-configured price ranges | `configured_by` |
| `consultation_disputes` | `dispute_id` | Payment/consultation disputes | `booking_id`, `submitted_by`, `resolved_by` |
| `refund_records` | `refund_id` | Refund transactions | `payment_id`, `dispute_id`, `approved_by` |
| `settlement_records` | `settlement_id` | Expert payout settlements | `commission_id`, `expert_profile_id` |

### AI & Safety (5 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `triage_assessments` | `assessment_id` | Symptom intake and risk classification | `user_id`, `journey_id`, `baby_id` |
| `triage_answers` | `triage_answer_id` | Individual symptom answers | `assessment_id` |
| `safety_monitoring_settings` | `setting_id` | IMU safety monitoring config | `user_id`, `emergency_contact_user_id` |
| `safety_events` | `safety_event_id` | Detected fall/impact events | `user_id`, `setting_id` |
| `safety_alerts` | `safety_alert_id` | Sent alerts to emergency contacts | `safety_event_id`, `recipient_user_id`, `location_snapshot_id` |

### Partner & Location (7 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `partner_organizations` | `partner_id` | Partner clinics/organizations | `representative_user_id`, `verified_by` |
| `partner_expert_links` | `partner_expert_link_id` | Expert-partner affiliations | `partner_id`, `expert_profile_id`, `approved_by` |
| `partner_services` | `service_id` | Services offered by partners | `partner_id` |
| `sponsored_campaigns` | `campaign_id` | Sponsored content campaigns | `partner_id`, `reviewed_by` |
| `care_facilities` | `facility_id` | Healthcare facility directory | `partner_id` |
| `emergency_events` | `emergency_event_id` | Emergency flow initiations | `user_id`, `selected_facility_id`, `selected_expert_id` |
| `location_snapshots` | `location_snapshot_id` | Time-limited location captures | `user_id` |

### Exercise & Posture (5 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `pregnancy_exercises` | `exercise_id` | Curated pregnancy exercise content | `created_by` |
| `exercise_safety_checks` | `safety_check_id` | Pre-exercise safety screening | `exercise_id`, `journey_id`, `user_id` |
| `exercise_sessions` | `exercise_session_id` | User exercise session tracking | `exercise_id`, `journey_id`, `user_id`, `safety_check_id` |
| `posture_analysis_configs` | `posture_config_id` | Posture analysis rules/models config | `exercise_id`, `configured_by` |
| `posture_feedback_events` | `feedback_event_id` | Real-time posture feedback data | `exercise_session_id`, `posture_config_id` |

### Device (2 entities)

| Entity | PK | Purpose | Key FK References |
|---------|----|---------|-------------------|
| `health_device_connections` | `connection_id` | Connected health devices/wearables | `user_id` |
| `device_measurements` | `device_measurement_id` | Synced device measurements | `connection_id` |

---

## Common Field Patterns

All entities follow consistent timestamp and audit patterns:

| Field | Type | Description |
|-------|------|-------------|
| `*_id` | UUID | Primary key (PK) |
| `created_at` | TIMESTAMP | Record creation timestamp (NOT NULL) |
| `updated_at` | TIMESTAMP | Last update timestamp (nullable) |
| Foreign keys ending `_id` | UUID | References to related entity PK |

### Status Field Conventions

| Entity Pattern | Status Values |
|----------------|---------------|
| User/Account status | `ACTIVE`, `INACTIVE`, `SUSPENDED`, `PENDING_VERIFICATION` |
| Content status | `DRAFT`, `PENDING`, `PUBLISHED`, `ARCHIVED`, `REJECTED` |
| Booking/Session status | `PENDING`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| Payment status | `PENDING`, `PAID`, `FAILED`, `REFUNDED`, `PARTIALLY_REFUNDED` |
| Moderation status | `PENDING`, `APPROVED`, `REJECTED`, `HIDDEN` |
| Triage risk level | `GREEN`, `YELLOW`, `RED` |

---

## Key Relationships Summary

### Core User-Centric Relationships

```
users
├── has many → user_roles → roles
├── has many → user_sessions
├── has one → community_profiles
├── has many → mother_journeys
├── has many → baby_profiles
├── has many → health_records
├── has many → care_groups (as owner)
├── has many → data_permissions (as owner)
├── has many → notifications
├── has one → expert_profiles (optional)
├── has many → triage_assessments
└── has one → safety_monitoring_settings
```

### Care Coordination Flow

```
mother_journeys / baby_profiles
├── belong to → care_groups
│   └── have many → care_group_members → users
│   └── have many → care_tasks → users (assigned_to)
└── have many → reminders
```

### Consultation & Payment Flow

```
expert_profiles
├── have many → expert_availability
├── have many → expert_consultation_prices
└── have many → consultation_bookings
    ├── create → consultation_sessions → consultation_messages
    ├── trigger → payment_transactions
    │   ├── generate → commission_records
    │   └── can have → refund_records
    └── can generate → consultation_disputes
        └── may lead to → refund_records
```

---

## Migration Notes

### Required Database Constraints

1. **Foreign Keys**: All `+` marked fields in PlantUML must have FK constraints
2. **Unique Constraints**:
   - `users.email` (UNIQUE)
   - `user_roles` composite unique on `(user_id, role_id)`
   - `community_topics.slug` (UNIQUE)
3. **Check Constraints**:
   - Status fields must match allowed values
   - `maternal_health_metrics.value_numeric` > 0 for valid measurements
4. **Indexes**:
   - All FK fields need indexes
   - Composite index on `(journey_id, baby_id)` for care entities
   - Full-text search on `community_questions.title`, `content_items.title`

### JSON Field Usage

Fields marked `*_json` store structured data:
- `data_permissions.permission_json` - permission details
- `care_group_members.permission_json` - member permissions
- `health_summaries.summary_json` - aggregated health data
- `safety_events.technical_log_json` - sensor metadata
- `exercise_sessions.summary_json` - session metrics

Use PostgreSQL `jsonb` type with GIN indexes for query performance.

---

## Design System Notes

For UI/UX implementation of these entities, refer to:
- **Claymorphism Design System**: `.claude/skills/ui-skill-system/`
- **Color Palette**: `#F6F1EC` (bg), `#C98C7B` (accent), `#5A463F` (text)
- **Shadows**: `shadow-[0_12px_32px_rgba(90,70,63,0.06)]`
- **Border Radius**: `rounded-2xl` (32px) for cards, `rounded-full` for buttons

---

**Document End**
