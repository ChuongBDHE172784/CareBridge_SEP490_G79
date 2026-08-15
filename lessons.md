# Lessons learned

## 2026-08-14 - Checklist item source-link authoring

- Treat an optional per-item source link as a round-trip contract: create, edit,
  clear, request/response mapping, version cloning, and JSON metadata persistence
  must change together even when the visible request is only for a form field.
- Keep URL policy identical at every boundary. The Web form, Bean Validation,
  and service guard now require HTTP(S), a hostname, no embedded credentials,
  and at most 2,048 characters; checking only `URL.protocol` leaves
  `https://user:password@host` incorrectly accepted by the UI.
- Adding source-only JSON metadata activates code paths that recurrence-only
  tests may never reach. Inject the real `ObjectMapper` in service tests and
  prove both update and clear behavior so an early `{}` return cannot silently
  discard the new field.

## 2026-08-14 - Postpartum and Baby Care stage split planning

- When product UX chooses to separate maternal postpartum and baby care, implement the separation as one cross-layer contract: stage, care context, anchor, subject, occurrence identity, authoring choice, filters, and presentation must change together.
- A safe stage split needs compatibility backend support before client rollout, disabled deterministic catalog replacements before activation, cohort remediation, and a later legacy-alias retirement release. Renaming the UI first or mutating published template versions creates unsupported-client and history risks.
- Baby Care checklist stages are not automatically the same enum domain as infant/toddler triage stages. Inventory every stage-bearing contract and change only the bounded checklist/content surfaces unless a separate requirement authorizes broader lifecycle changes.
- Do not infer Baby ownership from a template title. Classify legacy roots from stored context, anchor, leaf semantics, and provenance; quarantine ambiguity and publish nothing automatically.
- Calibrate change-management rigor to the delivery context. For a student project with one deployed environment, retain correctness-critical migration safety and regression evidence, but omit enterprise cohort rollout, long-lived compatibility programs, and observability infrastructure unless explicitly required.

## 2026-08-14 - POSTPARTUM checklist empty-state investigation

- The Flutter Today panel's normal empty state is evidence of a successfully parsed empty snapshot, not proof of a client filter or network failure; terminal/retryable failures render a separate state.
- Consolidating lifecycle stage names can destroy subject semantics: changing `BABY_CARE` roots to `POSTPARTUM` before a backfill derives leaf targets from `root.stage` can reclassify Baby content as Mother content.
- A repair that selects only mixed Mother/Baby roots does not cover formerly pure Baby roots that were already misclassified. Add a full historical-chain fixture from the pre-consolidation schema.
- Per-candidate exception isolation keeps Today available but can turn materialization defects into HTTP 200 empty lists. Diagnose `checklist_materialization_candidate_failed` alongside the sanitized Today payload and catalog/context rows.
- Dirty-worktree regression tests prove the proposed local repair, not deployed-account causality. Keep source-mechanism confidence separate from live-account confidence.
- A full-chain migration probe is stronger than a source-only read: a pure legacy `BABY_CARE` root was restaged `POSTPARTUM`, its leaf became `MOTHER`, and both it and a native legacy `POSTPARTUM` root were quarantined as `PENDING_REVIEW`/disabled before candidate selection.
- A mixed-root repair cannot discover rows already made `PENDING_REVIEW`/disabled; verify lifecycle flags before judging a split migration ineffective.
- Keep migration-chain evidence separate from account attribution: the probe proves the mechanism, not that the deployed account ran the same Flyway path or has the same rows.
- Migration gates can drift independently of runtime behavior: the full-chain test failed only because its expected latest version lagged the newly added split migration, while 30 service/materialization/Today tests passed.
- Before planning a deployed Supabase query, verify three separate prerequisites: CLI availability, API authentication, and project/DB selection. An `npx`-available CLI does not imply a logged-in Management API session.
- A deployed account can expose a stronger seam than a synthetic catalog probe: compare `created_at`, `historical_at`, `history_reason_code`, `window_start/end`, and `period_key` on the same instance. Here the 2.8-second transition plus `LIFECYCLE_STAGE_OBSOLETE` proved period-identity drift.
- For POSTPARTUM/BABY cadence, deriving `period_key` from pregnancy LMP/EDD while eligibility uses delivery/birth anchors creates self-invalidating instances. The cadence identity must use the same lifecycle anchor as current-scope reconciliation.

## 2026-08-13 - Preconception sequence advance diagnosis

- The sequence readiness path and the sequence advance materialization path build commands independently; proving Today reaches `READY_TO_ADVANCE` does not prove the successor can be materialized.
- `ChecklistSequenceAdvanceService.distributionCommand` must carry the successor root contract just like reconciliation does. A compatibility constructor silently converts V2 targetless content into the legacy materialization path.
- An HTTP 500 from sequence advance is not a normal readiness/configuration response: those branches are typed 400/404/409. Capture the backend stack trace before treating a static contract defect as the confirmed runtime exception.
- The production-path PostgreSQL regression must complete set 1, invoke the real advance service, and assert successor parent/task contract fields; testing only the resolver projection misses this transition seam.

## 2026-08-13 - Preconception checklist sequence requiredness drift

- The `1/4` pill in the Mother Today panel is sequence position, not item progress:
  `currentPosition/totalPositions`. Diagnose CTA visibility from `sequenceState` and
  `advanceAvailable`, not from that pill.
- Sequence qualification is based on materialized task snapshots, not the current
  template catalog. A live instance can be parent `COMPLETED` with every visible task
  completed yet remain `ACTIVE` when all snapshot `is_required` flags are false;
  `requiredCount > 0` then fails by construction.
- A migration that repairs only `IS NULL` requiredness does not fix rows that were
  prematurely persisted as the non-null default `false`. Audit catalog item and task
  snapshot side by side through `template_item_version_id` before trusting either.
- Unit tests can prove each isolated layer while missing deployed-artifact/data drift:
  source mapping, distribution persistence, resolver qualification, and Flutter CTA
  fixtures all passed with explicit correct booleans. Add a live-schema/E2E gate that
  materializes approved mandatory V2 content and asserts requiredness survives into the
  Today `READY_TO_ADVANCE` projection.

## 2026-08-13 - Checklist maternal health-metric support destination

- Keep `HEALTH_RECORDS` mapped to the health-record timeline; the maternal
  `Chỉ số sức khỏe` destination is a separate journey-scoped route and needs
  its own support-function contract code.
- A checklist Today task already carries `careContextType=JOURNEY` and
  `careContextId`, so mobile navigation can derive the trusted metric route
  without accepting a server-supplied URL or issuing another dashboard read.
- Adding a checklist support function is a four-layer contract change: Web
  TypeScript catalog, Java enum, both PostgreSQL whitelist constraints, and
  Flutter's client-owned destination catalog. Verify propagation end to end.
- Support function participates in checklist materialization identity checks;
  do not rewrite already-materialized task snapshots when introducing a new
  destination code.

## 2026-08-13 - Today task cadence indicator

- Cadence must be normalized at the Today API boundary from checklist root
  schedule metadata or reminder recurrence; the mobile card should not infer
  repetition from `dueAt`.
- Preserve a compatibility constructor/default while adding the cadence field,
  otherwise older Today providers and fixtures break at compile time.
- Flutter tooling on a read-only SDK location needs the real pub cache plus
  elevated access for the SDK lockfile; Dart format/analyze can still run with
  workspace-local analytics/cache paths.

## 2026-08-13 — Maternal BMI trend catalog drift diagnosis

- A successful Flyway history row does not prove reference data still exists. The live database recorded the BMI-definition migration as successful while `health_metric_definitions` was completely empty; verify both migration history and current catalog rows.
- Maternal Journey weight and height are projections of the latest BMI observation context (`weightKg`/`heightCm`), not separate metrics. One BMI trend failure therefore blanks both cards even when BMI observations remain intact.
- The BMI write path and read path enforce different contracts: recommendation synchronization can persist a BMI observation with a hard-coded definition version, while trend reads first require an active metric definition. A catalog/data integrity gate should cover both sides.
- A broad catch that logs only a generic recommendation warning cannot establish causality. Keep recommendation signal-resolution warnings separate from health-metric `METRIC-030` unless the exception cause or shared failing dependency is captured.

## 2026-08-12 — Pregnancy V2 staging activation evidence

- A signed-off Pregnancy V2 fixture must stay outside Flyway production locations; use a disposable embedded database and an explicit test-resource SQL patch so production WHO roots remain `DRAFT`/non-distributable.
- Keep database-gate evidence separate from application-service evidence: a direct SQL fixture proves constraint acceptance/rejection, while the approval service requires its own unit test.
- A current-chain preflight is not occurrence/history E2E or clinical sign-off. Label the evidence boundary explicitly in the runbook and never promote staging fixture activation into production approval.

