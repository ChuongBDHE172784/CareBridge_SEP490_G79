# CareBridge — Claude Code Context

## Commands

* Backend (`05_Development/CareBridgeAPI`):
  * Run: `set -a && source .env && set +a && ./mvnw spring-boot:run`
  * Build/Package: `./mvnw clean package`
  * Test: `./mvnw test`
  * Requires `.env` with `CAREBRIDGE_DB_*`, `JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEYS`, etc. (see `.env.example`); datasource defaults to local Docker PostgreSQL.
  * `JWT_PRIVATE_KEY` is base64 DER PKCS#8; `JWT_PUBLIC_KEYS` is a semicolon-separated `kid:base64-DER-SPKI` verification ring. Never commit real key material.
  * Values containing `&` or spaces must be quoted in `.env` for bash `source` compatibility.
* Web (`05_Development/CareBridgeWebApp`):
  * Run: `npm run dev`
  * Build: `npm run build`
* Mobile (`05_Development/CareBridgeMobileApp`):
  * Run on Chrome: `flutter run -d chrome`
  * Run on emulator: `cd 05_Development/CareBridgeMobileApp && flutter run` (defaults to `10.0.2.2:8080`)
  * Run on physical device: `flutter run -d <device-id> --dart-define=API_BASE_URL=http://<LAN_IP>:8080`
  * Build: `flutter build apk`
  * Test: `flutter test`
  * API base URL is configured via `--dart-define=API_BASE_URL`. When omitted, defaults: web → `localhost:8080`, Android emulator → `10.0.2.2:8080`.
  * If Android build fails with Kotlin incremental cache errors on Windows, add `kotlin.incremental=false` to `android/gradle.properties` and kill Gradle/Kotlin daemons before retrying.

## Stack

* Backend: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway.
* Web: React + TypeScript + Vite.
* Mobile: Flutter/Dart.
* Infrastructure: Docker Compose, GitLab CI/CD.
* Integrations: Firebase, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, Gemini.

## Firebase Agent Skills

Always look for and use the appropriate **Firebase agent skills** to perform tasks related to Firebase.

## Architecture

Use a modular monolith. Do not introduce microservices, MongoDB, new infrastructure, or dependencies without approval.

Backend package style: Package by domain, then layers inside:
`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `policy`.

Rules:
* Controller: validation, request/response mapping only; no business logic.
* Service: workflows, transactions, authorization checks.
* Repository: persistence/query only; no business decisions.
* Never expose JPA entities in API responses; use DTOs and mappers.
* Policy: reusable domain rules (healthcare safety, consent, payment eligibility, moderation).

## Read On Demand

Read only the files relevant to the task:
* Backend: `05_Development/CareBridgeAPI/`
* Web: `05_Development/CareBridgeWebApp/`
* Mobile: `05_Development/CareBridgeMobileApp/`
* Architecture: `03_Design/Architecture/`
* Database/contracts: `05_Development/Database/`, `05_Development/Contracts/`

Current code and migrations override historical design notes.

## Delivery Rules

Make the smallest scoped change. Do not refactor unrelated code, delete existing code, or upgrade dependencies unless requested.

For health, location, payment, expert, moderation, and safety workflows: enforce existing RBAC, consent scope/expiry, and audit requirements. AI provides guidance only; never diagnose, prescribe, or delay emergency routing.

Use Flyway for schema changes. Never modify an applied migration.

Inspect relevant code/tests first. Add focused tests where practical, run the narrowest relevant command, then report changed files, verification, and remaining risks.

## Code Search & Analysis Priority Matrix

**Rule: NEVER use grep/glob/read first. Always use Graph MCP tools to minimize tokens and map blast radius.**

1. **Step 1: Scoping & Blast Radius (code-review-graph)**
   * Use `detect_changes` and `get_impact_radius` to identify modified files and their affected areas.
   * Use `get_review_context` to fetch only the exact code changes and relevant context.
2. **Step 2: Architecture & Relations (gitnexus & code-review-graph)**
   * For call chains, dependency paths, or tracking tests: Use `query_graph` or `query` (cypher query).
   * For visual/conceptual routing & API maps: Use `route_map` or `get_architecture_overview`.
   * For structural multi-file renaming: Use `rename`.
3. **Step 3: Verification (Native Commands)**
   * Only read/edit the files discovered in Step 1 & 2.
   * Use native text search/read as a fallback only when seeking precise strings inside the narrowed file set.

## MCP Tools: code-review-graph

Use the project knowledge graph before filesystem scanning when exploring code, reviewing changes, mapping impact, locating relationships, or answering architecture questions. The preferred workflow is:

1. `detect_changes`
2. `get_affected_flows`
3. `query_graph` with `tests_for` where coverage must be checked
4. Focused native verification commands

## Synthetic Development Accounts

`DevDataSeeder` creates synthetic role accounts only when the Spring `dev` profile is active, the `prod` profile is absent, and `CAREBRIDGE_DEV_SEED_ENABLED=true` is explicitly set. The operator must inject a unique, non-default password through `CAREBRIDGE_DEV_SEED_PASSWORD` from an uncommitted local secret source.

Never place that password in source, documentation, shell history, logs, screenshots, or Git. A blank value or the retired historical default must fail startup while seeding is enabled. Never enable dev seeding on staging or production.
