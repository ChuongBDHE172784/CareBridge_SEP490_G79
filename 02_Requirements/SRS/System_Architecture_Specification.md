# CareBridge System Architecture Specification

| Field | Value |
| --- | --- |
| Project | CareBridge |
| Document type | System Architecture Specification |
| Architecture status | As-is development topology and target production topology |
| Version | 1.0 |
| Last updated | 2026-08-09 |
| Primary architecture diagram | `03_Design/Architecture/System Architecture-System Architecture.drawio.png` |

## 1. Introduction

### 1.1 Purpose

This document specifies the logical, runtime, integration, deployment, security, and algorithmic architecture of CareBridge. It translates the system architecture diagram into an implementation-oriented contract for developers, testers, operators, and future deployment agents.

The specification distinguishes between:

- the local development topology, which exercises the complete Cloudflare edge path;
- the production topology, in which the React portal is hosted by GitLab Pages and the API runtime is hosted on AWS EC2;
- mandatory core services and optional internal machine-learning sidecars; and
- implemented runtime behavior versus future or optional capabilities.

### 1.2 Scope

The architecture covers:

- CareBridge Mobile App and Web Portal clients;
- Cloudflare DNS, CDN, WAF, DDoS protection, analytics, and Tunnel routing;
- GitLab Pages static hosting;
- `cloudflared` and Nginx ingress components;
- the Spring Boot Backend API;
- the Python/FastAPI AI Triage Service;
- optional CompreFace and Exercise-Correction sidecars;
- PostgreSQL, pgvector capability, and object storage;
- external communication, mapping, media, AI, push-notification, chat, and video-call providers; and
- GitLab-based CI/CD and container delivery.

### 1.3 Architectural Principles

- Clients access application capabilities through HTTPS endpoints; internal containers are not public origins.
- Cloudflare is an edge and routing layer. It does not execute CareBridge application logic.
- Nginx is the only application reverse-proxy origin reached by the Cloudflare Tunnel.
- Spring Boot is the authority for authentication, authorization, consent, business rules, persistence, and audit-sensitive operations.
- AI and ML services provide bounded analysis results; they do not independently authorize users or directly approve medical, identity, or administrative decisions.
- Sensitive services and databases remain on private networks or provider-managed private endpoints.
- Environment overrides may change hostnames, image digests, capacity, and secret sources, but shall not silently redesign the canonical request paths.
- Optional integrations shall fail safely and shall not corrupt the primary transactional state when unavailable.

## 2. System Architecture Overview

### 2.1 Logical Layers

| Layer | Main components | Responsibility |
| --- | --- | --- |
| Client Layer | Flutter Mobile App, browser-hosted React SPA | User interaction, local device capabilities, API requests, sensor acquisition, and presentation |
| Static Delivery Layer | Cloudflare CDN, GitLab Pages | Delivery of versioned React HTML, CSS, JavaScript, and static assets |
| Edge Security Layer | Cloudflare DNS/WAF/CDN/DDoS/Bot protection/Analytics | HTTPS termination, traffic filtering, cache policy, public hostname routing, and edge observability |
| Tunnel and Proxy Layer | `cloudflared`, Nginx | Encrypted outbound tunnel, host/path validation, reverse proxying, and origin isolation |
| Application Layer | Spring Boot Backend API | Authentication, RBAC, consent, business workflows, persistence, integration orchestration, and audit |
| AI Orchestration Layer | Python/FastAPI AI Triage Service | Safety-first triage state machine, deterministic rules, bounded Gemini assistance, and evidence retrieval |
| Optional ML Sidecar Layer | Exercise-Correction, CompreFace | Private posture inference and supporting face-similarity evidence |
| Data Layer | Supabase PostgreSQL with pgvector capability | Relational persistence, controlled vector-backed capabilities, and transactional data management |
| External Service Layer | Gemini, Firebase, FCM, Gmail, Cloudinary, R2, ZegoCloud, TrackAsia | Specialized managed capabilities reached through secured provider APIs |
| Delivery Layer | GitLab Repository, GitLab CI/CD, Security Scanners, Container Registry | Source control, validation, image production, static publication, and deployment artifacts |