## 2026-08-12 — Firebase phone/email authentication hardening

- Firebase Phone Auth returns a short-lived ID token. If the backend exchange
  fails after SMS confirmation, retry with `User.getIdToken(true)` instead of
  replaying the cached proof; keep the confirmation session only until the SMS
  code succeeds.
- A role selected after a roleless login changes the server-side JWT authority,
  not just the profile response. Refresh the session before routing into a
  role-protected portal, and reconcile a lost role-selection response by
  re-reading the profile before retrying.
- OTP lookups use canonical E.164/lowercase subjects, so migrations must update
  legacy `auth_challenges.subject_identifier` rows as well as `users.phone`;
  otherwise resend/verify can disagree after deployment.
- Channel-bound resend tests must populate the same email/phone field as the
  pending challenge. A mock entity with no channel hides the production
  compatibility state populated by `@PostLoad` and produces misleading failures.

## 2026-08-12 — Targetless V2 parent metadata

- The P2 `checklist_instance_period_shape_ck` constraint applies to every V2
  parent, including unscheduled user-created tasks. Use a stable sentinel
  occurrence shape (`USER_CREATED`, `UTC`, `INTERACTIVE`, `was_actionable=true`)
  instead of weakening the database contract or adding a table.
- On Windows, a Maven wrapper cell can time out after the forked Surefire JVM
  has already completed; inspect the Surefire report before treating the run as
  failed.
- Family permission changes currently lack a runtime access-timeline/audit
  contract. Do not increment `checklist_access_epoch` ad hoc: the deferred P2
  trigger requires a paired timeline and access audit event, so this is a
  separate migration design item.

## 2026-08-11 - Checklist P1/P2 legacy evidence closure

- PostgreSQL 18 does not expose `jsonb_object_length(jsonb)`; exact JSON object
  shape checks can use `jsonb - ARRAY[allowed_keys] = '{}'::jsonb` together with
  `?&` instead of relying on a missing helper.
- Polymorphic immutable audit rows cannot be quarantined by resource marker. A
  legacy checklist access event with an unknown member id must fail the P2
  upgrade before any backfill write; malformed quarantine evidence likewise
  fails at final `VALIDATE`.
- A Flyway login kept separate from the NOLOGIN schema-owner role can still be
  authorized explicitly by its existing DDL privilege; runtime roles remain
  denied even if a broad schema grant accidentally gives them `CREATE`.
- Windows embedded PostgreSQL may require an unsandboxed test process for the
  restricted-token startup path; sandbox failures (`error code 87`) are not
  migration evidence.

## 2026-08-11 - Checklist P1/P2 migration seam review

- Deferred PostgreSQL constraint triggers validate final transaction state; malformed legacy shapes must be marked before the flush, with read-only preflight and trigger predicates kept in lockstep.
- Migration helper functions need an explicit execution-role model; revoking PUBLIC and granting a NOLOGIN owner role is insufficient when the deployment runner is separate.
- Family parent rows must be stamped only after the member epoch is durable, otherwise recipient authorization triggers reject the migration's own update.

## 2026-08-07 — MF-03 vaccination book & reminders

- `vaccination_records.care_subject_id` is NOT NULL but `VaccinationRecord` never mapped it, so
  **every** JPA insert on that table (UC229 add, UC232 complete, UC233 postpone) would have failed
  on a real database. It went unnoticed because all vaccination tests mock the repository. A unit
  suite that mocks persistence cannot see a not-null violation — a schema-touching feature needs at
  least one test that actually writes the row.
- `vaccination_schedules` held seven rows that each carried their *own* `schedule_version`
  (`legacy-b6f69eb8`, `legacy-ace5d2d9`, …). That is not a catalogue, it is seven catalogues of one
  row each, so "scan the catalogue" had no well-defined answer. A single active version selected by
  configuration turns it back into a set operation, and leaves the legacy rows readable for records
  that still point at them via `vaccination_schedule_id`.
- `notification_records_type_check` admits exactly seven type values, so a new notification category
  belongs in `reference_type`, not in `NotificationType`. Vaccination reminders ride `REMINDER` +
  `reference_type = 'VACCINATION'`, matching how APPOINTMENT and REMINDER_SCHEDULE already work — no
  migration, no constraint churn.
- Idempotency for a recurring notification needs a *milestone* key, not a record key. Keying only on
  the vaccination record would send one reminder ever; keying on `(record, daysBefore)` in the JSONB
  metadata gives each lead its own once-only delivery and makes extra job runs free.
- Count your own seed rows before asserting on them. The catalogue migration has 30 doses; the
  contract test asserted 29 and failed on the first real run. Two other tests in the same class
  passed and were the actual evidence — read *which* assertions failed before assuming the feature
  is broken.
- Baselining paid off again. A sweep of notification/reminder/audit/baby showed 9 failures + 19
  errors, all in `ReminderSecurityTest`, `TodayTaskControllerTest`, `UpdateReminderServiceTest`,
  `ReminderWorkerPropertyBindingTest` and two Postgres migration tests. `git stash push -u -- <path>`
  and re-running the same six classes at HEAD produced byte-identical counts, proving none were mine.
  Path-scoped stash works here even though `git worktree add` does not.
- Adding a constructor dependency to a service breaks `@InjectMocks` tests silently at compile time
  and loudly at runtime: three `BabyService*Test` classes NPE'd on a null `IVaccinationBookService`
  because Mockito injects null for an undeclared mock. Grep for `@InjectMocks <Service>` whenever a
  collaborator is added.

## 2026-08-07 — Auth taste-skill redesign

- Shared Flutter auth primitives make Welcome, Login, and Register visually coherent while keeping screen state and auth payloads local to each flow.
- Preserve test-facing keys on the concrete leaf widget when existing widget tests cast them (`password-login-submit` must remain on `FilledButton`, not only its wrapper).
- Responsive auth layouts need a stacked fallback for brand/header rows at large text scales; landscape accessibility tests exposed the overflow that normal-size screenshots would miss.
- When Flutter tooling cannot access the user Pub cache, run `pub get` with a workspace-local cache and verify with targeted widget tests rather than treating a silent CLI timeout as a test result.

## 2026-08-07 - Taste skill analysis

- The installed taste skills are a heterogeneous toolkit, not one compatible design system: separate brand, web, mobile-image, redesign, and output-process skills by task.
- For CareBridge, use the routed `taste-skill` family as the UI/UX baseline, then add only the surface-specific skill needed; do not combine `gpt-taste`, `high-end-visual-design`, `minimalist-ui`, and `industrial-brutalist-ui` as simultaneous global rules.
- Image-generation skills create references and do not implement Flutter or React; pair one image workflow with a separate implementation and verification pass.
- Marketing-oriented taste skills are out of scope for core dashboards, data tables, and multi-step health workflows unless their rules are deliberately adapted for accessibility, reduced motion, touch targets, and semantic states.

## 2026-08-05 — Database consolidation review

- A column named like generic settings can hold security and operational state; verify every JSON key and native SQL reader before proposing a drop.
- Treat legacy-looking columns and views as contract objects until both JPA mappings and native queries have been removed.
- Before dropping an apparently unused table, inspect inbound foreign keys, dependent indexes, views, triggers, and seed data on the live target.
- Product-declared 1:1 cardinality is not evidence of database-enforced 1:1; preflight nullable and duplicate children before consolidation.
- Polymorphic job tables should retain typed foreign keys, discriminator checks, and per-type unique identities instead of relying only on `source_type/source_id` plus JSONB.
- Do not replace a trigger-populated table with a view until its retention semantics are checked; `reminder_occurrence_aliases` intentionally preserves historical generations that current-state views cannot reconstruct.
- PostgreSQL `CHECK` constraints cannot contain subqueries directly; array/JSON collection validation needs an immutable helper function or a constraint trigger.
- Release waves are not database transactions. Define transaction and rollback boundaries per Flyway migration/object, with separate expand, backfill, cutover, observation, and contract releases.
- Removing workflow code requires reconciling pending records before endpoint/service removal, not merely before the eventual table drop.
- Observation readiness and contract readiness are different gates: inactive source artifacts may remain for rollback during observation, but mappings and queries must be removed before database contract.
- Static zero-reference scans need explicit allowlists for intentionally retained historical constants and migration tests; otherwise valid audit compatibility looks like cleanup failure.
- Breaking feature retirement should deploy client route/call cleanup before backend endpoint removal, while persistence-only consolidation should preserve public DTO contracts.

## 2026-08-06 — Database consolidation, feature-retirement half (R0a–R5c)

- Establish a pre-existing-failure baseline **before** touching anything. Six backend/web
  test failures in this session were already broken at `HEAD` (`AuthServiceLoginTest` stubs
  `findByEmailIgnoreCase` while production calls `findByEmail`; `Story69ContentSecurityTest`
  has a Mockito stub that no longer matches `AdminContentController.getChecklists`), and
  proving that with `git diff`/`git show` cost far less than assuming either way.
