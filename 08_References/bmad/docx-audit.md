# DOCX Source Audit

**Status:** DRAFT  
**Generated:** 2026-06-18  
**Agent:** DOC_ARCHITECT (Claude Sonnet 4.6)  
**Plan:** docs/plans/claude/PLAN-001-docx-to-bmad-docs.md

## 1. Source Artifacts Reviewed

| # | Source File | Format | Status | Notes |
|---|-------------|--------|--------|-------|
| 1 | `01_Requirements/SRS/Report1_Project Introduction.docx.md` | Markdown (converted from DOCX) | ✅ Reviewed | Large file (1.9MB). Contains project introduction, product background, global & Vietnam context, existing systems analysis, business opportunity, product vision, scope & limitations. |
| 2 | `01_Requirements/SRS/Report3_Software Requirement Specification.docx.md` | Markdown (converted from DOCX) | ✅ Reviewed | Large file (512KB). Contains full SRS: context diagrams, main workflows (14 workflows), user requirements (actors & use cases), functional specifications (extensive, covering auth, profile, notifications, community, expert, consultation, AI triage, emergency map, family sync, safety monitoring, exercise, partner content, audit, etc.). |
| 3 | `01_Requirements/SRS/Report2_Project Management Plan.docx.md` | Markdown (converted from DOCX) | ✅ Reviewed | Contains scope & estimation, project objectives, risks, management approach, deliverables, responsibility assignments, communications, configuration management. |
| 4 | `02_Design/Architecture/project-structure-design.md` | Markdown (native) | ✅ Reviewed | Contains explicit architecture decisions: Layered Architecture using Controller-Service-Repository pattern, backend domain package map, frontend structure, mobile structure, database strategy (PostgreSQL as source of truth, Firebase Realtime Database for approved realtime state, object storage for uploaded files), integration services, testing structure, implementation order. |
| 5 | `02_Design/PHASE-3_TDS.md` | Markdown | ❌ Not Found | Test Design Specification document not found in the project. |
| 6 | `02_Design/PHASE-4_Test-Spec.md` | Markdown | ❌ Not Found | Test Specification document not found in the project. |
| 7 | `02_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml` | PlantUML | ⚠️ Not parsed | ERD diagram exists but not parsed for this documentation pass. The architecture document already defines database split strategy. |
| 8 | `02_Design/ActivityDiagram/CareBridge-Main-Workflows.drawio` | Draw.io | ⚠️ Not parsed | Workflow diagrams exist but not parsed. SRS Report 3 already contains textual workflow descriptions. |
| 9 | `02_Design/Database/CareBridge_ERD.drawio` | Draw.io | ⚠️ Not parsed | ERD diagram exists but not parsed. |
| 10 | `AGENTS.md` | Markdown | ✅ Reviewed | Defines multi-agent workflow, approval gates, agent roles (Claude DOC_ARCHITECT, Codex MAIN_DEVELOPER, Gemini QA_REVIEWER_TESTER), plan structure, handoff standards, story approval process. |
| 11 | `README.md` | Markdown | ✅ Reviewed | Project directory structure (SDLC folders), empty folder tracking with `.gitkeep`. |

## 2. Major Requirement Domains Extracted

From Report 1 and Report 3, the following functional domains are clearly defined:

### 2.1 Core User Journeys
- **Authentication & Profile Management**: Registration, OTP verification, login/logout, password management, profile viewing/updating.
- **Mother & Baby Care Journey**: Pre-pregnancy, pregnancy, postpartum tracking; baby profile; health records; growth tracking; vaccination tracking.
- **Reminders & Care Tasks**: Appointment reminders, medication reminders, checkup schedules, family-shared care tasks.

### 2.2 Community & Expert Ecosystem
- **Community Q&A**: Anonymous posting, community feed, category tags, reporting mechanism, moderation workflow, expert answers with labels.
- **Expert Network**: Expert profile creation, credential verification, availability configuration, question queue, expert answers, consultation suggestion.
- **Consultation Management**: Booking, session management, consultation lifecycle, dispute resolution, refund policy, payment integration.