### 2.2 Canonical Request Paths

#### 2.2.1 Local Development

The local environment intentionally exercises Cloudflare rather than bypassing it.

```text
Browser
  -> https://portal.dev.carebridgevn.site
  -> Cloudflare
  -> cloudflared
  -> nginx-edge:8080
  -> Vite Web Development Server:5173

Web or Mobile Client
  -> https://api.dev.carebridgevn.site/api/v1/**
  -> Cloudflare
  -> cloudflared
  -> nginx-edge:8080
  -> Spring Boot:8080
```

No application container port is published directly to the host in the canonical local edge stack. The portal and API share the same Nginx origin, and Nginx separates the routes by the HTTP `Host` header.

#### 2.2.2 Production Web Portal

```text
Browser
  -> https://portal.carebridgevn.site
  -> Cloudflare CDN/WAF
  -> GitLab Pages
  -> React HTML/CSS/JavaScript
```

After the React application is loaded into the browser, it makes a separate API request path:

```text
React SPA in the user's browser
  -> https://api.carebridgevn.site/api/v1/**
  -> Cloudflare
  -> cloudflared on AWS EC2
  -> nginx-edge:8080
  -> Spring Boot:8080
```

The production build shall use:

```dotenv
VITE_API_URL=https://api.carebridgevn.site
```

GitLab Pages is not routed through the AWS Tunnel. Static portal delivery and API ingress are independent flows.

#### 2.2.3 Production Mobile API

```text
CareBridge Mobile App
  -> HTTPS 443 at api.carebridgevn.site
  -> Cloudflare
  -> cloudflared on AWS EC2
  -> Nginx
  -> Spring Boot:8080
```

The mobile application sends JSON REST requests to `/api/v1/**` using a JWT bearer token where authentication is required.

### 2.3 Runtime Ports and Trust Boundaries

| Component or service | Port/protocol | Exposure | Notes |
| --- | --- | --- | --- |
| Cloudflare public edge | HTTPS/WSS 443 | Public | Public portal and API hostnames |
| `cloudflared` connector | QUIC UDP 7844 or HTTP/2 TCP 7844 | Outbound only | Establishes the named encrypted tunnel |
| Nginx edge proxy | HTTP 8080 | Private Docker network | Origin reached by `cloudflared` |
| Spring Boot Backend | HTTP 8080 | Private Docker network | `/api/**` URI is preserved by Nginx |
| AI Triage Service | HTTP 8001 | Private AI network | Internal triage workflow endpoint |
| Exercise-Correction Sidecar | HTTP 8002 | Private `exercise-inference` network | Landmark-only inference |
| CompreFace frontend/API gateway | HTTP 80 internal | Private sidecar network | Optional host administration mapping at `localhost:8000` |
| PostgreSQL | TLS/TCP 5432 | Provider/private data connection | Accessed by Spring Boot, not directly by AI Triage |
| External APIs | HTTPS/WSS 443 | Controlled egress | Gemini, Firebase, ZegoCloud, TrackAsia, Cloudinary, and R2 HTTPS endpoints |
| Gmail SMTP | SMTP 587 with STARTTLS | Controlled egress | Transactional email and OTP delivery |

## 3. Component Specifications

### 3.1 CareBridge Mobile App

**Technology:** Flutter and Dart for Android and iOS.

**Responsibilities:**

- Provide Mother, Family Member, Expert, and other authorized mobile experiences.
- Call the Backend API through `api.carebridgevn.site`.
- Store only the minimum client-side state required for the active user experience.
- Use `sensors_plus` for phone accelerometer and gyroscope readings.
- Use the Gadgetbridge-based Android connector for supported BLE wearable pairing and synchronization.
- Receive FCM push notifications and participate in Firebase/ZegoCloud communication flows.

**Constraints:**

- The mobile client shall not receive backend-only provider credentials.
- The mobile client shall not call PostgreSQL, AI sidecars, CompreFace, or internal Backend endpoints directly.

### 3.2 CareBridge Web Portal

