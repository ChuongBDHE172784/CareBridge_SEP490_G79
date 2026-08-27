# CareBridge Code-First Use Case Catalogue

> Status: **Draft**  
> Baseline date: **2026-08-23**  
> Completed/reachable actor-goal count: **88** (derived from code; not normalized to 91).

| ID | Use case | Domain | Actor | Confidence | Canonical specification folder |
| --- | --- | --- | --- | --- | --- |
| UC-AC-01 | Register and Verify Account | Access, Identity, and Trust | Guest | High | `UC-AC-01-RegisterAndVerifyAccount/` |
| UC-AC-02 | Authenticate and End Current Session | Access, Identity, and Trust | Registered User | High | `UC-AC-02-AuthenticateAndEndCurrentSession/` |
| UC-AC-03 | View and Revoke Login Sessions | Access, Identity, and Trust | Authenticated User | High | `UC-AC-03-ViewAndRevokeLoginSessions/` |
| UC-AC-04 | Recover Forgotten Password | Access, Identity, and Trust | Guest | Medium | `UC-AC-04-RecoverForgottenPassword/` |
| UC-AC-05 | Change Password | Access, Identity, and Trust | Authenticated User | Medium | `UC-AC-05-ChangePassword/` |
| UC-AC-06 | View and Edit Account Profile | Access, Identity, and Trust | Authenticated User | High | `UC-AC-06-ViewAndEditAccountProfile/` |
| UC-AC-07 | Link Google Identity | Access, Identity, and Trust | Authenticated User | High | `UC-AC-07-LinkGoogleIdentity/` |
| UC-AC-08 | View and Acknowledge Notifications | Access, Identity, and Trust | Authenticated User | High | `UC-AC-08-ViewAndAcknowledgeNotifications/` |
| UC-AC-09 | Manage Privacy Settings and Consent Grants | Access, Identity, and Trust | Authenticated User | High | `UC-AC-09-ManagePrivacySettingsAndConsent/` |
| UC-AC-10 | Deactivate Own Account | Access, Identity, and Trust | Authenticated User | High | `UC-AC-10-DeactivateOwnAccount/` |
| UC-AC-11 | Submit Account Lock Appeal | Access, Identity, and Trust | Blocked User | High | `UC-AC-11-SubmitAccountLockAppeal/` |
| UC-EX-01 | Create Expert Profile and Select Expert Type | Expert and Consultation | Expert Applicant | High | `UC-EX-01-CreateExpertProfileAndType/` |
| UC-EX-02 | Review and Accept Expert Contract | Expert and Consultation | Expert Applicant | Medium | `UC-EX-02-ReviewAndAcceptExpertContract/` |
| UC-EX-03 | Verify Expert Identity and Face | Expert and Consultation | Expert Applicant | High | `UC-EX-03-VerifyExpertIdentityAndFace/` |
| UC-EX-04 | Submit Credentials and Track Verification | Expert and Consultation | Expert Applicant | High | `UC-EX-04-SubmitCredentialsAndTrackVerification/` |
| UC-EX-05 | Manage Professional Profile | Expert and Consultation | Verified Expert | Medium | `UC-EX-05-ManageProfessionalProfile/` |
| UC-EX-06 | Manage Availability Calendar | Expert and Consultation | Verified Expert | High | `UC-EX-06-ManageAvailabilityCalendar/` |
| UC-EX-07 | Browse Expert Directory and Public Profile | Expert and Consultation | Mother / Family | High | `UC-EX-07-BrowseExpertDirectory/` |
| UC-EX-08 | Mother Creates and Manages Consultation Request | Expert and Consultation | Mother | High | `UC-EX-08-MotherManagesConsultationRequest/` |
| UC-EX-09 | Expert Processes Consultation Requests | Expert and Consultation | Verified Expert | Medium | `UC-EX-09-ExpertProcessesConsultationRequests/` |
| UC-EX-10 | Exchange Direct Messages and Attachments | Expert and Consultation | Mother / Expert | High | `UC-EX-10-ExchangeDirectMessages/` |
| UC-EX-11 | Make and Receive Voice/Video Calls | Expert and Consultation | Mother / Expert | High | `UC-EX-11-MakeAndReceiveDirectCalls/` |
| UC-EX-12 | Review Shared Maternal Metrics and Checklists | Expert and Consultation | Authorized Expert | Medium | `UC-EX-12-ReviewSharedMaternalCare/` |
| UC-MH-01 | Complete Journey Consent and Stage Onboarding | Mother Journey and Health | Mother | High | `UC-MH-01-CompleteJourneyOnboarding/` |
| UC-MH-02 | Create or Update Maternal Journey | Mother Journey and Health | Mother | High | `UC-MH-02-CreateOrUpdateMaternalJourney/` |
| UC-MH-03 | Record Pregnancy Outcome and Transition | Mother Journey and Health | Mother | High | `UC-MH-03-RecordPregnancyOutcome/` |
| UC-MH-04 | View Journey Dashboard, History, and Timeline | Mother Journey and Health | Mother | High | `UC-MH-04-ViewJourneyTimeline/` |
| UC-MH-05 | Manage Recommendation Profile | Mother Journey and Health | Mother | High | `UC-MH-05-ManageRecommendationProfile/` |
| UC-MH-06 | Browse Personalized Recommendations | Mother Journey and Health | Mother | High | `UC-MH-06-BrowsePersonalizedRecommendations/` |
| UC-MH-07 | Record and Review General Maternal Metrics | Mother Journey and Health | Mother | High | `UC-MH-07-ManageMaternalMetrics/` |
| UC-MH-08 | Track Fetal Movement Sessions | Mother Journey and Health | Mother | High | `UC-MH-08-TrackFetalMovement/` |
| UC-MH-09 | Track Hydration | Mother Journey and Health | Mother | Medium | `UC-MH-09-TrackHydration/` |
| UC-MH-10 | Complete and Review EPDS Screening | Mother Journey and Health | Mother | High | `UC-MH-10-CompleteEpdsScreening/` |
| UC-MH-11 | Request AI Health Overview Screening | Mother Journey and Health | Mother | High | `UC-MH-11-RequestAIHealthScreening/` |
| UC-MH-12 | Manage Health Records and Attachments | Mother Journey and Health | Mother / Authorized Family / Expert | High | `UC-MH-12-ManageHealthRecordsAndAttachments/` |
| UC-MH-13 | Manage Appointments and Calendar | Mother Journey and Health | Mother / Authorized Family | High | `UC-MH-13-ManageAppointments/` |
| UC-MH-14 | Manage General, Medication, and Vaccination Reminders | Mother Journey and Health | Mother / Authorized Family | High | `UC-MH-14-ManageReminders/` |
| UC-MH-15 | Manage Recurring Reminder Schedules | Mother Journey and Health | Mother / Authorized Family | High | `UC-MH-15-ManageReminderSchedules/` |
| UC-MH-16 | Manage Personal Checklist and Roadmap | Mother Journey and Health | Mother / Authorized Family | High | `UC-MH-16-ManagePersonalChecklist/` |
| UC-MH-17 | View and Act on Unified Today Tasks | Mother Journey and Health | Mother / Authorized Family | High | `UC-MH-17-ActOnUnifiedTodayTasks/` |
| UC-MH-18 | Browse Exercises and Complete Safety Check | Mother Journey and Health | Mother | High | `UC-MH-18-BrowseExercisesAndSafetyCheck/` |
| UC-MH-19 | Perform Exercise Session and Review Results | Mother Journey and Health | Mother | High | `UC-MH-19-PerformExerciseSession/` |
| UC-BC-01 | Manage Baby Profiles | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-01-ManageBabyProfiles/` |
| UC-BC-02 | View Baby Care Hub and Detail Overview | Baby Care | Mother / Authorized Caregiver | Medium | `UC-BC-02-ViewBabyCareHub/` |
| UC-BC-03 | Manage Baby Daily Logs | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-03-ManageBabyDailyLogs/` |
| UC-BC-04 | Review 24-Hour Daily Log Summary | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-04-ReviewDailyLogSummary/` |
| UC-BC-05 | Manage Growth Measurements and Chart | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-05-ManageGrowthMeasurements/` |
| UC-BC-06 | Manage Development Milestones | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-06-ManageDevelopmentMilestones/` |
| UC-BC-07 | Manage Vaccination Records | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-07-ManageVaccinationRecords/` |
| UC-BC-08 | Review Vaccination Schedule and Create Next-Dose Reminder | Baby Care | Mother / Authorized Caregiver | High | `UC-BC-08-ReviewVaccinationSchedule/` |
| UC-CO-01 | Browse and Search Community Q&A | Community and Content Consumption | Authenticated User | High | `UC-CO-01-BrowseCommunityQA/` |
| UC-CO-02 | Manage Own Community Questions | Community and Content Consumption | Authenticated User | High | `UC-CO-02-ManageOwnCommunityQuestions/` |
| UC-CO-03 | Answer Community Questions | Community and Content Consumption | Authenticated User / Eligible Expert | High | `UC-CO-03-AnswerCommunityQuestions/` |
| UC-CO-04 | Like, Bookmark, and Follow Community Content | Community and Content Consumption | Authenticated User | High | `UC-CO-04-EngageWithCommunityContent/` |
| UC-CO-05 | Browse Verified Health Content | Community and Content Consumption | Authenticated User | High | `UC-CO-05-BrowseVerifiedHealthContent/` |
| UC-CO-06 | Report Community Content or Account | Community and Content Consumption | Authenticated User | High | `UC-CO-06-ReportCommunityTarget/` |
| UC-AI-01 | Use AI Nurse RAG Chat | AI Nurse and Clinical Assistance | Mother / Family where allowed | High | `UC-AI-01-UseAINurseRagChat/` |
| UC-ES-01 | Find and Navigate to Care Facility | Emergency and Safety | Mother / Family | High | `UC-ES-01-FindAndNavigateCareFacility/` |
| UC-ES-02 | Start and Resolve Emergency Session | Emergency and Safety | Mother | High | `UC-ES-02-StartAndResolveEmergency/` |
| UC-ES-03 | Respond to Family Emergency Alert | Emergency and Safety | Authorized Family Member | High | `UC-ES-03-RespondToFamilyEmergency/` |
| UC-ES-04 | Configure and Test Fall Detection | Emergency and Safety | Mother | High | `UC-ES-04-ConfigureFallDetection/` |
| UC-ES-05 | Respond to Detected Fall or Impact | Emergency and Safety | Mother | High | `UC-ES-05-RespondToDetectedFall/` |
| UC-FM-01 | Manage Care Group Lifecycle | Family Cooperative Care | Mother / Group Owner / Member | High | `UC-FM-01-ManageCareGroupLifecycle/` |
| UC-FM-02 | Manage Invitations, Join Requests, and Membership | Family Cooperative Care | Group Owner / Invitee / Applicant | High | `UC-FM-02-ManageCareGroupMembership/` |
| UC-FM-03 | Manage Family Member Permissions | Family Cooperative Care | Mother / Group Owner | High | `UC-FM-03-ManageFamilyPermissions/` |
| UC-FM-04 | Assign and Track Family Care Tasks | Family Cooperative Care | Mother / Authorized Family | High | `UC-FM-04-AssignAndTrackCareTasks/` |
| UC-FM-05 | Monitor Shared Family Care | Family Cooperative Care | Authorized Family Member / Mother | High | `UC-FM-05-MonitorSharedFamilyCare/` |
| UC-AD-01 | Manage User Accounts and Roles | Administration and Operations | System Admin | High | `UC-AD-01-ManageUsersAndRoles/` |
| UC-AD-02 | Provision Staff Accounts | Administration and Operations | System Admin | High | `UC-AD-02-ProvisionStaffAccounts/` |
| UC-AD-03 | Review Account Lock Appeals | Administration and Operations | System Admin | High | `UC-AD-03-ReviewAccountLockAppeals/` |
| UC-AD-04 | Inspect Audit Activity | Administration and Operations | System Admin / Authorized Operations | High | `UC-AD-04-InspectAuditActivity/` |
| UC-AD-05 | Configure System and Maintenance Mode | Administration and Operations | System Admin | High | `UC-AD-05-ConfigureSystem/` |
| UC-AD-06 | Verify Experts and Credentials | Administration and Operations | System Admin / Authorized Reviewer | High | `UC-AD-06-VerifyExpertsAndCredentials/` |
| UC-AD-07 | Oversee Consultation Calls and Recordings | Administration and Operations | System Admin | High | `UC-AD-07-OverseeConsultationCalls/` |
| UC-AD-08 | Author and Version Articles and FAQs | Administration and Operations | Content Admin | High | `UC-AD-08-AuthorArticlesAndFaqs/` |
| UC-AD-09 | Manage Community Taxonomy | Administration and Operations | Content Admin / Authorized Moderator | High | `CommunityTopicManagement/` |
| UC-AD-10 | Author Checklist Templates | Administration and Operations | Content Admin | High | `UC-AD-10-AuthorChecklistTemplates/` |
| UC-AD-11 | Review, Approve, and Activate Checklist Versions | Administration and Operations | System Admin | High | `UC-AD-11-ApproveChecklistVersions/` |
| UC-AD-12 | Manage Exercise Catalogue | Administration and Operations | Content Admin | High | `UC-AD-12-ManageExerciseCatalogue/` |
| UC-AD-13 | Manage Posture Analysis Configuration | Administration and Operations | System Admin | High | `UC-AD-13-ManagePostureConfiguration/` |
| UC-AD-14 | Approve or Reject Submitted Content | Administration and Operations | System Admin | High | `UC-AD-14-ApproveSubmittedContent/` |
| UC-AD-15 | Unpublish or Archive Content | Administration and Operations | Content Admin where allowed | High | `UC-AD-15-UnpublishOrArchiveContent/` |
| UC-AD-16 | Moderate Pending and Visible Community Content | Administration and Operations | Moderator | High | `UC-AD-16-ModerateCommunityContent/` |
| UC-AD-17 | Claim and Resolve User Reports | Administration and Operations | Moderator | High | `UC-AD-17-ResolveUserReports/` |
| UC-AD-18 | Manage Account Violations | Administration and Operations | Moderator | High | `UC-AD-18-ManageAccountViolations/` |
| UC-AD-19 | Configure AI Moderation Policies | Administration and Operations | System Admin | High | `UC-AD-19-ConfigureAIModerationPolicies/` |
| UC-AD-20 | Manage AI Knowledge Base | Administration and Operations | Authorized Technical Operator | High | `UC-AD-20-ManageAIKnowledgeBase/` |
| UC-AD-21 | Run AI Diagnostic and Clinical Simulators | Administration and Operations | Authorized Technical Operator | High | `UC-AD-21-RunAIDiagnostics/` |