- `git worktree add` fails on this repo (`Filename too long` under `03_Design/UI_UX/`), so
  baseline comparison has to use `git show HEAD:<path>` and `git status --short <path>`
  rather than a second checkout.
- `rm -rf` is blocked by the permission layer here; `git rm -r` does the same job, is
  reviewable, and stages the deletion in one step.
- Removing a role from an enum breaks more than the enum: `@EnumSource(names=…)`, JDBC
  seed arrays and `@WithMockUser(roles="…")` fixtures all reference it as a string and
  fail at runtime, not compile time. Swap "unauthorised role" fixtures to a role that
  still exists rather than deleting the negative test.
- A test fixture's `@PreAuthorize` role and its `@GetMapping` path are separate facts —
  changing only the role turned an expected 200 into a 404 because the parameterised test
  derived the URL from the role name.
- When no Docker is available, a zonky embedded-Postgres base class that runs the whole
  Flyway chain with `ddl-auto: validate` is the strongest migration evidence obtainable:
  a mapping still pointing at a dropped column fails at context startup.
- Check the actual column names of the replacement table before writing gate SQL —
  `direct_conversation_read_cursors` keys on `reader_user_id`, not `user_id`, and the
  wrong name would have made the gate query error instead of gating.
- `npx tsc --noEmit -p tsconfig.app.json` does not cover `*.test.tsx`; a Vitest file kept
  referencing removed model fields and only the static scan caught it.
- Retiring a self-service workflow can silently remove the user's only recourse. Dropping
  the lock-appeal flow left no support contact anywhere in either client, so one had to be
  introduced as configuration — worth checking for whenever a "contact us instead" path is
  the replacement.

## 2026-08-06 — Database consolidation, persistence half (R6–R8)

- Do not bump a revision column during a representation-only backfill. Jobs snapshot the
  `config_revision` they were materialised from, so bumping it during the R7 backfill
  would have made every in-flight PENDING job look obsolete to `cancelObsoleteRevisions`
  and cancelled valid notifications. The spec's "same transaction" requirement is about
  atomicity between rules and revision, not a mandate to bump on migration.
- Hibernate builds its **own** ObjectMapper for `SqlTypes.JSON` columns and cannot be
  relied on to have the parameter-names module, so a Java `record` may fail to
  deserialize. Use a mutable bean with a no-arg constructor for JSON value objects.
- `LocalTime[]` with `@JdbcTypeCode(SqlTypes.ARRAY)` and `columnDefinition = "time[]"`
  passes `ddl-auto: validate` on PostgreSQL 18 — verified, not assumed.
- Two spurious `NoClassDefFoundError`s in this session had different causes and needed
  different fixes: one was a genuine package/path mismatch (file under `schedule/`
  declaring `package …schedule.service`), the other was purely a stale incremental build
  artifact cleared by `mvnw clean test-compile`. Check which before "fixing" either.
- Adding a column with a NOT NULL default to a table whose CHECK spans several columns
  means the CHECK must be added **after** the backfill, and the backfill guard must match
  the default-shaped row so a rerun cannot clobber values the new code already wrote.
- `AbstractPostgresIntegrationTest` uses a raw `PostgreSQLContainer` with no
  `disabledWithoutDocker` guard, so every test extending it errors rather than skipping on
  a Docker-less machine. `AbstractEmbeddedPostgresIntegrationTest` (zonky) is the one that
  works locally — prefer it for new migration contract tests.
- A "source queue is frozen" claim based on **row counts** is not evidence that nothing
  writes to it. During R13 the counts sat at 221/10 for a day while a second backend
  instance — still on a pre-R12 build — was quietly working a job in
  `reminder_schedule_jobs`. Only the `updated_at > cutover` gate caught it, seven seconds
  before this build worked the same `job_id` in `notification_jobs`. Check timestamps, not
  cardinality, and remember a production database may have more than one writer attached.
- Before blaming the running build for an unexpected write, search the **built artifact**,
  not just `src`. Unzipping the fat jar and grepping `BOOT-INF/classes` for the table name
  (0 hits in 2070 classes; only migration SQL matched) is what proved the writer was an
  external process rather than a missed R12 reference.
- A gate keyed to "unchanged since <fixed timestamp>" is unsatisfiable forever once a
  stray write lands, because `updated_at` cannot be un-set. Prefer a rolling quiet window
  plus a parity check: the window detects a *running* writer, the parity check is what
  actually prevents data loss if the window is wrong. Say plainly which of the two is
  load-bearing — a quiet window cannot distinguish "stopped" from "idle because nothing
  is due".
- `AbstractPostgresIntegrationTest` never provisioned the NOLOGIN roles that the checklist
  migrations demand (`CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED`), so all 76 subclasses were
  failing at context load and nobody saw what was inside them. Once unblocked, one class
  turned out to have a fixture years out of date with the schema. A test that errors before
  its first assertion is not a passing test — check *why* a suite is red at the base class.

## 2026-08-07 — Mother Home mobile redesign

- When Flutter is installed under `Program Files`, the wrapper can hang on SDK lock/cache
  access. A workspace-local `APPDATA`/`PUB_CACHE` plus the existing Flutter snapshot is a
  reliable fallback for formatting, analyze, and targeted widget tests.
- Preserve interaction keys on the concrete tappable widget when replacing a
  `GestureDetector`/`Container` with `Material`/`InkWell`; the Mother Home tests continued
  to pass their layout and interaction assertions after the visual change.
- The Mother Home screen and router use canonical `/appointments/calendar`, while the
  existing widget fixture still declares `/reminders/calendar`; keep that compatibility
  mismatch explicit instead of changing navigation during a visual-only redesign.
- Before dropping a table, grep for **writers**, not just readers and mappings. Wave 13's
  cutover moved the service, the repository and three consumers but missed `DevDataSeeder`,
  which still INSERTed the source table. Check the seeder's activation condition too before
  calling it a production risk: `@Profile("dev & !prod")` meant it could never have run there,
  and claiming otherwise sent the user through an unnecessary deploy cycle.
- Never run two Maven builds against the same `target/` at once. Doing so produced
  `FileNotFoundException: class path resource [...]` across 17 unrelated test classes, which
  looked exactly like test-order flakiness and led to a wrong "the suite is unstable" claim.
  Measure stability with sequential runs and nothing else touching the build directory.
- A migration's gates can pass **vacuously**. The wave-13 backfill ran green on an empty
  `growth_measurements`, proving nothing about the transformation. Seed real source rows and
  replay the statement *read out of the migration file* so the test cannot drift from what
  production will execute — that is also what caught an `unnest.column_name` SQL error before
  it reached the live database.
- `text_value` on `health_observations` is the observation's own textual content (344
  POSTURE_FEEDBACK rows, `115/75 mmHg`), even though the entity field is named `note`. Check
  what a column actually holds before mapping something onto it on the strength of its Java
  field name.

## 2026-08-08 — Live Supabase metadata audit

- Never run `PrintSupabaseTablesTest` for a read-only audit: it drops and recreates `public`. Query `information_schema` through Supabase's read-only endpoint, or use JDBC with both `Connection#setReadOnly(true)` and `SET default_transaction_read_only = on`.
- Count current catalog columns by fully qualified `schema.table`, and report application tables in `public` separately from Supabase-managed schemas such as `auth`, `storage`, `realtime`, and `vault`.
- JShell can fail in the Windows sandbox because Registry-backed preferences are unavailable. Java source-file mode from `D:\tmp` is a reliable one-off JDBC fallback that keeps credential values inside the existing local `.env`.

## 2026-08-08 — Column-level Supabase cleanup audit

- An all-null or constant column is only a candidate, not deletion evidence. Subtype entities, native writers, indexes, constraints, RLS, external consumers, and legal/clinical meaning can keep a currently empty column essential.
- Wide polymorphic tables should usually be split by lifecycle and retention boundary instead of having isolated columns dropped. PostgreSQL's null bitmap makes empty nullable columns cheap; operational complexity and oversized indexes are the larger costs.
- Profile data growth before schema cleanup. `safety_events` contained 9,054 alert-attempt rows for 4,527 logical attempts across only three sessions because `NO_RECIPIENTS` was retried every minute without a cap; deleting rows before fixing that writer would only hide the root cause.
- A contract-drop shortlist must survive separate verification. Security counters, consent/audit fields, temporal interval fields, and compatibility identifiers stay in review until canonical ownership and external-reference gates are explicit.

## 2026-08-08 — `public.users` feature-to-column audit