### 2.3 AI & Safety
- **Gemini AI Triage**: Structured intake, symptom normalization, risk classification (green/yellow/red), safe recommendation generation, red-flag detection, disclaimer enforcement, logging.
- **RAG Knowledge Base**: Verified content, checklists, pregnancy exercises, Gemini-powered RAG answers from verified sources.
- **Safety Monitoring**: IMU-based fall/impact detection, safety confirmation countdown, family alert, false-positive feedback, safety event history.

### 2.4 Emergency & Location
- **TrackAsia Emergency Map**: Emergency map display, nearby care search, quick-call buttons, route/ETA calculation, location sharing with consent.
- **Location Services**: Distance calculation, geocoding, location snapshots for emergency context.

### 2.5 Family & Privacy
- **Family Sync**: Family invitation, shared permissions, care calendar, shared task assignment, data sharing with granular consent.
- **Consent & Privacy**: Privacy settings management, data permission grants/revocations, sharing history, consent scope by data type/purpose/recipient/expiry.

### 2.6 Content & Partner Governance
- **Verified Content Management**: Article creation, versioning, RAG indexing, content quality labels, moderation state.
- **Partner Governance**: Partner clinic profile, service listing governance, sponsored content approval, performance metrics.

### 2.7 Audit, Security & Compliance
- **Audit Logging**: Immutable audit events for sensitive actions (RBAC changes, consent, health record access, expert verification, moderation, AI triage, emergency events, payment/refund, security incidents).
- **Security Incident Response**: Access review workflows, incident investigation, security event tracking.
- **RBAC**: Roles include user, mother, family member, verified expert, moderator, content admin, system admin, partner representative.

### 2.8 Payment & Business
- **Payment Processing**: VNPay integration, transaction recording, payment callbacks, refund management.
- **Commission Calculation**: Expert earnings, platform commission, settlement tracking.
- **Notification Services**: Firebase Cloud Messaging for reminders, community replies, consultation updates, emergency alerts.

### 2.9 External Integrations
- **Gemini AI**: For triage intake, RAG answers, symptom normalization.
- **Firebase**: Cloud Messaging (notifications), Realtime Database (chat/real-time), Storage (file uploads).
- **TrackAsia**: Maps, geocoding, nearby search, route calculation.
- **ZegoCloud**: Real-time communication for consultations.
- **VNPay**: Payment gateway.
- **MediaPipe**: Pregnancy exercise posture analysis.
- **Wearable/IMU**: Smartwatch data sync, phone IMU for safety monitoring.

## 3. Architecture & Design Decisions Recorded