## Partial / API-only / unreachable capabilities

| Item | Disposition |
| --- | --- |
| Notification preferences self-service UI | Partial — backend GET/PUT exists, but Mobile profile navigation is TODO and Web shortcuts are static. |
| Health summary build/share | API-only — `/api/v1/health-summaries/**` has no current Mobile/Web consumer. |
| Expense tracking | API-only — `/api/v1/expenses/**` has backend/tests but no current client consumer. |
| Baby care-overview and care-timeline projections | API-only — both routes have no client consumer; current hubs compose underlying resources. |
| Baby appointment-preparation summary | API-only — no current client consumer. |
| Vaccination completion and postponement operations | API/service-only — completion has a Mobile service method but no reachable UI caller, and postponement has no current client caller; neither is promoted into the completed vaccination UCs. |
| Care-facility verification | API-only/Partial — pending/verify endpoints exist but no dedicated Web route; do not claim a completed portal UC. |
| Structured AI triage session/history/handoff | Partial backend infrastructure — no reachable Mobile intake starts or continues triage sessions; client service methods alone do not prove an actor entry flow. The Python `/internal/triage/turn` handler is a compatibility bridge for this incomplete path, not the implemented AI Nurse RAG chat. |
| Spring triage evidence-source governance | API/Swagger-only — administrative/internal evidence-source endpoints have no current role portal and are not the Python AI knowledge-base UC. |
| Expert contribution points | API-only/not reachable from current clients. |
| Expert location sharing and online toggle | Service/API capability without a reachable current UI action; imported service methods do not establish an actor goal. |
| System-admin moderation escalation UI | API-only — backend escalation contract has no current Web consumer. |
| Bulk/paged login-session administration for self-service | API/service-only — bulk revoke and paged list have no current screen caller; UC-AC-03 covers reachable list/single revoke only. |
| Direct notification dispatch endpoint | Supporting/internal API — clients register/deregister tokens and read notifications, but no actor screen directly invokes arbitrary `/notifications/send`. |
| Emergency location snapshots and handoff history | API-only — snapshot and handoff-history routes have no current Mobile/Web consumer; reachable navigation handoff remains UC-ES-01. |
| Credential issuer catalogue | API-only — issuer lookup exists without a current client consumer. |
| Global cross-domain search | API-only — `/api/v1/search` has no current client consumer. |
| Lifecycle checklist content projection | API-only — `/api/v1/content/lifecycle/checklists` is covered by backend tests but has no current Mobile/Web consumer. |
| Legacy verified-expert list projection | API-only — `/api/v1/expert/verified` has no current client consumer; the reachable consumer flow uses the expert directory contract. |
| Development-only manual data seed | Development supporting endpoint — `/api/manual-seed` is enabled only by the dev profile and explicit property; it is not a production actor-goal use case. |
| Operational health and checklist E2E attestation | Operational diagnostics only — AI-triage health, posture-sidecar health, and checklist environment attestation are not actor-goal product use cases. |

