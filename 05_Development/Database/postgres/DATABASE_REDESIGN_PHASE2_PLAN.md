# DATABASE REDESIGN PHASE 2 — MIGRATION PLAN

Approved baseline: 70 tables (69 business/application + `flyway_schema_history`).
Application code remains unchanged until the database validation gate passes.

| Wave | Scope | Canonical work | Drop policy |
|---|---|---|---|
| 1 | account/person/auth | persons, care_subjects, users, identities, sessions, revocations, challenges, deletion requests | retain legacy auth/profile tables |
| 2 | mother/baby | journeys, lifecycle events, maternal observations/exercise, care logs, growth, milestones, vaccination | retain legacy mother/baby sources |
| 3 | community/expert | public community content/interactions and professional identity/credential lifecycle | retain source tables |
| 4 | triage/knowledge | triage sessions/evidence, red-flag rules, health context, knowledge sources/reviews | retain intake/triage sources |
| 5 | health/files/devices | health records, attachments, record links, device connections/observations | retain file/device sources |
| 6 | family/care plan | care groups/members, scheduled items, tasks, checklist items/templates | retain reminder/task sources |
| 7 | content/moderation | verified content links, moderation cases and append-only events | retain report/action sources |
| 8 | safety/facility | safety config/session/events/actions, contacts, administrative areas/facilities/nearby support | retain safety/facility sources |
| 9 | audit/security/consent/archive | audit/security/permissions/config, expenses and domain archives | retain audit/security sources |
| 10 | final pending-drop cleanup | evidence scans and exact-name comparison | drop only after all three evidence gates pass |

Every wave must be idempotent, preserve source IDs where safe, reconcile mapped rows, create explicit constraints/indexes, and report net table delta. No `DROP CASCADE`, live database access, or application changes are allowed in this phase.