**Technology:** React, TypeScript, Vite, and Tailwind CSS.

**Responsibilities:**

- Provide browser-based administration, moderation, content, expert, and authorized user workflows.
- Load static HTML, CSS, and JavaScript from GitLab Pages in production.
- Call the API from the browser using the build-time `VITE_API_URL` value.

**Constraints:**

- A browser loading the portal is not a backend process. API responses return to the React application running on the user's device.
- CORS shall allow only approved portal origins.
- The production portal shall not be served by Nginx on AWS EC2.

### 3.3 Cloudflare Edge

**Responsibilities:**

- Manage DNS for `carebridgevn.site` hostnames.
- Terminate public HTTPS connections.
- Deliver/cache permitted static portal assets.
- Bypass caching for API traffic.
- Apply WAF, DDoS, and bot-protection controls.
- Provide edge traffic analytics.
- Route API traffic to the named Cloudflare Tunnel.

**Constraints:**

- Cloudflare contains no CareBridge application runtime or business logic.
- WAF and rate-limit rules shall be tested against authentication, uploads, WebSocket traffic, and real mobile clients before enforcement.

### 3.4 Cloudflared Tunnel Connector

**Responsibilities:**

- Establish an outbound encrypted connection from the deployment host to Cloudflare.
- Route approved public API hostnames to `http://nginx-edge:8080`.
- Avoid exposing inbound application ports on the host.

**Constraints:**

- The tunnel token shall be injected through a file-backed secret and shall not be stored in Compose YAML or source control.
- `cloudflared` shall not route directly to Spring Boot or any ML sidecar.

### 3.5 Nginx Reverse Proxy

**Responsibilities:**

- Validate the expected host and request path.
- Proxy API requests to `http://backend:8080` while preserving `/api/**` URIs.
- Proxy the local development portal to Vite when the local portal hostname is used.
- Reject unknown hostnames and invalid cross-origin routes.

### 3.6 CareBridge Backend API

**Technology:** Java and Spring Boot.

**Responsibilities:**

- Authenticate requests and validate JWTs.
- Enforce RBAC, ownership, consent, and business rules.
- Coordinate transactions and PostgreSQL persistence.
- Issue internal calls to AI/ML services.
- Integrate with external providers without exposing their credentials to clients.
- Record security-sensitive and business-sensitive audit events.
- Apply safe timeout, retry, idempotency, and fallback behavior where required.

### 3.7 CareBridge AI Triage Service

**Technology:** Python, FastAPI, LangGraph, and LangChain components.

**Responsibilities:**

- Execute the internal V2 triage turn workflow.
- Apply deterministic context, target, intent, stage, safety, question-planning, and outcome rules.
- Use Gemini only for bounded extraction or explanation tasks when enabled.
- Retrieve verified evidence after an outcome without allowing retrieval to change the outcome.

**Internal Interfaces:**

- Backend to AI: `POST /internal/triage/v2/turn` over HTTP 8001 using a shared internal key.
- AI to Backend: `GET /internal/api/v1/triage/evidence-sources/approved` over HTTP 8080 using a shared internal key.

**Authority Boundary:**

The reverse AI-to-Backend call returns approved evidence-source metadata only. It is not a general patient-data API and does not permit patient lookup. Authentication, consent, persistence, and owner scoping remain Spring Boot responsibilities.

### 3.8 Optional Internal ML Sidecars

#### 3.8.1 Exercise-Correction Sidecar

- Runs as a private Python/FastAPI container on port 8002.
- Accepts named MediaPipe landmarks through `POST /v1/inference/landmarks`.
- Does not accept or store image frames, JWTs, CareBridge identities, or database credentials.
- Is reached only by Spring Boot at `http://exercise-correction:8002`.
- Uses a rule-based Backend fallback when disabled or unavailable.

#### 3.8.2 CompreFace Face Verification

- Runs as an optional Docker profile.
- Is reached by Spring Boot through `http://compreface-fe:80` on the internal network.
- May expose `localhost:8000` only for controlled local administration.
- Produces face-detection and similarity evidence; it never approves an expert automatically.

