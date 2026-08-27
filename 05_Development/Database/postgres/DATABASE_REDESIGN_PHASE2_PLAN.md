# DATABASE REDESIGN PHASE 2 — MIGRATION PLAN

Approved baseline: 70 core tables (69 business/application + `flyway_schema_history`) plus 3 approved Release-1 extensions: `expert_consultation_requests`, `consultation_context_shares`, and `consultation_context_citations`. The effective deployed inventory is 73 base tables (72 business/application + 1 technical), expressed in gates as **70 core + 3 extensions**.
Application code remains unchanged until the database validation gate passes.

## Branch-history rollout contract

The merged migration chain must be executed by exactly one Flyway migration
leader while application writes are quiesced. Do not start another application
instance until the leader has completed the full chain and the canonical
inventory and Hibernate validation gates have passed. Take the normal database
backup/snapshot first and retain it until post-deployment verification completes.

The branch-history bridges are intentionally forward-only and each version is
transactional, but the whole multi-version sequence is not one transaction. If
execution stops after a capture migration, keep the bridge schema and rerun the
same immutable chain; the next migration verifies and restores the captured
values. Never edit or delete `flyway_schema_history`, run `flyway repair`, skip a
pending version, or manually drop a bridge relation to make startup continue.
Any checksum mismatch, unexpected source count, unapproved cleanup reason, or
non-empty synthetic shadow is a release stop that requires evidence review.

| Wave | Scope | Canonical work | Drop policy |
|---|---|---|---|
| 1 | account/person/auth | persons, care_subjects, users, identities, sessions, revocations, challenges, deletion requests | retain legacy auth/profile tables |
| 2 | mother/baby | journeys, lifecycle events, maternal observations/exercise, care logs, growth, milestones, vaccination | retain legacy mother/baby sources |
| 3 | community/expert | public community content/interactions and professional identity/credential lifecycle | retain source tables |
| 4 | triage/knowledge | triage sessions/evidence, health context, knowledge sources/reviews | retain intake/triage sources; admin medical warning catalog retired |
| 5 | health/files/devices | health records, attachments, record links, device connections/observations | retain file/device sources |
| 6 | family/care plan | care groups/members, scheduled items, tasks, checklist items/templates | retain reminder/task sources |
| 7 | content/moderation | verified content links, moderation cases and append-only events | retain report/action sources |
| 8 | safety/facility | safety config/session/events/actions, contacts, administrative areas/facilities/nearby support | retain safety/facility sources |
| 9 | audit/security/consent/archive | audit/security/permissions/config, expenses and domain archives | retain audit/security sources |
| 10 | final pending-drop cleanup | evidence scans and exact-name comparison | drop only after all three evidence gates pass |

Every wave must be idempotent, preserve source IDs where safe, reconcile mapped rows, create explicit constraints/indexes, and report net table delta. No `DROP CASCADE`, live database access, or application changes are allowed in this phase.