- An unused physical column can coexist with an active same-named feature stored in JSON. Trace exact ORM annotations, native SQL, and compiled classes before deciding whether to drop the column, migrate the feature into it, or remove the feature.
- Five JPA entities mapped onto one wide row create ownership hazards beyond null-column count: lifecycle callbacks can overwrite another aggregate's fields, defaults become meaningless for unrelated roles, and one shared `updated_at` loses domain-specific meaning.
- Similar-looking profile fields must be judged by exposure boundary, not name alone. Private/public avatar and area/region pairs can be intentional, while `phone`/`phone_number` is a real dual-source problem unless product semantics explicitly distinguish them.
- For security state currently mirrored in `settings_jsonb`, prefer a typed canonical column plus a gated cutover of every reader and writer; do not infer redundancy from all-null physical data while the application still stores the live value elsewhere.

## 2026-08-08 - Duplicate user phone column cleanup

- When removing `public.users.phone_number`, keep `users.phone` as the canonical storage and remap `UserProfile` before applying the contract migration; otherwise Hibernate validation or profile writes will still target the dropped column.
- Native integration fixtures must be migrated with the schema. The embedded PostgreSQL suite applied the full Flyway chain and passed without Docker, while container-based profile tests remained unavailable when Docker was not installed.

## 2026-08-08 — Semantic duplicate-column audit

- Same-name heuristics must be classified by boundary: private/public fields, surrogate ID/business code, identifier/human snapshot, generated JSON projection, provenance snapshot, and polymorphic subtype fields are not interchangeable even when names look similar.
- Live equality does not establish canonical ownership. `users.must_change_password` matched its JSON key on every populated row, but the JPA property is `@Transient` and the application writes the JSON representation; source ownership has to be checked before choosing the cutover direction.
- Default-shaped values can create vacuous equality in wide tables. `safety_events.alert_*_recipient_count` matched generic recipient counts on thousands of action rows largely because the alert columns are NOT NULL with zero defaults, not because both field families represent one writable source.
- A deterministic mirror is lower risk than two independent writers but still has a contract. Exercise configuration JSON is regenerated from typed fields and hashed, so removing duplicate keys requires versioning the hash/projection rather than simply dropping JSON data.
- Identity aliases and their indexes are separate cleanup decisions: all live `users.user_id/person_id` pairs matched, but active entity callbacks still require the alias; one duplicate UNIQUE constraint can be removed safely before the broader identity-adapter cutover.

## 2026-08-08 — Presentation ERD synchronization

- Treat edited domain tabs as the authoritative ordered `(key, name, meta)` projection and verify the Complete tab against all of them before editing.
- For a large draw.io XML file, line-preserving cell-span edits avoid unrelated entity/whitespace churn from DOM serialization; guard the final copy with source and preview hashes.
- For use-case ellipses, `autosize=1` plus `spacing=8` lets Draw.io resize labels during editing; keep content-based initial geometry and verify containment/overlap after resizing.
- In PowerShell array literals, parenthesize concatenations such as `($rowId + '_key')`; otherwise the comma operator can emit a stray suffix (`_key`) and hide the real lookup error.
- Re-baseline the current ERD before every follow-up edit: this file gained three legitimate edges between turns, so relying on the previous `150` count would have deleted or misreported user work; the correct guarded transition was `153 -> 147`.

## 2026-08-09 — Use-case diagram style synchronization

- Treat an already-dirty Draw.io file as authoritative and preserve the existing reference pages before changing the remaining pages.
- For large Draw.io XML, line-scoped patches avoid unrelated serialization churn; validate XML parsing, edge source/target references, `targetUc/ucId` metadata, and geometry containment after visual-style changes.
- If an uncommitted Draw.io version disappears, inspect backups, stashes and unreachable Git objects first; if no exact copy exists, reconstruct from the session's verified use-case/edge definitions and report that limitation explicitly.

## 2026-08-09 — Checklist task detail and support routing

- New template fields that become distributed-task snapshots need both write-path propagation and a live-upgrade backfill; otherwise pre-existing rows lose content and idempotency comparisons can turn into false key conflicts.
- Treat support destinations as client-owned enum-to-route mappings rather than API-provided URLs, so unknown values remain non-navigable and every supported destination is testable.
- On this Windows setup, Flutter verification needs workspace-local `APPDATA`, `LOCALAPPDATA`, and `PUB_CACHE`; the SDK lockfile under `Program Files` may still require an elevated no-pub test/analyze run.

## 2026-08-09 — Exercise history contract diagnosis

- `PaginatedResponse<T>` serializes `data` as a top-level list and keeps page metadata beside it; a mobile parser that assumes `data.content` will fail at runtime even when the list is empty. Cover the actual JSON envelope in a cross-stack contract test.
- Filter labels and request parameters are one contract: exercise-type values such as `YOGA` cannot be sent as `trimesterScope`, and a label such as “Tháng này” must send real `from`/`to` bounds.
- Forward seed reconciliation needs gates for every JPA enum-backed column. The legacy exercise template retained `stage = 'PREGNANCY'` and `template_status = 'ACTIVE'`, values that `TrimesterScope` and `ExerciseStatus` cannot hydrate, so a history title lookup can turn otherwise valid completed-session data into HTTP 500.
- Navigation-only widget tests do not cover a data flow. Exercise history needs both a mobile service/widget test for the canonical paginated envelope and a backend embedded-Postgres endpoint test that reads the seeded completed session.

## 2026-08-09 — End-day artifact and index recovery

- Draw.io Desktop's `--page-index` is 1-based. Treat the generator's canonical page order as the contract, then require validator counts, unique export hashes, and OCR role mapping before publishing; otherwise a duplicated first page can shift every later PNG while still producing valid images.
- A tracked file under an ignored generated directory can appear modified even when its filtered and raw hashes match `HEAD`. Refresh it with `git add -u -- <tracked-path>` and confirm `git diff --cached --quiet` before deciding it is a real change; do not commit generated noise.
- A strict end-day preflight should stop on the wrong branch or any dirty entry. Preserve verified work through explicit commits and ancestry-proven fast-forwards rather than hiding state with stash, reset, clean, or force operations.

## 2026-08-09 - Account lock appeal ERD restoration

- A 5 MB Draw.io XML can exceed both the patch helper's 64 KB transport limit and selective text editors' artifact-size limit. Use a temporary, hash-guarded transformation script with exact-once anchors, candidate XML validation, and an atomic replace that keeps a backup until the final hash matches.
- Rectangle non-overlap is not enough after reflow: an existing auto-routed FK can cross a newly inserted table. Export and inspect the affected modules, then place the new entity in a clear routing corridor and separate relationship labels with explicit waypoints.
- Exporting the full Complete ERD can hang Draw.io Desktop. Build a temporary preview by importing only the relevant table-ID prefixes and edges into a one-page document; constructing a new XML tree is much faster than repeatedly removing thousands of cells.
- Derive summary counters from the final XML, not from the existing subtitle. Restoring one table and two FK edges changed the authoritative Complete projection from 62/147 to 63/149, while the old displayed 150-FK counter was already stale.

## 2026-08-10 — Checklist cadence design discovery

- `ChecklistRangeUnit.DAY|WEEK|MONTH` measures an eligibility or due-date offset; it is not a recurrence discriminator. A daily recurring checklist needs explicit cadence metadata so existing one-day milestones are not silently reinterpreted.
- The canonical checklist aggregate can represent daily or weekly occurrences without a new table: keep template policy on `care_item_templates` and use the existing `checklist_instances.window_start/window_end` plus the centralized distribution-key factory for occurrence identity.
- `stage = null` already means lifecycle-neutral compatibility, especially for FAMILY-only authoring. Do not repurpose every null-stage template as cross-stage DAILY; either require an explicit discriminator with guarded semantics or publish separate stage-specific templates.
- The Spreadsheets skill treats a missing `load_workspace_dependencies`/artifact-tool runtime as a hard read blocker. Do not fall back to alternate XLSX libraries or ZIP/XML parsing; report the limitation and keep the source workbook unchanged.
- Architecture reviewer gates should attack both the target design and the current runtime: the checklist GET path accepted a caller date and rollover historized/cancelled old windows, so those are explicit cutover tests rather than assumed invariants.
- Additive cadence fields do not protect old clients by themselves. A server capability/contract gate must control both projections and actions, with a documented status/envelope and cache variation.
- For shared-table root/item entities, “add columns” is incomplete design evidence. State physical nullability, entry-type row-shape checks, JPA ownership, clone propagation, and effective-timestamp boundary semantics together.
- Recurrence safety needs budgets at every layer: per-context candidates, whole invocation, and projected child rows, serialized by one existing lock/coordination primitive with deterministic continuation.
- A source count is not traceability. Pin source hashes and import batches, exclude structural checkboxes, assign hierarchical leaf locators/text hashes, and require a bijection before clinical approval.

## 2026-08-10 — Recovery branch integration