### 3.9 Data Layer

**Primary Store:** Supabase-managed PostgreSQL.

**Responsibilities:**

- Store accounts, profiles, care records, permissions, consent, audit data, content, communication metadata, and workflow state.
- Support SQL transactions and Flyway-managed schema evolution.
- Provide pgvector capability for approved vector-backed features.

**Boundary:**

The Spring Boot Backend is the application owner of database access. The current Python AI Triage V2 runtime does not connect directly to PostgreSQL or pgvector.

### 3.10 DevSecOps and CI/CD

**Technology:** GitLab Repository, GitLab CI/CD, Semgrep, Trivy, GitLab Container Registry, and GitLab Pages.

**Pipeline Flow:**

1. A developer pushes a commit or merge request to GitLab.
2. GitLab CI validates the project and builds the applicable modules.
3. Semgrep performs static analysis and Trivy scans dependencies or container images.
4. The Web pipeline creates the Vite `dist` artifact with the production API URL and publishes it to GitLab Pages.
5. The Backend/AI pipeline builds immutable Docker images and pushes them to GitLab Container Registry.
6. The deployment workflow pulls immutable images to AWS EC2 and starts the approved Docker Compose production topology.

The frontend deployment arrow terminates at GitLab Pages. The backend deployment arrow terminates at the AWS/Docker deployment environment. These are separate delivery targets.

## 4. Other Design Specifications

### 4.1 System Integrations

#### 4.1.1 Cloudflare Edge, WAF, CDN, and Tunnel

**Description:** Cloudflare provides the public DNS, HTTPS, security, analytics, CDN, and named-tunnel edge for CareBridge.

**Purpose:**

- Protect the public portal and API from common web attacks, DDoS traffic, and abusive bots.
- Deliver static portal assets efficiently.
- Connect public API traffic to a private origin without exposing inbound application ports.

**Integration Method:**

- Browser and mobile clients connect to Cloudflare over HTTPS 443.
- Cloudflare serves the portal through GitLab Pages and sends API traffic through the named Tunnel.
- `cloudflared` establishes an outbound QUIC or HTTP/2 connection on port 7844 and forwards traffic to Nginx.
- API routes use cache bypass; static portal assets may use CDN caching.

#### 4.1.2 GitLab Pages

**Description:** GitLab-managed static web hosting for the production React portal.

**Purpose:**

- Host the compiled React HTML, CSS, JavaScript, and public assets.
- Decouple static frontend delivery from the AWS Backend runtime.

**Integration Method:**

- GitLab CI runs the Web build and publishes the generated `dist` directory.
- Cloudflare maps `portal.carebridgevn.site` to the GitLab Pages origin.
- The compiled SPA uses `VITE_API_URL=https://api.carebridgevn.site` for subsequent API calls.

#### 4.1.3 Supabase PostgreSQL and pgvector

**Description:** Provider-managed PostgreSQL database with optional vector-extension capability.

**Purpose:**

- Persist relational and transactional CareBridge data.
- Support controlled vector-backed functions where an approved feature requires them.

**Integration Method:**

- Spring Boot connects through PostgreSQL TLS on TCP 5432.
- JPA/Hibernate manages application persistence and Flyway manages schema changes.
- Database credentials remain Backend-only.

**Current Constraint:** AI Triage V2 uses a local verified BM25 evidence index and has no direct Python-to-pgvector runtime path.

#### 4.1.4 Gemini API

**Description:** Google-managed generative AI API used as an optional bounded assistant.

**Purpose:**

- Generate structured summaries and bounded text outputs.
- Assist with fact extraction, explanation, RAG response generation, or content-moderation tasks where enabled.

**Integration Method:**

- Spring Boot and the AI Triage Service use backend-managed HTTPS calls with separate configuration boundaries.
- API keys are injected through runtime secrets.
- Deterministic rules and safe fallbacks remain authoritative when Gemini is unavailable, invalid, disabled, or timed out.

**Safety Constraint:** Gemini shall not independently diagnose, prescribe, authorize access, approve experts, or override deterministic emergency rules.

