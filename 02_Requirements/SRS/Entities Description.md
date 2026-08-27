# Entities Description

This document describes the 56 business entities in the CareBridge logical data model.

| No. | Entity | Description |
| ---: | --- | --- |
| 1 | `CareSubject` | Represents a care recipient in the system, such as a mother or baby, together with identity and data ownership information. |
| 2 | `MotherJourney` | Represents a mother's care journey across stages such as preconception, pregnancy, and postpartum. |
| 3 | `CareGroup` | Represents a coordinated care group involving the mother, family members, and other authorized supporters. |
| 4 | `CareItemTemplate` | Defines reusable care content used to create checklists, tasks, guidance, or stage-specific materials. |
| 5 | `CareTask` | Represents a specific care task that can be scheduled, assigned, and tracked through completion. |
| 6 | `ChecklistInstance` | Represents a checklist distributed to a recipient for a specific journey or care-subject context. |
| 7 | `ChecklistTaskInstance` | Represents an actionable item within a distributed checklist, including its content snapshot and completion status. |
| 8 | `ChecklistActionCommand` | Records a requested checklist or task state change to support consistent and idempotent processing. |
| 9 | `ExpenseEntry` | Represents an expense incurred while caring for a mother or baby. |
| 10 | `HealthRecord` | Represents a health document or record provided by a user and associated with a mother, baby, or care journey. |
| 11 | `HealthObservation` | Records a health metric, measurement, or observation captured at a specific time. |
| 12 | `HealthMetricDefinition` | Defines a health metric's identity, unit, display rules, and applicable scope. |
| 13 | `DevelopmentMilestone` | Records a baby's developmental milestone, including its date, source, and assessment status. |
| 14 | `VaccinationRecord` | Records a baby's planned or completed vaccination dose, administration date, and status. |
| 15 | `VaccinationSchedule` | Defines a reference vaccination schedule by vaccine, dose, and recommended age or timing. |
| 16 | `MaternalExerciseSession` | Records a maternal exercise session, including the exercise, duration, progress, and safety-monitoring results. |
| 17 | `User` | Represents a CareBridge account, including identity, role, public profile, professional information, and security status. |
| 18 | `AccountLockAppeal` | Represents a request to review an account lock and the administrator's final decision. |
| 19 | `AuthChallenge` | Represents a time-limited authentication challenge such as an OTP, identity verification, or account activation request. |
| 20 | `AuthSession` | Represents a user login session, including device information, token lifecycle, and revocation status. |
| 21 | `DataPermission` | Represents permission to share or use data between an owner and recipient for a defined scope, purpose, and validity period. |
| 22 | `DirectConversation` | Represents a private communication channel between a mother and an expert. |
| 23 | `DirectMessage` | Represents a message within a direct conversation, including text or an optional attachment. |
| 24 | `ConversationCall` | Records an audio or video call initiated within a direct conversation. |
| 25 | `DeviceToken` | Represents a registered device token used to deliver push notifications to a user. |
| 26 | `NotificationRecord` | Records a notification created for a user or care group, including its content and delivery or read status. |
| 27 | `NotificationJob` | Represents a background job that schedules, retries, and tracks notification delivery. |
| 28 | `Specialty` | Defines a professional specialty that can be assigned to an expert. |
| 29 | `ExpertAvailability` | Represents a time slot and consultation channel in which an expert is available. |
| 30 | `ExpertConsultationRequest` | Represents a user's request to connect with or receive support from an expert. |
| 31 | `ExpertLocationShare` | Records an expert's consent-based location sharing for a defined duration. |
| 32 | `ConsultationBooking` | Represents a scheduled consultation appointment between a requester and an expert. |
| 33 | `ConsultationContextShare` | Records a consent-authorized health or triage context package shared with an expert. |
| 34 | `CommunityTopic` | Defines a topic used to organize community content and interactions. |
| 35 | `CommunityContent` | Represents community-generated content such as questions, answers, or personal-experience posts. |
| 36 | `ContentItem` | Represents managed care or health-education content distributed by the system. |
| 37 | `KnowledgeSource` | Represents a medical or care-related source used to support content and guidance. |
| 38 | `KnowledgeSourceReview` | Records the review, approval, or status change of a knowledge source. |
| 39 | `AiModerationPolicy` | Defines policies and criteria used by AI to classify risky or policy-violating content. |
| 40 | `AiContentScanJob` | Represents an AI content-scanning job, including its target, processing status, and retry information. |
| 41 | `AiContentAssessment` | Stores an AI assessment result, including classification, confidence, evidence, and recommended action. |
| 42 | `ModerationCase` | Represents a moderation case requiring human review and a final enforcement decision. |
| 43 | `HealthContextMemory` | Stores selected health context to maintain continuity across interactions and triage sessions. |
| 44 | `TriageSession` | Represents a symptom-intake and care-urgency classification session that does not replace medical diagnosis. |
| 45 | `TriageSessionEvidence` | Records evidence or citations supporting the result of a triage session. |
| 47 | `SafetyMonitoringSession` | Represents an active safety-monitoring period for a user or connected device. |
| 48 | `SafetyEvent` | Records a safety or emergency event, including verification, response, alerting, and resolution details. |
| 49 | `ReminderSchedule` | Defines recurrence, time zone, and validity rules for generating care reminders. |
| 50 | `ReminderOccurrenceAlias` | Represents the stable identity of a reminder occurrence generated from a reminder definition and schedule. |
| 51 | `AppointmentNotificationConfig` | Defines notification timing and delivery rules for an appointment or care task. |
| 52 | `AdministrativeArea` | Represents a hierarchical administrative division used for location search and geographic scoping. |
| 53 | `CareFacility` | Represents a healthcare or care facility that can be searched, displayed, or used for emergency assistance. |
| 54 | `Attachment` | Represents a file uploaded and associated with a record, message, or supporting evidence. |
| 55 | `AuditEvent` | Records an audit or security event to trace actions, affected subjects, and changes to sensitive data. |
| 56 | `SystemConfiguration` | Represents a versioned system-level operational configuration and its update history. |