- A branch-specific `.gitignore` can make a worktree look clean on the recovery branch and expose many existing generated artifacts after switching to an older target. Group the untracked paths and compare them against the incoming tree before proceeding; a collision-free fast-forward can preserve the artifacts while applying the incoming ignore rule.
- A timed-out `git commit` may still have completed successfully. Check `HEAD`, the index/worktree status, `index.lock`, and live Git processes before retrying so the same content is not committed twice.
- Publishing a locally merged branch is a separate external action from merging and deleting a local recovery branch. If push is not explicitly authorized, leave remote refs untouched and report their exact lag.

## 2026-08-10 — Cadence revision reality check

- Re-hash source artifacts at review time: the pregnancy checklist had already moved from the documented rejected hash to a 62-leaf decision-aligned revision, so provenance remained gated but “source unsynchronized” had become stale.
- A migration can make legacy target columns nullable without dropping their ORM fields; V1 adapters still need nullable mappings while V2 writers/projections explicitly use null, and only a later zero-consumer gate permits physical removal.
- When a design introduces gestational dating authority, bind it to the existing Journey routes and request/response DTOs; a domain rule without an endpoint/version/error contract is not implementation-ready.

## 2026-08-10 — Admin-authored exercise and posture capability

- Exercise content and posture-analysis readiness are separate governed assets. `CONTENT_ADMIN` can author a `DRAFT`, while a posture-enabled publish needs an independently validated, versioned configuration owned by `SYSTEM_ADMIN`/ML operations plus clinical review.
- `supportsPostureAnalysis` is a request, not evidence of readiness. The current resolver and sidecar support only `bicep_curl`, `plank`, `squat`, and `lunge` on one pinned concept-demo model that is not validated for pregnancy or the CareBridge population.
- Validate posture configuration before activation or publication. Runtime fallback for a missing or invalid config can keep a session alive while hiding that only generic rules ran, and a valid-looking but semantically wrong exercise key can produce confident feedback for the wrong movement.
- New exercises should default to posture analysis off. A new movement family requires an engineering release for model/rule work and verification; the admin UI must not imply that adding catalog content trains or extends ML automatically.
- Do not infer a model family from a localized title or seeded UUID. Expose a canonical server-owned posture capability/key only after the content-to-model mapping has been reviewed.
- `rg --files` can omit ignored `_bmad-output` context files. When a skill names a persistent fact path and the user supplies its exact location, read that path directly rather than concluding that the file is absent.

## 2026-08-10 — Cadence revision adversarial convergence

- Re-read normative artifacts after concurrent reviewer edits and remove findings that the current bytes already close; a review against a stale revision is worse than a shorter review.
- A monotonic dating revision is not enough to make correction prospective: app-independent catch-up also needs a durable revision-effective instant, otherwise restart logic can backfill pre-correction periods under the new anchor.
- Current Family authorization and historical materialization are different predicates. Repair needs temporal membership/permission bounds so a new grant cannot silently manufacture earlier recipient-owned History.
- An append-only transition event cannot later acquire its own `effectiveTo`; derive the interval end from the next event or append an explicit prior-closure fact. Also compare every canonical identity/order tuple across spine and addendum—omitting `accessEpoch` from either the repair cursor or V2 hash recreates re-grant collisions.
- A final architecture PASS can be narrower than release readiness: once canonical interval, cursor, and key tuples converge, preserve migration, client-floor, provenance, scheduler, and alert evidence as separate activation gates instead of reopening settled design decisions.

## 2026-08-10 — Exercise authoring improvement roadmap

- Fix safety truth before visual polish: new exercises currently request posture analysis by default while publication ignores posture readiness. Default posture off and enforce a static, no-mutation publish gate before wiring any Web activate action.
- A create workflow must distinguish save, preview and submit-review. Navigating away without persisting, swallowing the API error into a preview route without an ID, and presenting non-interactive toolbar/upload affordances all make the current form dishonest.
- Reuse the existing `care_item_templates` approval columns and checklist state-machine pattern, but not the checklist entity/service wholesale: their `SQLRestriction`, triggers and rules are scoped to `TEMPLATE_ROOT`, while exercise rows use `EXERCISE_TEMPLATE` and `template_status`.
- Keep two axes: `content_status` for review and `template_status` for catalogue publication. A public compatibility flag must represent effective posture readiness, not merely an author's request.
- Validate registry key/model/rule configuration before save/activation and keep live sidecar health outside the publication transaction; transient provider outages belong to runtime degradation, not editorial approval.
- Cross-role setup needs a read-only posture-candidate projection for `SYSTEM_ADMIN`. Requiring a copied exercise UUID is a symptom of the current RBAC boundary, not an acceptable admin workflow.
- Media semantics are a contract: a field labeled video cannot be rendered everywhere as an image. Either narrow the MVP to an image or add an explicit media type and matching validation/renderer in a separate compatible wave.

## 2026-08-10 — Exercise authoring implementation

- A boxed Boolean can support both backward-compatible omission and strict explicit-null rejection: initialize the create DTO field to `false`, retain `@NotNull`, and normalize at the mapper boundary with `Boolean.TRUE.equals(...)`.
- Keep the already-published idempotent return before readiness checks, then run the static posture policy before changing status, saving, or writing an activation audit. This preserves old retries while making every real publication fail closed without partial mutation.
- A truthful draft form needs to retain the first returned server ID as local state: POST once, PUT that exact ID thereafter, and navigate to preview only after the awaited persistence succeeds.
- Parse the shared backend error envelope at the feature API boundary so stable codes and field details remain typed; UI catch blocks can then preserve form data and render actionable inline feedback without duplicating Axios-shape assumptions.
- Treat every error-envelope element and successful response identity as untrusted at runtime; malformed `details` entries or a mismatched exercise ID must degrade to visible failure instead of hiding the original save outcome.
- A save notice is only truthful if the editable snapshot cannot change in flight. Disable authoring controls while persistence is pending, then re-enable them after success or failure.
- A transition-time publish gate does not settle edits to an already-published record, concurrent activation, or ambiguous POST recovery; record those as explicit policy/idempotency follow-ups rather than silently broadening a narrow safety sprint.
- Review trails stored under ignored `_bmad-output` paths need separate UTF-8 and link-anchor validation; `git diff --check` cannot inspect an ignored spec, and terminal mojibake can make a correct patch anchor fail safely.

## 2026-08-10 — Final cadence architecture reality gate

- A persisted V1/V2 discriminator does not select a wire contract by itself. When user-created aggregates split into target-bearing V1 and targetless V2 namespaces, bind the existing create/read/delete/import routes to explicit version negotiation (or name a separate V2 route) and test old-client isolation plus cross-contract idempotency.
- If the prescribed `uv run` wrapper cannot initialize its cache under the sandbox, run the same deterministic review script directly, retain the wrapper failure as an environment note, and still re-hash every frozen input after writing the review.

## 2026-08-10 — Final cadence adversarial architecture gate

- An architecture PASS and activation readiness are different claims: independently reconstruct the row owners, immutable timelines, key tokens, locks, and authorization order first, then preserve migrations, clients, repair, audit, and operations as evidence gates rather than treating their pending implementation as a spine inconsistency.
- A V2 union cannot promise epoch-authorized mutation of legacy Family rows unless migration gives those rows a normative epoch boundary or the compatibility contract explicitly hides them after revoke; otherwise current-membership authorization and stored-epoch authorization are both plausible and diverge after re-grant.

## 2026-08-10 — Concurrent normative review freeze

- Re-hash both normative artifacts after every concurrent edit and immediately before and after generating the review artifact. Bind the verdict to one explicitly frozen hash pair and stop the approval pass on any drift; a matching hash for only one companion document is not a stable review boundary.

## 2026-08-10 — User-created route contract recheck

- Closing a cross-contract API seam requires the complete live operation matrix, not only a create endpoint: bind POST/GET/DELETE/template self-assignment, header-first error ordering, DTO/context rules, tombstones, client helpers, and activation tests together; keep the still-V1 implementation clearly labeled as cutover debt.

## 2026-08-10 — Frozen Family-epoch integrity gate

- An epochless legacy parent key cannot safely survive revoke/re-grant by restamping the old aggregate. Initialize retained-row authority only through one atomic empty-timeline baseline, quarantine unstamped rows when a timeline already exists, and make post-re-grant V1 creation authorize first and then require the epoch-qualified V2 contract without disclosing the old parent.

## 2026-08-10 — Cadence ownership migration gate

- Classify a compatibility row by its persisted recipient owner before applying Family access epochs. A Mother-owned row that a legacy route merely projected to Family cannot be stamped, relabeled, or cloned without fabricating recipient History; preserve it on Mother surfaces, exclude it from Family after cutover, and materialize any Family work prospectively with independent state.