#### 4.1.5 Firebase Realtime Services and Firestore

**Description:** Firebase-managed services for chat events, presence, session state, signaling, and token bridging.

**Purpose:**

- Deliver low-latency direct-chat and call-session updates.
- Maintain short-lived signaling or presence data required by communication features.

**Integration Method:**

- Authorized clients connect through Firebase SDKs over HTTPS/WSS 443.
- Spring Boot uses Firebase Admin credentials to mint or bridge authorized identities and publish controlled events.
- Retention jobs remove expired signaling data according to the configured retention window.

#### 4.1.6 Firebase Cloud Messaging

**Description:** Managed push-notification delivery service for mobile devices.

**Purpose:**

- Notify users about reminders, care tasks, invitations, messages, and safety events.

**Integration Method:**

- Spring Boot sends requests using the FCM HTTP v1/Admin SDK integration.
- FCM delivers notifications to registered mobile-device tokens.
- Notification delivery is recorded and designed as best-effort where a push failure must not roll back an already-committed business transaction.

#### 4.1.7 Gmail SMTP

**Description:** SMTP service used for transactional email communication.

**Purpose:**

- Send OTPs, verification messages, invitations, and other approved transactional emails.

**Integration Method:**

- Spring Boot connects to the configured SMTP host on port 587 using authentication and required STARTTLS.
- SMTP credentials remain server-side secrets.

#### 4.1.8 Cloudinary

**Description:** Cloud-based public media storage and delivery service.

**Purpose:**

- Store and deliver approved public or general application media.
- Provide managed media URLs and transformations where applicable.

**Integration Method:**

- Clients upload files to authenticated Backend endpoints.
- Spring Boot validates the file and calls Cloudinary using backend-only credentials.
- The Backend returns only the resulting approved asset reference to the client.

#### 4.1.9 Cloudflare R2

**Description:** S3-compatible object storage used for private CareBridge objects.

**Purpose:**

- Store protected expert identity files, health documents, attachments, or other private objects selected by storage policy.
- Separate sensitive-object storage from public media delivery.

**Integration Method:**

- Spring Boot communicates with the configured R2 endpoint through an S3-compatible client.
- Access key, secret key, bucket, and endpoint configuration remain Backend-only.
- Clients receive authorized application responses or short-lived access mechanisms rather than permanent provider credentials.

#### 4.1.10 ZegoCloud

**Description:** Managed real-time audio/video communication platform.

**Purpose:**

- Support authorized direct consultation rooms and video-call sessions.

**Integration Method:**

- Spring Boot validates conversation membership and issues time-limited room join credentials.
- Authorized clients connect to ZegoCloud over HTTPS/WSS 443 using those credentials.
- The Zego server secret is never exposed to Web or Mobile clients.

#### 4.1.11 TrackAsia

**Description:** Mapping, place-search, geocoding, nearby-facility, route, and ETA service.

**Purpose:**

- Search for permitted nearby care facilities.
- Resolve place and coordinate information.
- Provide route and estimated-travel information for emergency or care-support flows.

**Integration Method:**

- Spring Boot calls TrackAsia HTTPS APIs using a server-managed API key.
- The Backend normalizes provider results before returning them to clients.
- Location use remains subject to user permission, minimum-necessary disclosure, and feature-specific consent.

#### 4.1.12 CompreFace Face Verification API

**Description:** Optional self-hosted face-detection and face-similarity service.

**Purpose:**

- Compare an expert's submitted selfie with the portrait extracted from the submitted identity document.
- Provide supporting evidence for manual expert verification.

**Integration Method:**

- Spring Boot calls `http://compreface-fe:80` on a private Docker network.
- The local administrative UI may be mapped to `localhost:8000` during controlled setup.
- Detection probability, face count, and similarity thresholds are configured server-side.

**Decision Constraint:** A similarity score never proves document authenticity and never automatically approves an expert.

#### 4.1.13 Exercise-Correction API

**Description:** Optional internal FastAPI and MediaPipe-based posture-inference sidecar.

**Purpose:**

