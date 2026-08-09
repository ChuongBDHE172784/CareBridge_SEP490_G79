# Lessons learned

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
- In PowerShell array literals, parenthesize concatenations such as `($rowId + '_key')`; otherwise the comma operator can emit a stray suffix (`_key`) and hide the real lookup error.
- Re-baseline the current ERD before every follow-up edit: this file gained three legitimate edges between turns, so relying on the previous `150` count would have deleted or misreported user work; the correct guarded transition was `153 -> 147`.