## Supplemental hand-authored implementation evidence

These folders remain because they contain deeper cross-UC or technical-subflow evidence still related to current code. Their files are deliberately named `*-Evidence.md`, not `*_TDS.md` or `*_Test-Spec.md`: they are not additional completed UC IDs and are non-authoritative when they conflict with current code or the canonical code-first pair.

| Folder | Canonical UC mapping | Disposition |
| --- | --- | --- |
| `04_Implement/AIContentModeration` | `UC-AD-17`, `UC-AD-19` | Deep AI-assessment/moderation implementation history; current policy and controller code override conflicts. |
| `04_Implement/ChecklistDistributionE2E` | `UC-AD-10`, `UC-AD-11`, `UC-MH-17` | Cross-UC checklist distribution/reconciliation evidence, not a separate actor-goal UC. |
| `04_Implement/CommunityQuestionLike` | `UC-CO-04` | Deep question-like subflow specification. |
| `04_Implement/ContentImageOrphanCleanup` | `UC-AD-08` | Technical media-cleanup subflow supporting content authoring/versioning. |
| `04_Implement/ContentRichTextEditor` | `UC-AD-08` | Technical rich-text/media authoring subflow. |
| `04_Implement/EpdsFamilyNotification` | `UC-MH-10`, `UC-AC-08` | Cross-UC EPDS event/notification delivery detail. |
| `04_Implement/ExpertConsultationRequests` | `UC-EX-08`, `UC-EX-09` | Deep consultation-request lifecycle implementation history. |
| `04_Implement/ModeratorReportsRevert` | `UC-AD-17` | Deep resolved-report revert subflow. |
| `04_Implement/MotherExpertDiscoveryInbox` | `UC-EX-07`, `UC-EX-10` | Cross-UC directory-to-conversation navigation/detail. |
| `04_Implement/UndoModerationAction` | `UC-AD-16`, `UC-AD-18` | Shared moderation undo subflow. |

## Generation and validation

```bash
python3 scripts/docs/generate_code_first_use_case_specs.py --check
python3 scripts/docs/generate_code_first_use_case_specs.py
```

UC-AD-09 intentionally points to the hand-authored `CommunityTopicManagement` pair, which remains the canonical detailed baseline. Generated documents remain Draft and do not claim unexecuted tests as Green.

The generated `04_Implement/ROUTE_COVERAGE_AUDIT.md` proves the disposition of every parsed Spring/FastAPI controller handler as completed/reachable or explicit Partial/API-only/supporting evidence.