## 2026-08-10 — Implementation-plan replacement completeness

- A refreshed plan that supersedes an older plan must map adopted base-spine invariants as well as the new addendum. A blanket final-phase instruction to run every normative verification row does not assign missing implementation work such as route removal, compatibility adapters, timezone authority, or audit lifecycle controls.

## 2026-08-10 — Exercise history implementation

- Nullable `OffsetDateTime` parameters in JPQL can remain untyped in PostgreSQL when used only in `:value IS NULL`; `COALESCE(:value, typed_column)` preserves optional-bound semantics while giving PostgreSQL a resolvable type. Prove both-null, from-only, to-only, and bounded cases on real PostgreSQL.
- `TrimesterScope.ALL` is an exercise applicability scope, not the query wildcard. A specific trimester filter must include exact-scope and `ALL` exercises, while only a null request parameter means no filtering.
- Account-scoped Flutter loads need all three guards: load generation, `AuthState.sessionGeneration`, and `userId`. Listen for session changes, clear rendered data immediately, reject late results, and make custom auth/service injection fail closed so test seams cannot mix credentials.
- Material chips retain internal selection animations even after removing an explicit `AnimatedContainer`; honor `MediaQuery.disableAnimations` with `ChipAnimationStyle` and test the configured animation styles directly.
- Pin live-upgrade tests to the migration under test rather than “latest”, then inject an invalid enum value and assert both the stable failure marker and transaction rollback. This keeps the test durable as later migrations are added and proves the fail-fast path.

## 2026-08-11 — Recommendation BMI synchronization review

- A deterministic replay key must still validate the stored observation's care-subject and metric ownership; otherwise a colliding identifier can silently suppress another user's health record.
- A focused Maven run may need temporary network access even when the parent POM appears in `.m2`; record the environment failure separately from actual test failures.
- On this repository, the code-review-graph pre-commit hook can hang behind a stale `next-index-*.lock`; only skip the hook after running graph review and focused tests independently, and commit an explicit file list.

## 2026-08-11 — Checklist implementation-plan approval gate

- A production pre-activation gate is not executable merely because roots remain Draft: the plan must define a bounded writer state transition for allowlisted validation, a separate general enablement before the scheduled boundary, and a scheduler fail-closed check on the bound configuration revision.
- Family checklist authorization must be assigned as implementation work by stored `origin`: VIEW gates distribution/read and private user-created actions, while system-template COMPLETE/REOPEN additionally requires COMPLETE; retries still re-authorize the stored epoch.
- Break-glass evidence is complete only when a failed secondary write denies access and successful secondary evidence is reconciled idempotently into the primary audit trail.
- Administrative approval should update only plan metadata and retain the reviewed body hash; this preserves the exact independent-review boundary while making the lifecycle status explicit.
- P0 brownfield inventory exposed a real contract gap that static design review alone could miss: Today GET still performs reconciliation/history writes and overdue checklist tasks remain actionable. Treat these as red-test blockers before designing the resolver/repair cutover.
- The existing target-required V1 entity/approval surface, client-local multi-method dating, and legacy migration rehearsal fixtures must be classified as compatibility seams before any targetless V2 or server-dating implementation is claimed ready.
- A green unit baseline does not clear the checklist cutover: the PostgreSQL baseline still exposed four existing authorization/audit fault-injection failures, while canonical Flyway passed and the Flutter subset timed out without a result. Record each as its own evidence state instead of collapsing them into one PASS/FAIL.
- For accepted Family membership, invitation expiry is not a revocation signal; policy must gate pending acceptance only and use current VIEW/epoch after acceptance. Also, a fault-injection test that targets an audit action absent from the exercised lifecycle proves a fixture gap, not atomicity.

## 2026-08-12 - Firebase phone authentication contract

- Keep Firebase PHONE proof behind intent-specific register/login endpoints. A generic federated auto-create path silently bypasses registration fields, password policy, and the product rule that unknown phone numbers must not auto-register.
- An SMS-verified Firebase token may link an existing CareBridge account only when the stored canonical phone was already verified; an optional profile phone is not an authentication factor and must require an authenticated linking/recovery flow.
- Client validation must mirror the server's actual phone and password policy before sending an SMS. Otherwise the product can consume SMS quota and then deterministically reject the request.
- Preserve the Firebase confirmation proof after a successful code exchange until the backend session exchange succeeds, and preserve the previous native challenge when a resend fails. These are distinct retry boundaries.
- Flutter verification on this Windows workspace needs SDK/cache writes outside the sandbox; use an approved direct snapshot run with workspace-local `APPDATA` and record the resulting test evidence. A failed Maven rerun can be unrelated test-compilation drift, so distinguish it from auth test failures.

## 2026-08-12 - Firebase CLI credential output

- Never use `firebase login:list --json` when reporting or inspecting auth state: Firebase CLI 15.26.0 includes OAuth credential fields in the JSON response. Prefer plain `login:list`, and immediately log out affected accounts if sensitive fields are emitted.
- Firebase CLI can select projects and deploy supported auth configuration, but Phone provider enablement and SMS-region policy are not exposed by the CLI workflow; those require Firebase Console/Identity Platform configuration after the correct project/account is confirmed.
- When database trigger/migration semantics already implement the accepted-membership boundary but Java policy diverges, treat it as a cross-layer authorization split: preserve generic membership behavior and add a checklist-specific accepted-VIEW/epoch predicate instead of globally deleting expiry checks.

## 2026-08-11 - Baby birth Growth projection

- Deterministic Growth replay must validate every existing canonical row's subject, metric, source, group, and legacy identity before reuse; a stable UUID alone is not fail-closed idempotency.
- For optional pediatric measurements, verify the one-row PostgreSQL projection as well as the two-row happy path, and assert the real serialized mobile payload rather than only the request object.

## 2026-08-11 - Checklist P0 evidence continuation

- Run embedded-Postgres integration classes in isolation when they share a static fixture initializer; a concurrent Maven invocation can produce a class-startup collision and must not be retained as test evidence.
- Fault-injection tests must arm an audit action actually emitted by the exercised lifecycle. A Mother correction historyizes the old window without `CHECKLIST_CANCELLED`; targeting the real `CHECKLIST_ASSIGNED` boundary preserved the atomic-audit policy and yielded clean evidence.
- The canonical reconciliation/outbox/quarantine tables are pre-retirement migration evidence, not proof of a post-finalizer repair queue. Record the missing durable retry/replay primitive as a P0 blocker rather than inventing a new table.

## 2026-08-11 - Checklist P0 closure contract

- A logical `SECURITY_*` stream inside the primary `audit_events` table cannot satisfy a normative independent break-glass sink requirement. Keep the secondary destination outside the primary persistence boundary with a distinct credential/failure path, fail closed only for break-glass reads when its write fails, and reconcile successful evidence by correlation.
- When reusing an append-only audit table for retry evidence, freeze the exact JSON shape, sole writer, authorized replay query/order, authoritative-row success rule, legal-hold retention, and privileged purge path. Hash context and period identity instead of persisting raw period/date tokens or recommendation copy.

## 2026-08-11 - Checklist P1/P2 legacy contract compatibility

- Nullable V1/V2 discriminators need one explicit compatibility interpretation at every write-time check and contract-matching trigger: `NULL` is legacy V1, while only explicit `2` activates V2 targetless/period requirements. Relying on SQL three-valued logic makes the rule accidental and hides future regressions.
- If a migration makes `correlation_id` mandatory for every `CHECKLIST_*` audit row, legacy generic audit callers must receive a generated correlation at the audit boundary; fixing only the migration leaves the first old template write as a production rollback.

## 2026-08-12 - Firebase Console routing for Phone Auth

- Firebase CLI 15.26.0 exposes only `auth:export` and `auth:import` under the `auth` namespace; it cannot enable the Phone provider or configure SMS region policy through an official command.
- `firebase open auth` is not a supported Authentication shortcut in this CLI version and can be interpreted as a Realtime Database resource, producing a misleading database-initialization prompt. Never follow that prompt for Phone Auth; open the project overview or the official project-scoped Authentication Console URL instead.
- CLI login state and browser Console login state are independent. Confirm the account/project with plain `login:list` and `projects:list`, then expect a separate browser sign-in before Console-only settings can be changed.

- When Firebase Web SDK config, Authorized domains, Phone provider, and SMS region policy all match, `auth/invalid-app-credential` from an automated in-app browser points to reCAPTCHA rejection before SMS dispatch. Verify in a normal user browser; do not add unrelated domains or retry repeatedly from the automation harness.
- On Flutter Web, `Failed to initialize reCAPTCHA Enterprise config. Triggering the reCAPTCHA v2 verification.` is a non-fatal Firebase Auth fallback, not proof that Phone Auth failed. Require a later `codeSent` event or an exact `verificationFailed` error code before changing Enterprise, billing, domains, or client code; backend refresh 401 and FCM Web token failures are independent streams.