- Evaluate named exercise landmarks against versioned model artifacts.
- Return bounded posture-correction inference to the Backend.

**Integration Method:**

- Spring Boot sends landmark-only payloads to `POST /v1/inference/landmarks` at `http://exercise-correction:8002`.
- The service runs on the private `exercise-inference` Docker network and publishes no host port.
- Model files are verified against a manifest before readiness is reported.

**Safety Constraint:** The current models are concept-demo assets and are not clinically validated for pregnancy or the CareBridge population.

#### 4.1.14 BLE Wearable Integration

**Description:** Mobile-side integration with supported Bluetooth Low Energy wearable devices through a Gadgetbridge-based Android connector.

**Purpose:**

- Pair supported devices.
- Import user-authorized observations and synchronize selected wearable data.

**Integration Method:**

- The Android application communicates with the wearable over BLE.
- The mobile app sends approved observations to Spring Boot through the normal HTTPS API path.
- The wearable never connects directly to the Backend database or internal services.

### 4.2 Algorithms

#### 4.2.1 Safety-First AI Triage Workflow

**Problem Statement:** Free-text health concerns are incomplete and ambiguous, while an unsafe model-generated answer could delay urgent care or overstate clinical certainty.

**Solution:** CareBridge uses a deterministic, stateful triage workflow in which safety rules and validated structured signals are authoritative. Gemini is optional and bounded to extraction or explanation tasks.

**Process:**

1. Validate request shape, internal authentication, consent state, and session version.
2. Run the global safety gate before target, intent, or scope processing.
3. Resolve the primary target as Mother, Baby, Unknown, or Conflicted.
4. Validate intent, care stage, structured fields, and entity-stage consistency.
5. Apply stage-specific safety rules and canonical rule contracts.
6. Ask only permitted clarification questions, with bounded questions and rounds.
7. Produce a controlled outcome and action-oriented response.
8. Retrieve evidence after the outcome; retrieved material cannot change the disposition.
9. Persist state through Spring Boot with audit, owner scope, redaction, and retention metadata.

**Fallback Behavior:**

- A trusted explicit global danger signal may produce a conservative Backend RED fallback.
- Missing or unavailable AI dependencies shall produce a controlled `NEEDS_MORE_INFORMATION` or fallback-only state, never an unsupported GREEN result.
- Gemini failure does not bypass deterministic safety logic.

**Outcome:**

- Consistent, explainable triage behavior.
- Conservative handling of incomplete or conflicting information.
- Clear separation between assistance and clinical diagnosis.

#### 4.2.2 Verified-Evidence Retrieval and RAG

**Problem Statement:** Generated health explanations require traceable sources, while open-web retrieval or unverified documents can introduce unsafe or irrelevant evidence.

**Solution:** AI Triage V2 uses a locally controlled evidence registry and deterministic BM25 ranking over approved, hash-verified documents.

**Process:**

1. The AI service requests approved evidence-source metadata from the Backend internal endpoint.
2. Candidate documents must satisfy source status, organization, URL, section/page, target, stage, language, mapping, and SHA-256 integrity requirements.
3. The query and eligible evidence are scored using deterministic BM25 relevance.
4. The highest-ranked eligible evidence is attached only after the triage outcome is fixed.
5. If no verified source qualifies, the response keeps the disposition but omits citations.

**Current Architecture Decision:**

- The AI Triage Python service does not directly query PostgreSQL or pgvector.
- pgvector remains a database capability for separately approved Backend features.
- AI Triage V2 does not browse the Internet at runtime.

**Outcome:**

- Reproducible evidence ranking.
- Reduced risk of fabricated or unapproved citations.
- No RAG-based override of safety disposition.

#### 4.2.3 Exercise Posture-Correction Inference

**Problem Statement:** Exercise guidance requires posture feedback without sending raw camera frames to a general Backend or retaining unnecessary biometric media.

**Solution:** The client derives or supplies named MediaPipe landmarks, and Spring Boot sends a bounded landmark-only request to a private inference sidecar.

**Process:**

