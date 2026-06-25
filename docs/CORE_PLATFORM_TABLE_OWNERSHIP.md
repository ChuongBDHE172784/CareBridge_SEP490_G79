---
title: CareBridge Core Platform Table Ownership
project: CareBridge_SEP490_G79
created: 2026-06-25
status: Baseline — V1 schema (71 tables)
---

# CareBridge Core Platform Table Ownership

This document maps every table in `V1__init_schema.sql` to its owning
backend domain package. "Current" means a Java @Entity exists today.
"Future" means the table is schema-only — no @Entity yet.

---

## Identity & Access

| Table | Backend Package | Status |
|---|---|---|
| `users` | `security.entity` | Current |
| `refresh_tokens` | `security.entity` | Current |
| `otp_verifications` | `security.entity` | Current |
| `roles` | `identity.entity` | Current |
| `user_roles` | `identity.entity` | Current |
| `user_sessions` | `identity.entity` | Current |
| `community_profiles` | `identity.entity` | Current |
| `notification_preferences` | `identity.entity` | Current |
| `notifications` | `identity.entity` | Current |
| `data_permissions` | `identity.entity` | Current |

## Consent & Audit

| Table | Backend Package | Status |
|---|---|---|
| `consent_grants` | `consent.entity` | Current |
| `audit_logs` | `audit.entity` | Current |
| `security_events` | `audit.entity` | Current |

## Care Journey

| Table | Backend Package | Status |
|---|---|---|
| `mother_journeys` | `carejourney.entity` | Future |
| `maternal_health_metrics` | `carejourney.entity` | Future |
| `postpartum_logs` | `carejourney.entity` | Future |

## Baby Care

| Table | Backend Package | Status |
|---|---|---|
| `baby_profiles` | `babycare.entity` | Future |
| `baby_daily_logs` | `babycare.entity` | Future |
| `development_milestones` | `babycare.entity` | Future |
| `growth_measurements` | `babycare.entity` | Future |
| `vaccination_records` | `babycare.entity` | Future |

## Health Records

| Table | Backend Package | Status |
|---|---|---|
| `health_records` | `healthrecord.entity` | Future |
| `health_summaries` | `healthrecord.entity` | Future |

## Care Coordination

| Table | Backend Package | Status |
|---|---|---|
| `care_groups` | `carecoordination.entity` | Future |
| `care_group_members` | `carecoordination.entity` | Future |
| `care_tasks` | `carecoordination.entity` | Future |
| `expenses` | `carecoordination.entity` | Future |
| `reminders` | `reminder.entity` | Future |

## Community & Content

| Table | Backend Package | Status |
|---|---|---|
| `community_topics` | `community.entity` | Current |
| `community_questions` | `community.entity` | Current |
| `community_answers` | `community.entity` | Current |
| `contribution_points` | `community.entity` | Current |
| `content_items` | `content.entity` | Current |
| `content_reports` | `content.entity` | Current |
| `moderation_actions` | `content.entity` | Current |
| `checklist_templates` | `content.entity` | Current |
| `checklist_items` | `content.entity` | Current |

## Expert & Consultation

| Table | Backend Package | Status |
|---|---|---|
| `expert_profiles` | `expert.entity` | Future |
| `expert_credentials` | `expert.entity` | Future |
| `expert_availability` | `expert.entity` | Future |
| `expert_location_shares` | `expert.entity` | Future |
| `expert_reviews` | `expert.entity` | Future |
| `consultation_bookings` | `consultation.entity` | Future |
| `consultation_sessions` | `consultation.entity` | Future |
| `consultation_messages` | `consultation.entity` | Future |

## Payment

| Table | Backend Package | Status |
|---|---|---|
| `payment_transactions` | `payment.entity` | Future |
| `commission_records` | `payment.entity` | Future |
| `consultation_price_bands` | `payment.entity` | Future |
| `expert_consultation_prices` | `payment.entity` | Future |
| `consultation_disputes` | `payment.entity` | Future |
| `refund_records` | `payment.entity` | Future |
| `settlement_records` | `payment.entity` | Future |

## AI & Triage

| Table | Backend Package | Status |
|---|---|---|
| `triage_assessments` | `triage.entity` | Current |
| `triage_answers` | `triage.entity` | Current |

## Partner & Location

| Table | Backend Package | Status |
|---|---|---|
| `partner_organizations` | `partner.entity` | Current |
| `partner_expert_links` | `partner.entity` | Future |
| `partner_services` | `partner.entity` | Future |
| `sponsored_campaigns` | `partner.entity` | Future |
| `care_facilities` | `partner.entity` | Future |
| `emergency_events` | `emergency.entity` | Future |
| `location_snapshots` | `emergency.entity` | Future |

## Device & Smart Safety

| Table | Backend Package | Status |
|---|---|---|
| `health_device_connections` | `device.entity` | Future |
| `device_measurements` | `device.entity` | Future |
| `safety_monitoring_settings` | `safety.entity` | Future |
| `safety_events` | `safety.entity` | Future |
| `safety_alerts` | `safety.entity` | Future |

## Pregnancy Exercise & Posture

| Table | Backend Package | Status |
|---|---|---|
| `pregnancy_exercises` | `exercise.entity` | Future |
| `exercise_safety_checks` | `exercise.entity` | Future |
| `exercise_sessions` | `exercise.entity` | Future |
| `posture_analysis_configs` | `exercise.entity` | Future |
| `posture_feedback_events` | `exercise.entity` | Future |

---

## Count Summary

| Category | Count |
|---|---|
| Current (has @Entity) | 25 |
| Future (schema-only) | 46 |
| **Total** | **71** |

---

## Rules for Future Implementation

1. When implementing a "Future" domain, create the @Entity in the owning package.
2. Do NOT create a V2+ migration for the table itself — it already exists.
3. DO create a V2+ migration for any NEW FK constraints you want added between
   existing tables (e.g., `author_id → users` for community tables).
4. Add any new columns the @Entity requires via a V2+ migration (ALTER TABLE ADD COLUMN).
5. Update this file status column from "Future" to "Current" when done.