## 2026-08-12 - Pregnancy dating authority and checklist occurrence revisions

- A resolved pregnancy tuple must be reused only when basis, positive revision, effective instant, active stage, and quarantine state all agree; raw LMP/EDD columns alone are compatibility data, not checklist authority.
- Include the gestational dating revision in occurrence identity and current-scope comparison. Otherwise a dating correction that leaves the same calendar window can silently reuse the prior occurrence and keep stale Mother/Family work active.

## 2026-08-12 - P4 lifecycle payload hardening

- JSONB audit payloads are legacy/untrusted input: entity hydration must neutralize malformed enum/date/number/UUID values so history/timeline reads fail closed instead of throwing during `@PostLoad`.
- Native PostgreSQL casts over audit payloads need a regex/CASE guard, including UUID projections; otherwise one malformed legacy row can make an entire timeline query fail.
- Embedded migration tests may log Flyway errors while passing when the error is the deliberate fail-closed assertion; report that distinction explicitly in test evidence.

## 2026-08-12 - Firebase Phone Auth callback lifecycle

- On native FlutterFire, the `verifyPhoneNumber` Future can finish after callback registration, before `codeSent`, `verificationFailed`, or timeout. Keep the UI in a sending state until one of those callbacks establishes the real outcome; the setup Future is not SMS-dispatch evidence.
- Native auto-verification can outlive its screen. Guard the backend-to-session persistence boundary as well as the widget callback, and block normal back navigation while a verified Firebase proof is being exchanged, so an abandoned flow cannot overwrite a later session.
- Allocate Web Phone Auth request generations before awaiting `signInWithPhoneNumber`; otherwise an older completion can clear a newer `ConfirmationResult`. Preserve the prior challenge while a resend is pending or fails, and retire it only when the newer challenge succeeds.
- Material `SegmentedButton` may replace the selected segment icon with a checkmark. Widget tests should assert stable method labels, values, and keys within the selector instead of the selected icon's rendered implementation detail.

## 2026-08-12 - Checklist Family epoch and targetless V2 continuation

- Bind checklist action authorization to the accepted membership epoch and re-check it after the action/idempotency locks; otherwise a revoke/re-grant race can replay an old command under a new grant.
- Family checklist projections must use materialized FAMILY rows with the epoch snapshot; projecting writable Mother rows makes an old membership indistinguishable from a new grant.
- Persist the checklist contract discriminator on every leaf, not only on the root. A targetless V2 root with an unmapped V1 leaf passes service validation but fails the database invariant at flush time.

## 2026-08-12 - Targetless user-task adapter and verification

- Keep the V1 target-bearing request as a compatibility adapter while routing new mobile-created tasks through an explicit `X-Checklist-Contract-Version: 2` header; namespace the V2 parent key so a V1 aggregate cannot receive V2 children.
- If a Flutter service accepts injected request callbacks for tests, the V2 callback must fall back to that same injection instead of silently calling the real API; otherwise widget tests leave the process and hang on network/SDK state.
- The Maven wrapper's PowerShell path can fail on a null reparse-point target in this Windows workspace. Use the already-installed Maven distribution directly for evidence, and keep Flutter test timeouts separate from formatter/analyzer evidence.

## 2026-08-12 - Mobile/Web dating metadata UI release slice

- Treat LMP and EDD as the only client dating authorities; remove legacy conception, gestational-age, and cycle-length controls instead of mapping them to a guessed EDD.
- Optimistic dashboard reconciliation must preserve explicit server nulls for active pregnancy dating metadata; otherwise unresolved/quarantine can resurrect a stale week or Plan.
- User-entered EDD is `SELF_REPORTED`/`ESTIMATED` unless a separately verified clinical source is present. Keep targetless V2 labels neutral across Today, History, and task detail.

## 2026-08-13 - Checklist authoring form week numbering

- Keep checklist authoring weeks source-facing (1-based) and convert to the existing runtime eligibility offset (0-based) at the UI boundary; this keeps Plan 2 as 21-25 while preserving persisted offsets 20-24.
- Removing the visible contract/target controls requires deriving the version from stage and retaining legacy target fields only in the compatibility payload path.

- For recurring checklist authoring, keep the root cadence and item markers consistent: persist item markers in the existing JSON metadata, reject mixed cadence rows, and carry the root contract version into distribution so a V1 postpartum recurrence cannot be stamped as V2.
- A daily period is a local calendar date, not a gestational week. Use the passed `ZoneId` for the period key, replay missed dates as catch-up History rows, and check the daily period in current-scope filtering.

## 2026-08-13 - Checklist authoring follow-up verification

- Contract propagation must cover non-cadence V2 self-assignment as well as
  recurring roots: persist the V2 discriminator on parent and leaf, use a
  separate occurrence-key namespace, and avoid the legacy personal-instance
  fallback when resolving a targetless row.
- Lifecycle history cancellation must compare the contract/cadence identity;
  a same-window V1 row is not obsolete V2 work. UI transitions from an
  open-ended window to a single week must also clear the stage-exit mode.
- Keep authoring cadence checks and payload filtering based on the same set of
  populated rows, otherwise a blank placeholder can make UI and backend
  validation disagree.

## 2026-08-13 - V2 checklist requiredness redesign

- Targetless V2 can still carry an explicit item-level `is_required` boolean;
  keep `target_subject` nullable while changing requiredness rules independently.
- A contract change needs coordinated form payloads, approval/distribution
  validation, sequence resolution, runtime task snapshots, and a forward
  migration; changing only the database leaves the sequence blocked.
- Backfill legacy V2 nulls from the root template type (`MANDATORY` true,
  `OPTIONAL` false) and fail closed for unknown roots; preserve user-created V2
  task rows without a template with a deterministic false default.

## 2026-08-13 - POSTPARTUM checklist context routing

- A consolidated lifecycle stage does not make its contexts interchangeable. In
  POSTPARTUM, Journey carries maternal `deliveryDate` while Baby carries `birthDate`;
  candidate routing must match root and leaf anchors to the context before distribution.
- `recipient_scope` answers who receives a checklist, `target_subject` describes whom a
  leaf concerns, and care context supplies lifecycle dates. Treat these as separate axes.
- Creating a standalone Baby must not silently transition or date a maternal Journey.
  Maternal POSTPARTUM state belongs to the explicit pregnancy-outcome workflow, while
  direct POSTPARTUM Journey creation must collect its own delivery/outcome anchor.
- Approval validation must cover leaf due anchors as well as the root eligibility anchor;
  otherwise a root may pass and an incompatible leaf can throw inside a transaction.
- When consolidating `BABY_CARE` into `POSTPARTUM`, update schema authoring guards and
  Java compatibility semantics together. An enum alias can make runtime accept a stage/
  anchor pair that the catalog trigger still models under the legacy stage name.
- Mixed-root repair must classify both V1 target-bearing leaves and V2 targetless leaves;
  V2 ownership comes from the explicit due anchor, not a target field that must remain null.
- Catalog split migrations should clone as Draft, verify every source leaf is represented,
  activate replacements, archive the source, and leave materialized instance/task snapshots
  untouched. Add a database trigger so direct writers cannot recreate the invalid shape.
- Candidate context compatibility includes anchor presence, not just context type. A Baby
  without birth date or Journey without delivery date should be excluded before distribution.
- Checklist root policy and item requiredness are separate persistence axes: a root stores
  `template_type` (`MANDATORY`/`OPTIONAL`), while `is_required` belongs to each
  `CHECKLIST_ENTRY`. Always inspect `entry_type` before treating a root NULL as data loss.

## 2026-08-14 - Separate postpartum and baby-care checklist verification

- When Baby Care is grouped by `careContextId`, section headings must include the baby
  label and tests must assert the new contextual heading instead of the former generic one.
- An active `flutter run` owns the Flutter wrapper lock. Run focused tests through the
  current SDK tool snapshot with `--no-pub` rather than terminating the developer session.
- Technical migration review and clinical provenance sign-off are independent approval
  gates; preserve the provenance check before returning the generic migration-path error.
- Migration repair must fail closed for ambiguous legacy roots: archive/disable the source,
  create a disabled Draft replacement marked review-required, and preserve runtime history.

## 2026-08-14 - Exercise detail media and spoken posture feedback

- A single exercise `mediaUrl` is sufficient when product scope permits one media item;
  local demo overrides should be deterministic by exercise identity/title and return null
  for exercises without approved media instead of inventing placeholders as data.
- Pin `video_player` exactly to the Flutter baseline supported by the repository; a caret
  constraint can resolve to a newer package whose SDK requirements exceed the intended
  deployment toolchain.