1. Validate the exercise identifier and landmark payload.
2. Reject unsupported exercises, malformed landmarks, or arbitrary model paths.
3. Load only manifest-verified model and scaler artifacts.
4. Perform the selected posture inference.
5. Return a bounded result to Spring Boot.
6. Use a rule-based fallback when the sidecar is disabled or unavailable.

**Outcome:**

- Reduced transfer and retention of raw visual data.
- Isolated and independently deployable ML inference.
- Predictable fallback behavior.

#### 4.2.4 Face Detection and Similarity Evaluation

**Problem Statement:** Expert verification requires evidence that a submitted selfie resembles the portrait on the submitted identity document, while automated face matching alone is insufficient for approval.

**Solution:** CompreFace provides detection and similarity measurements, which are treated as supporting evidence for a human administrator.

**Process:**

1. The Backend validates the authenticated verification workflow and submitted media references.
2. CompreFace detects faces and applies configured detection-probability and face-count rules.
3. CompreFace computes a similarity score between the selfie and document portrait.
4. The Backend stores the bounded verification result and audit context.
5. An authorized administrator makes the final approval or rejection decision.

**Outcome:**

- Consistent supporting similarity evidence.
- Explicit human-in-the-loop identity governance.
- Safe handling of timeout, no-face, and multiple-face conditions.

#### 4.2.5 Smart Fall and Impact Detection

**Problem Statement:** A phone sensor must distinguish a suspected fall or impact from ordinary movement while allowing the user to cancel false alarms.

**Solution:** CareBridge combines an on-device three-stage IMU detector with a Backend sensitivity filter and a user-facing safety countdown.

**On-Device Process:**

1. Detect a free-fall phase when acceleration magnitude drops below the configured threshold.
2. Detect a high-impact phase within the permitted time window using acceleration and jerk.
3. Confirm a post-impact immobility period using acceleration and gyroscope stability.
4. Cancel the candidate when significant movement indicates that the user is active.

**Backend Process:**

1. Normalize the acceleration magnitude relative to gravity.
2. Compare it with the configured HIGH, MEDIUM, or LOW sensitivity threshold.
3. Classify the event as suspected impact or suspected fall.
4. Start the safety-check workflow and record the user's `I am OK`, `Need Help`, timeout, or escalation response.

**Outcome:**

- Layered screening of suspected safety events.
- User confirmation before escalation where the workflow permits.
- False-positive feedback for later threshold evaluation.

## 5. Security Architecture

### 5.1 Edge Security

- All public traffic uses HTTPS 443.
- Cloudflare provides HTTPS termination, WAF, DDoS protection, bot controls, and analytics.
- API caching is bypassed.
- The deployment host accepts no direct public application port in the canonical topology.
- Unknown tunnel hostnames and invalid origin routes are rejected.

### 5.2 Application Security

- Spring Boot validates JWT bearer tokens and token key identifiers.
- RBAC, ownership, consent, and resource scope are enforced before protected operations.
- Sensitive actions generate appropriate audit evidence.
- File uploads are size/type validated before provider storage.
- CORS uses an explicit allowlist containing only approved portal origins.

### 5.3 Service-to-Service Security

- Backend and AI Triage authenticate internal calls using a shared internal key.
- Internal services are reachable only on the minimum necessary Docker networks.
- The AI evidence-registry endpoint returns approved source metadata only.
- ML sidecars do not receive general database credentials or client JWTs.

### 5.4 Secret Management

- Tunnel tokens, API keys, database credentials, JWT signing keys, SMTP credentials, provider secrets, and service-account material shall not be committed to Git.
- Local secrets use ignored files or environment files with restricted permissions.
- Production secrets shall come from an approved secret source or file-backed deployment secret.
- Secrets shall not appear in container images, logs, screenshots, client bundles, or error responses.

### 5.5 Data Privacy

- Clients and providers receive only the minimum data required for the selected operation.
- Health, identity, location, and communication data remain subject to consent and retention rules.
- AI logs and telemetry shall exclude raw health text, identifiers, prompts, and secrets.
- Face similarity is supporting evidence, not an autonomous identity decision.
- Exercise sidecars receive landmarks rather than raw frames.

