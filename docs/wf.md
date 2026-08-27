
#### ***1.2.1 Maternal Care Plan Workflow***

![][image4]

**Figure 3: Maternal Care Plan Workflow**

**Image Detail: [Maternal Care Plan Workflow](mainworkflow-Trang-3.drawio.png)**

**Description**

| \# | Step Name | Detail Description | Role | Note |
| --- | --- | --- | --- | --- |
| 1 | Register Account / Login | The mother registers a new account or logs in to access the CareBridge platform. | Mother | Entry point of the maternal care workflow. |
| D1 | Already have an account? | The system checks whether the user already has an existing account and initialized care profile. | System | If Yes, navigates directly to Step 5 (Receive daily plans). If No, proceeds to Step 2 (Send Health & Stage Survey). |
| 2 | Send Health & Stage Survey | The system presents a health and lifecycle-stage survey to collect maternal profile data. | System | Supports preconception, pregnancy, and postpartum stages. |
| 3 | Select Stage & Fill Health Survey (Preconception, Pregnancy, Postpartum) | The mother selects her current lifecycle stage and completes the initial health questionnaire. | Mother | The selected stage and survey responses establish the baseline maternal context. |
| 4 | Save Information & Generate Care Journey | The system securely stores the health profile and generates a personalized maternal care journey tailored to the selected stage. | System | Establishes the foundation for daily care plans and automated support. |
| 5 | Receive daily plans | The mother accesses her personalized daily care plan containing daily tasks, health schedules, reminders, and stage-specific content. | Mother | Central recurring hub updated according to stage progress, logged metrics, and doctor approvals. |
| 5A | Recommend Tailored Articles | The system analyzes the mother's profile and current health state to curate relevant educational articles. | System | Pushes evidence-based knowledge to the mother. |
| 5B | Recommended Articles | The mother views and reads the recommended health and educational articles. | Mother | Supports maternal self-education and wellness awareness. |
| 6A | Create reminders, Appointments, Save documents & Exercise | The mother schedules health reminders, logs medical appointments, uploads clinical documents, and views recommended physical exercises. | Mother | Manages proactive self-care and appointment tracking. |
| 6A1 | Create reminder notifications, save documents & suitable exercise | The system creates automated push reminder notifications, securely archives uploaded medical documents, and provides suitable stage-appropriate exercise regimens. | System | Keeps schedules synchronized and documents accessible. |
| 6B | Activate IMU | The mother enables the smartphone's IMU (Inertial Measurement Unit) sensor monitoring. | Mother | Activates background physical fall and movement safety monitoring. |
| Daemon | Fall detection daemon | The system runs a background daemon analyzing real-time accelerometer and gyroscope sensor telemetry to detect accidental falls. | System | Operates continuously in the background for safety assurance. |
| SOS | Trigger Auto SOS Alert & Broadcast GPS Location | Upon detecting a severe fall impact or emergency trigger, the system automatically dispatches an SOS alert with real-time GPS location coordinates. | System | Broadcasts urgent emergency alerts to family members. |
| Family-1 | Receive Emergency Fall Notification, Open Navigation Map | The family member receives the urgent fall notification and opens the navigation map with live GPS tracking to locate and assist the mother. | Family | Enables rapid real-world emergency response. |
| 6 | Log Health Metrics | The mother logs routine daily health metrics (e.g., blood pressure, weight, blood glucose, symptoms, fetal kicks). | Mother | Provides continuous data for maternal health tracking and AI anomaly analysis. |
| 7 | AI Detects Health Anomaly / Risk? | The AI monitoring engine evaluates logged metrics against clinical thresholds and historical baselines to detect abnormal patterns. | System | If No (Normal), the mother continues daily care (loops to Step 5). If Yes (Anomaly), proceeds to Step 8. |
| 8 | Emergency / Critical Risk Detected? | The system evaluates whether the detected anomaly represents a life-threatening or critical medical emergency. | System | If Yes (Critical), proceeds to Step 9A. If No (Non-critical), proceeds to Step 9 for AI Nurse triage. |
| 9A | Trigger Medical Emergency Mode | The system immediately activates Medical Emergency Mode and alerts designated emergency contacts. | System | Initiates urgent escalation protocols for critical cases. |
| 9B | View Nearest Hospital List, Call 115 / Hospital Hotline, Open Navigation Map | The mother views a list of nearby hospitals, can one-tap call 115 or emergency hotlines, and opens turn-by-turn navigation map directions. | Mother | Provides instant access to emergency hospital care and rapid calling shortcuts. |
| 9 | Clarify Symptoms with AI Nurse Assistant (RAG Chat) | The mother interacts with the AI Nurse Assistant via RAG-powered chat to clarify non-critical symptoms and receive preliminary triage guidance. | Mother | Provides immediate conversational health support and risk triage (not a definitive medical diagnosis). |
| 10 | Need Expert Consultation? | The system and mother assess whether professional healthcare consultation is recommended based on symptom severity. | System / Mother | If No, proceeds to Step 11A. If Yes, proceeds to Step 11B. |
| 11A | Self-tracking requirement & Disclaimer | The system issues self-tracking instructions, safety disclaimers, and guidelines for continued home monitoring. | System | Routes to Step 17 to update and continue the care plan. |
| 11B | Recommend Available Experts | The system recommends verified medical specialists and obstetricians available for teleconsultation. | System | Also serves as fallback if a booking request is rejected in Step 14. |
| 12 | Select Specialist & Confirm Booking (Chat / Video) | The mother selects an expert, chooses the consultation mode (Chat or Video), and confirms the booking request. | Mother | Transmits the consultation booking request to the selected specialist. |
| 13 | Receive Consultation Request Notification | The specialist receives a notification with the mother's consultation request and relevant triage summary context. | Expert | Informs the doctor of the pending patient appointment. |
| 14 | Accept Booking? | The specialist reviews the request and decides whether to accept or decline the consultation. | Expert | If No, routes back to Step 11B to select another expert. If Yes, proceeds to Step 15. |
| 15 | Conduct Chat / Video Teleconsultation Session | The mother and specialist conduct a live remote teleconsultation session via chat or video call. | Mother / Expert | Facilitates remote clinical evaluation and medical advice. |
| 16 | Prescribe & Authorize Updated Personalized Care Plan | The specialist prescribes updated clinical recommendations, modifies daily care activities, and authorizes the new personalized care plan. | Expert | Official medical adjustment approved by a verified healthcare professional. |
| 17 | Receive & Apply Updated Care Plan | The mother receives the updated, doctor-approved personalized care plan (or self-care guidelines) and integrates it into her daily routine. | Mother | The new plan is applied, looping back to Step 5 (Receive daily plans). |