- High-frequency posture warnings need both a global speech throttle and an exact-message
  dedupe window. Keep TTS behind an injectable boundary, preserve visual feedback as the
  authority, and stop speech on pause, completion, and disposal.
- Nullable values are not promoted reliably across asynchronous gaps and compound assignment;
  bind the fetched exercise detail to a final non-null local before starting the session.

## 2026-08-14 - Database Design to conceptual ERD projection

- Re-baseline a large Draw.io source immediately before transforming it. The clean source
  advanced from 62 tables/147 edges to 63 tables/149 edges because
  `account_lock_appeals` was legitimately added between turns.
- Draw.io table-child geometries use absolute pixel widths. After removing a datatype
  cell, set the attribute-name cell width to its parent detail container width; `width=1`
  means one pixel, not 100 percent. XML/count checks cannot catch this clipping, so render
  and inspect a focused crop before approval.
- The Draw.io CLI `--page-index` is one-based in this environment: the appended fourteenth
  diagram exports with `--page-index 14`. The Electron CLI may return a non-zero status
  even after creating the PNG, so verify the output file and inspect it instead of trusting
  the exit code alone.
- Keep PowerShell 5 transformation helpers ASCII-only or add an explicit compatible
  encoding marker; construct non-ASCII separators such as the middle dot at runtime.
  Guard every write with the current SHA-256 and validate the complete temporary XML before
  copying it over the exact workspace target.
- For content-based table widths, measure rendered labels with their actual font weight,
  round to a stable pixel grid, and preserve each table center while updating every nested
  row/detail/name geometry; changing only the outer table width causes clipping or drift.

## 2026-08-14 - Flyway production baseline squash

- Generate a production baseline from the materialized PostgreSQL catalog rather than
  concatenating historical migrations; this preserves the final tables, native objects,
  constraints, triggers, ownership, and ACLs without carrying upgrade-only logic forward.
- Keep schema DDL and reference data in separate Flyway files and enforce that boundary in
  tests: V1 must reject top-level seed DML, while V2 must reject DDL and use an explicit
  allowlist with fixed counts and demo/account-owned tables proven empty.
- A focused clean-bootstrap test can prove the migration deliverable even when the broader
  suite contains stale fixtures, but both evidence states must be reported. Do not claim the
  full suite passed, and do not broaden the migration task to repair unrelated fixture drift.
- Database reset is gated by endpoint and identity. If PostgreSQL/`psql` is unavailable on
  the approved loopback endpoint, leave DROP/CREATE and strict startup unchecked; never
  substitute a remote database or install tooling without authorization.

## 2026-08-14 - Hierarchical Checklist Excel import

- For a two-sheet parent/child import, normalize the shared checklist code consistently,
  make any invalid child invalidate its parent preview, and perform source-facing week to
  persisted-offset conversion at the parser boundary so UI and backend contracts stay aligned.
- Async file previews need a generation token, and file replacement must be locked while the
  import request is in flight; otherwise stale parse or POST results can be shown against a
  newer filename.
- Bound batch size in both the UI and Jakarta validation while preserving one transaction per
  checklist row, so partial failures remain isolated without allowing unbounded sequential work.

## 2026-08-15 - Checklist Excel dropdown validation

- SheetJS may return an `ArrayBuffer` even when TypeScript casts the result to `Uint8Array`;
  normalize the runtime byte type before browser download or a typed-array copy can emit a
  zero-filled workbook.
- When list validation requires targeted OpenXML injection, preserve worksheet child ordering,
  fail fast if the workbook entry is missing, and verify the generated file with Excel itself;
  checking only for XML substrings does not prove the dropdown is usable.
- Keep inline validation lists below Excel's 255-character formula limit and split long codebook
  guidance across readable rows so every supported value remains visible in the template.

## 2026-08-15 - Canonical Checklist Excel template asset

- When stakeholders maintain the approved workbook in `08_References`, publish that exact
  binary as the Web download asset instead of rebuilding a second hard-coded workbook in
  TypeScript; this preserves formatting, guidance, dropdown validation, and future edits.
- Protect the synchronization with a byte-equality contract between the reference and public
  asset, then separately assert sheet names, row counts, parser validity, and the modal's
  download URL so both workbook content and UI wiring are covered.
- Verify the production build copies the workbook without changing its hash, and open the
  runtime asset with Excel when data-validation behavior is part of the acceptance criteria.

## 2026-08-15 - Workspace-local PostgreSQL reset and strict startup

- Canonicalize the server address in the JDBC identity gate with
  `host(inet_server_addr())` before comparing it to `127.0.0.1`; keep the maintenance
  database, port, and current user in the same fail-closed assertion before DROP.
- A trust-auth disposable cluster still needs a complete, nonblank datasource tuple because
  the runtime environment post-processor validates configuration shape before connecting.
  Use a clearly non-secret local placeholder and redact it from evidence.
- A protected readiness endpoint may correctly return HTTP 401. Treat the Flyway completion,
  Hibernate validation, `Started BackendApplication` log, JDBC history proof, and clean process
  observation as startup evidence instead of weakening endpoint security for a local check.
- Terminating a verified `spring-boot:run` process makes Maven end nonzero. Report startup and
  teardown as separate evidence; do not describe forced application termination as graceful.
- PostgreSQL and Java tooling can hit Windows restricted-token or external-cache sandbox gates.
  Preserve the exact loopback flags and rerun the same command with approved escalation rather
  than changing endpoints, copying dependencies, or installing replacement tooling.
- PostgreSQL 18 records the grantor for role memberships. A non-`CREATEROLE` Flyway runner
  cannot remove a temporary retention-owner membership granted by an administrator, so an
  exact two-file baseline that ends with an unreachable NOLOGIN owner needs a privileged,
  dedicated migration runner and an early stable fail-closed marker for runtime-style roles.
- `PUBLIC` is a pseudo-role, not a normal login for `has_function_privilege` assertions.
  Verify revoked PUBLIC function ACLs through `aclexplode(...).grantee = 0`, and pair catalog
  checks with an actual role-context invocation/denial test.
- When asserting sequence ACLs, source relation OIDs from `pg_catalog.pg_sequence` before
  calling `has_sequence_privilege`. A `pg_class.relkind = 'S'` predicate alone does not
  guarantee evaluation order, so PostgreSQL may invoke the privilege function on a TOAST
  relation and fail with `is not a sequence` before applying the filter.

## 2026-08-15 - Supabase CLI connectivity verification

- On this Windows workspace, `supabase` is not on the global PATH and `npx.ps1` is blocked by
  PowerShell Execution Policy. Invoke the cached `supabase.exe` directly (or use `npx.cmd` when
  it is responsive) and allow its user-profile telemetry write when running in a sandbox.
- Verify the Management API and database paths separately: `projects list --output json` proves
  CLI authentication/project linking, while `migration list --linked` proves a read-only remote
  PostgreSQL connection. An empty migration list with exit code 0 is still successful connectivity.

## 2026-08-15 - PRE_PREGNANCY Today sequence with empty checklist items

- A Today sequence pill such as `1/5` can be a synthetic server projection when no current
  instance exists; it is the sequence position, not evidence that one item or one instance is
  visible. Inspect `currentInstanceId` and all Today sections before interpreting the pill.
- V2 non-cadence materialization deliberately persists the sentinel period identity
  `O:USER_CREATED`. Current-scope reconciliation must recognize that supported identity for
  `SET`/`SEQUENCE_STEP`; treating every configured schedule/policy as a cadence handled by
  `ChecklistPeriodIdentity` makes the expected key null and immediately historicalizes valid work.
- `LIFECYCLE_STAGE_OBSOLETE` is a generic reconciliation reason for any `!isCurrent` result. It
  does not by itself prove a real lifecycle-stage transition; compare the journey stage, catalog
  policy, instance period identity, timestamps, and current-scope predicates.
- The existing pre-pregnancy PostgreSQL test calls Today with `reconcile=false` and seeds roots
  without the live `SET`/`SEQUENCE_STEP` fields, so it cannot catch the create-then-reconcile seam.
  A regression must exercise automatic ensure plus reconciliation with the production catalog shape.

## 2026-08-15 - PRE_PREGNANCY current-scope fix and review

- Treat persisted occurrence identity as a shared writer-reader domain contract. Centralizing the
  exact V2 non-cadence tuple prevents distribution and reconciliation from drifting independently.
- When tightening a fail-closed branch, explicitly test older supported shapes and configured
  null-key identities; compatibility and security-style rejection can regress in opposite directions.
- Preserve both `reconcile=true` and `reconcile=false` coverage while adding an end-to-end regression,
  then prove repeat reads, required completion, and sequence advancement against PostgreSQL.
- A code fix that prevents future historicalization does not authorize restoring an already-historical
  production row. Keep data remediation separate, explicit, scoped, and independently approved.