## 6. Deployment Architecture

### 6.1 Local Environment

The root Compose stack provides Backend, Vite Web, Nginx, and `cloudflared`. Optional overlays add Exercise-Correction or CompreFace when explicitly enabled. Private networks isolate portal origin, API origin, tunnel traffic, and optional inference traffic.

### 6.2 Production Environment

The target Backend environment is AWS EC2 with Docker Compose. The base production stack contains:

- AI Triage Service;
- Spring Boot Backend;
- Nginx edge proxy; and
- `cloudflared` connector.

The Web Portal is published separately through GitLab Pages. Optional ML sidecars require explicit production enablement, capacity planning, private-network attachment, health checks, and secret configuration.

### 6.3 Deployment Invariants

- Production images shall be immutable and preferably pinned by digest.
- Services shall expose container ports only to approved private networks.
- Health checks shall gate dependent-service startup.
- The portal CORS origin and API hostname shall be exact production values.
- Production shall preserve `Client -> Cloudflare -> cloudflared -> Nginx -> Spring Boot` for API traffic.
- Deployment overrides shall not redirect the portal through the Backend host.

## 7. Reliability and Failure Handling

- Container health checks cover Spring Boot readiness, AI health, Nginx origin health, and optional sidecar readiness.
- External provider calls use bounded connection/read timeouts.
- Optional AI, notification, and ML failures use feature-specific fallback or best-effort behavior.
- A notification-provider failure shall not roll back a successfully committed primary business transaction unless the use case explicitly requires atomic delivery.
- Triage dependency failure shall fail conservatively and shall not produce an unsupported low-risk result.
- Model artifacts shall be integrity-checked before the Exercise-Correction service becomes ready.
- Retryable operations shall use idempotency or deduplication controls where duplicate side effects are possible.

## 8. Observability

- Cloudflare provides edge analytics and security events.
- Application services provide structured logs, health endpoints, and bounded technical metrics.
- Logs shall use correlation or request identifiers where supported.
- Metrics and logs shall not include secrets, full tokens, raw health text, private files, or unnecessary identity information.
- AI observability should include latency, fallback, schema, ruleset/hash, outcome, citation, and state-conflict counters using closed-enum labels.

## 9. Architecture Constraints and Known Limitations

- AI Triage is informational and is not a diagnosis, prescription service, medical device, or replacement for professional care.
- AI Triage V2 public release remains controlled by feature flags and readiness gates.
- Verified evidence coverage may be incomplete; absence of a citation shall not be represented as proof of safety.
- Exercise-Correction models are concept-demo assets and are not clinically validated for pregnant users.
- CompreFace results require manual administrative review.
- Optional ML sidecars are not part of the base production Compose contract until explicitly approved and provisioned.
- The diagram's PostgreSQL/pgvector component represents data-layer capability; it does not authorize a direct AI Triage-to-database connection.
- GitLab Pages hosts static frontend files only; it is not an API runtime and does not connect directly to Spring Boot.

## 10. Architecture Traceability

| Architectural decision | Primary implementation/source |
| --- | --- |
| Local and production Cloudflare request paths | `05_Development/Deployment/EDGE_TOPOLOGY.md` |
| Local edge stack | `docker-compose.yml` |
| Production API stack | `05_Development/Deployment/docker-compose.production.yml` |
| Nginx routing | `05_Development/Deployment/nginx/` |
| Backend integrations and ports | `05_Development/CareBridgeAPI/src/main/resources/application.yaml` |
| AI Triage boundaries | `05_Development/CareBridgeAITriageService/` and AI Triage architecture documentation |
| Exercise-Correction sidecar | `05_Development/Deployment/docker-compose.exercise-ml.yml` |
| CompreFace optional profile | `05_Development/Deployment/docker-compose.compreFace.yml` |
| Client applications | `05_Development/CareBridgeMobileApp/` and `05_Development/CareBridgeWebApp/` |
| Visual system topology | `03_Design/Architecture/System Architecture-System Architecture.drawio.png` |