**Table 4: Maternal Care Plan Workflow Description**

#### ***1.2.2 Baby Care Journey & Growth Workflow***

![][image5]    

**Figure 4: Baby Care & Growth Workflow**

**Image Detail: [Baby Care & Growth Workflow](https://drive.google.com/file/d/17y0IdwbEjvbSdFhjNlHi0TEbACsvEN1W/view?usp=drive_link)**

**Description**

| \#       | Step Name                                                                    | Detail Description                                                                                                                                  | Role            | Note                                                                                                                |
| -------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------- |
| 1        | Create baby profile                                                          | The mother creates a profile for the baby to begin the baby care and growth workflow.                                                               | Mother          | The baby profile provides the information required to generate the Baby Care Plan.                                  |
| 2        | Generate Baby Care Plan                                                      | The system uses the baby profile information to generate a Baby Care Plan for the active baby profile.                                              | System          | This step initializes the plan used in the recurring baby-care cycle.                                               |
| 3        | Receive Daily Baby Care Plan                                                 | The mother receives the current Daily Baby Care Plan and uses it as the starting point for daily care activities.                                   | Mother          | After Step 10, the workflow loops back to this step for the next care cycle.                                        |
| 4        | Record feeding, sleep, diaper, symptom or night-mode journal                 | The mother records daily baby-care information, including feeding, sleep, diaper activity, symptoms or night-mode journal entries.                  | Mother          | These entries support observation and care continuity; they are not medical diagnoses.                              |
| 5        | Add growth measurement or development milestone                              | The mother records a supported growth measurement or an observed development milestone for the active baby profile.                                 | Mother          | A measurement or milestone may be added when applicable and is not necessarily entered every day.                   |
| 6        | Store baby logs, measurements and milestone history                          | The system stores the submitted baby-care journals, growth measurements and development milestone history for the active baby profile.              | System          | Stored information is retained for later review and timeline display.                                               |
| 7        | Apply age/stage reference labels for observation only                        | The system applies age- or stage-based reference labels to the stored information to support observation.                                           | System          | Reference labels are informational only and must not be interpreted as a diagnosis or developmental assessment.     |
| 8        | Add vaccination record or view source-labelled reference schedule            | The mother may add a vaccination record for the baby or view a source-labelled vaccination reference schedule.                                      | Mother          | The reference schedule is informational; actual vaccination records remain associated with the active baby profile. |
| 9        | Send due/overdue vaccination or appointment reminder                         | The system sends a reminder when a vaccination or appointment is due or overdue based on the available schedule and recorded information.           | System          | The reminder supports follow-up and appointment preparation and does not replace professional advice.               |
| 10       | Review active baby journals, growth chart, milestones and vaccination status | The mother reviews the active baby's journals, growth chart, recorded milestones and vaccination status.                                            | Mother          | After review, the workflow returns to the Daily Baby Care Plan for the next recurring care cycle.                   |
| Boundary | Workflow boundary                                                            | The workflow supports observation and appointment preparation. It does not diagnose conditions, assess child development or replace pediatric care. | System / Mother | This boundary applies to all activities in the workflow.                                                            |

Table 5: Baby Care Journey & Growth Workflow Description

#### ***1.2.3 Community Q\&A & Moderation Workflow***

![][image6]    

**Figure 5: Community Q\&A & Moderation Workflow**

**Image Detail: [Community Q\&A & Moderation Workflow](https://drive.google.com/file/d/1UYKPn3URS6-9q9D5B_EWLTjeEalPANcd/view?usp=drive_link)**

**Description**

| \#  | Step Name                                                | Detail Description                                                                                                                                                | Role                   | Note                                                                                                                      |
| --- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| 1   | View feed, search/filter topics or open question detail  | The mother or family member views the community feed, searches or filters topics, or opens a question detail screen.                                              | Mother / Family Member | This is the entry point for browsing community content before creating a question or submitting an answer.                |
| 2   | Create question or submit answer                         | The mother or family member creates a new community question or submits an answer to an existing question, then chooses a public or anonymous community identity. | Mother / Family Member | The content is assigned status AI\_PENDING and remains visible only to the author until automated screening is completed. |
| 3   | Async AI scan for spam, abuse and prohibited advertising | The system asynchronously screens the submitted content for spam, abusive content and prohibited advertising.                                                     | System                 | The AI scan is a preliminary screening step and does not directly apply moderator enforcement actions.                    |
| 4   | Potential policy violation?                              | The system determines whether the AI scan indicates a potential violation of community policy.                                                                    | System                 | If No, the content proceeds directly to approval and publication. If Yes, the content is sent for moderator review.       |
| 5   | Moderator reviews flagged content and available evidence | The community moderator reviews the flagged content and any available evidence to determine whether the content complies with community policy.                   | Community Moderator    | While under review, the content status is PENDING and the content requires moderation.                                    |
| 6   | Approve content?                                         | The community moderator decides whether the reviewed content may be approved for publication.                                                                     | Community Moderator    | If Yes, the workflow proceeds to Step 7A. If No, it proceeds to Step 7B.                                                  |
| 7A  | Publish safe content                                     | The system publishes approved content to the community feed, list or question-detail screen.                                                                      | System                 | The content status is updated to APPROVED. This step is also used when the AI scan finds no potential policy violation.   |
| 7B  | Apply moderation action and record outcome               | The community moderator may hide, reject, warn, suspend or request an edit, and records the moderation outcome for audit purposes.                                | Community Moderator    | Depending on the action, the content status may be DELETED, HIDDEN or LOCKED.                                             |
| 8   | Notify content outcome and retain traceable event        | The system sends the publication or moderation outcome to the content author and retains a traceable event for audit and review.                                  | System                 | Both the approved and moderated branches converge at this step.                                                           |
| 9   | Receive content status notification                      | The mother or family member receives a notification showing the current status or moderation result of the submitted question or answer.                          | Mother / Family Member | The workflow ends after the author receives the content status notification.                                              |

Table 6: Community Q\&A & Moderation Workflow Description

#### ***1.2.4 Verified Expert Network & Contribution Workflow***

**![][image7]** 

**Figure 6: Verified Expert Network & Contribution Workflow**

**Image Detail: [Verified Expert Network & Contribution Workflow](https://drive.google.com/file/d/16oIudra1rP--51sKMxwmWPkO6pkIO1LS/view?usp=drive_link)**

**Description**

|  \#   | Step Name                            | Detail Description                                                                                                                                      |  Role  |                                                      Note                                                       |
| :---: | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- | :----: | :-------------------------------------------------------------------------------------------------------------: |
|   1   | Expert Identity Submission           | The expert uploads a selfie together with the front and back images of the identity document.                                                           | Expert |   All identity files are treated as protected sensitive data and must be submitted before credential review.    |
|   2   | Expert Credential Submission         | The expert submits specialty evidence, professional license files and the corresponding expiration date.                                                | Expert |  The credential package must identify the claimed specialty and include valid, readable supporting documents.   |
|   3   | Add to Pending Review Queue          | The system stores the identity and credential package and adds the application to the pending review queue for an administrator.                        | System |          The application status is set to Pending Review; the expert is not yet displayed as verified.          |
|   4   | Admin Reviews Case                   | The administrator fetches the queued review case and inspects the submitted identity files, credential files and related data.                          | Admin  |       The review action should record the reviewer, review time and assessment result for audit purposes.       |
|  D1   | Identity & Docs Approved?            | The administrator decides whether the submitted identity and professional documents satisfy the verification requirements.                              | Admin  |                                   Yes leads to Step 5B. No leads to Step 5A.                                    |
|  5A   | Request More Info / Reject           | The administrator requests missing or corrected information, or rejects the application, and provides specific reasons for the failed review.           | Admin  |                     The reasons are shown to the expert and retained in the review history.                     |
| 5A-1  | Expert Resubmits Data                | The expert updates the requested files or information and resubmits the application for another verification review.                                    | Expert |         The resubmitted application returns to the identity and credential submission and review flow.          |
|  5B   | Publish Verified Status              | After approval, the system marks the expert profile as active and verified.                                                                             | System |          Only an approved expert with valid, non-expired credentials may display the verified status.           |
|   6   | Expert Community Contribution        | The verified expert answers community questions and shares professional knowledge within the approved specialty scope.                                  | Expert |          All contributions remain subject to community, moderation and professional conduct policies.           |
|   7   | Record Points & Badges               | The system records eligible participation points and badges and updates the expert trust level based on contribution activity.                          | System | Points and badges represent participation and trust signals, not a guarantee of clinical competence or outcome. |
|  D2   | Policy Violation or License Expired? | The system or administrator determines whether a policy violation has occurred or the expert license has expired.                                       | Admin  |              Yes leads to Step 8A. No ends the workflow with the current verified state unchanged.              |
|  8A   | Admin Reviews Incident               | The administrator reviews the incident, evaluates its severity and decides the appropriate enforcement action.                                          | Admin  |                 The decision should be supported by evidence and recorded in the audit history.                 |
|  8B   | Restrict & Suspend                   | The system applies the administrator's decision by hiding the badge, restricting contributions, revoking verification or suspending the expert account. | System |   The applied restriction ends the workflow and the expert profile is updated according to the final action.    |

**Table 7: Verified Expert Network & Contribution Workflow Description**