- **Backend Architecture**: Layered Architecture using Controller-Service-Repository pattern, organized by business domain (not by layer globally). Each domain has `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `policy/`.
- **Frontend Architecture**: Vite + React + TypeScript, feature-based organization (`features/` folder), shared components and API clients in `shared/`.
- **Mobile Architecture**: Flutter, feature-based organization with `screens/`, `widgets/`, `services/`, `repositories/`, `models/`.
- **Data Architecture**: PostgreSQL as the source of truth for all structured business data (identity, RBAC, consent, care journey, health records, consultation lifecycle, payments, audit logs, content). Firebase Realtime Database for approved realtime state only. Object storage for uploaded files. PostgreSQL JSONB columns for flexible metadata where needed.
- **Integration Strategy**: Dedicated integration services under `backend/integration/` with fallback behavior for unavailable external systems.
- **Safety Strategy**: Centralized consent checks, audit logging, RBAC, triage policies; AI output is support-only with disclaimer; emergency-first routing enforced.

## 4. Gaps & Ambiguities

| Gap Area | Description | Impact |
|----------|-------------|--------|
| Report 2 completeness | The Project Management Plan appears to be present but may lack detailed WBS or risk mitigation details beyond what's in the SRS. | Implementation planning will rely primarily on SRS and architecture design. |
| API contract granularity | SRS describes functional specs but does not define exact API request/response schemas, status codes, or error formats. | Implementation will need to derive API contracts from use cases and acceptance criteria. |
| Database schema | ERD diagrams exist in Draw.io and PlantUML but were not parsed. The architecture doc describes data split strategy but not table definitions. | Entity design will need to be inferred from functional requirements. |
| UI/UX wireframes | No explicit wireframes or high-fidelity designs found in the scanned paths (UI_UX folder may exist but not checked in this pass). | Frontend implementation will rely on component-based architecture and feature requirements. |
| External integration credentials | The architecture references Firebase, Gemini, TrackAsia, ZegoCloud, VNPay, but no credential management or configuration details are documented in the scanned artifacts. | Implementation will need to define configuration properties and mock implementations for demo. |
| Test case specifications | PHASE-3_TDS.md and PHASE-4_Test-Spec.md were not found in the paths checked. Test strategy is described in architecture doc but detailed test cases are not available. | Test implementation will derive from story acceptance criteria. |

## 5. Contradictions & Resolutions

**Contradiction:** The architecture document shows a `security` domain and an `identity` domain as separate packages. AGENTS.md says "Keep current backend package structure." The current backend scaffold (seen in directory listing) shows only `security/` and `audit/` exist so far, with others likely to be added.  
**Resolution:** The architecture document is the target structure. We will document the target domain structure as planned. Implementation will follow the layered domain-based approach.

## 6. Vietnamese Product Terms & Feature Names

The following Vietnamese terms appear in the source documents and should be preserved where appropriate in user-facing content:

- **Mẹ bầu** (pregnant mother)
- **Thai kỳ** (pregnancy)
- **Mang thai** (pregnancy)
- **Nuôi dạy con** (childcare/parenting)
- **Bé của mẹ** (mother's baby)
- **Trợ lý** (assistant/helper)
- **Theo dõi** (tracking/monitoring)

Feature names in Vietnamese context:

- **Tư vấn viên** (consultant/expert)
- **Cộng đồng** (community)
- **Hỏi đáp** (Q&A)
- **Đồng ý chia sẻ** (consent to share)
- **Lịch tiêm chủng** (vaccination schedule)
- **Sổ khám chữa bệnh** (health record book)

## 7. Legal-Safe Healthcare Boundaries

The source documents repeatedly emphasize:

1. **Not a medical service**: CareBridge is a healthcare *support* platform, not a hospital information system, telemedicine provider, or emergency dispatch service.
2. **No diagnosis or treatment**: The system does not diagnose diseases, prescribe medication, provide treatment, replace direct medical examination, or replace emergency services.
3. **AI support only**: AI and community functions are for intake, summarization, risk classification, and safe next-action guidance only. Must include disclaimers.
4. **Emergency routing**: Emergency flows must be reachable from triage, safety alert, and dashboard; emergency-first actions must not be blocked by payment or paid consultation suggestions.
5. **Healthcare governance**: Red-flag routing is owned by TriageService and RedFlagPolicy, not delegated fully to Gemini; emergency flow must be accessible.
6. **Compliance**: References to Vietnamese regulations (Nghị định 13/2023/NĐ-CP and Luật Khám bệnh, chữa bệnh 2023) in sections on privacy and data protection.
7. **Consent & Audit**: Strict consent management and audit logging for health data access.

These boundaries must be preserved in all documentation and implementation.

## 8. Notes for Downstream Documents

- **PRD**: Should condense the extensive SRS feature list into prioritized product requirements, separating MVP scope from future scope. Must include safety, privacy, consent, moderation, audit, RBAC as first-class requirements.
- **Architecture**: Should confirm the layered modular monolith with domain packages. Should document persistence strategy (PostgreSQL as source of truth, Firebase Realtime Database for approved realtime state, object storage for uploaded files), integration services (Gemini, Firebase Cloud Messaging, Firebase Realtime Database, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, CompreFace, wearables, MediaPipe), and security/consent/audit cross-cutting concerns.
- **Coding Standards**: Should define Java/Spring Boot conventions (controllers, services, repositories, entities, DTOs, mappers, policies, exceptions, responses, validation, audit, transactions), React/Vite/TypeScript conventions (feature folders, API clients, forms, routing, state, UI components), Flutter conventions (feature folders, screens, services, repositories, models). Should be practical for a student capstone MVP.
- **Stories**: The foundation epic should cover: project documentation baseline (this work), backend shared domain scaffold (common utilities, security base, audit base), mobile app foundation (Flutter setup, routing, navigation, base screens), frontend web portal foundation (Vite setup, routing, layout, shared components), and auth/consent/privacy baseline (registration, OTP, login, JWT, consent models, privacy controls). Each story should be small, implementable in one pass, with clear acceptance criteria.

---

**Next Step:** Create `docs/bmad/prd.md` with product vision, actors, MVP scope, and prioritized requirements.
