# Checklist retention privileged migration runbook

## Scope and principals

Use this runbook for the checklist retention finalizer and for any later DDL
that needs ownership of its protected tables, guard functions, or purge
function. The principals must remain separate:

- the Flyway login applies ordinary versioned migrations;
- the deployment provisioner is a short-lived superuser used only by the
  reviewed privileged runner;
- `carebridge_checklist_schema_owner` and
  `carebridge_checklist_retention_owner` are `NOLOGIN` owners with no role
  memberships;
- `carebridge_application` and `checklist_operations` retain only their
  verified runtime privileges.

Never grant an owner role to Flyway, the application, operations, or a human
login to make a migration pass.

## Step 0 — provision the four roles (once per database, before any Flyway run)

`V20260731070000__canonical_post_20260719180000_schema.sql` asserts all four
principals exist with exact attributes and aborts with `42501` otherwise
(`CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED` and its three siblings). Flyway holds
no `CREATEROLE` privilege by design, so a database that has never been
provisioned cannot complete its first migration.

Run `00_provision_checklist_roles.sql` as a principal holding `CREATEROLE` —
on Supabase that is `postgres`, not the Flyway login:

```
psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -f 00_provision_checklist_roles.sql
```

It is idempotent and normalises an existing role whose attributes drifted; note
that every role requires `NOINHERIT`, which is not PostgreSQL's default, so a
hand-created role usually fails the migration until this script repairs it. The
script finishes with `CHECKLIST_ROLE_PROVISIONING_VERIFIED` and refuses to run
as one of the four principals.

Passwords for the two `LOGIN` roles (`carebridge_application`,
`checklist_operations`) are deliberately absent from the script. Set them from
the deployment secret store afterwards; never place them in source control.

## Apply finalizer V20260729150001

1. Stop application startup and background workers for the target database.
2. Confirm backup/PITR readiness and apply all Flyway migrations for the
   release. On a fresh database, Step 0 must already have run.
3. Review the finalizer and the SHA-256 pinned by
   `Invoke-ChecklistRetentionFinalizer.ps1`. Do not edit an applied version.
4. Supply the `psql` executable and database URL through
   `CAREBRIDGE_PSQL_PATH` and `CAREBRIDGE_RETENTION_PROVISIONER_DB_URL`.
   Inject `CAREBRIDGE_RETENTION_PROVISIONER_DB_PASSWORD` from the deployment
   secret store when password authentication is used; never place a password
   in source control, command arguments, logs, or the URL.
5. Run `Invoke-ChecklistRetentionFinalizer.ps1` from the deployment host.
   Success requires both `CHECKLIST_RETENTION_FINALIZER_COMPLETE` and the
   version/checksum expected by the release manifest.
6. Start the application only after its retention isolation verifier passes.

After commit, the runner first attests the full verifier's pinned canonical
definition, owner attributes, execution attributes, search path, and exact
owner/operations ACL. Only then does it invoke the full catalog verifier and
require the exact versioned `VERIFIED` output. A failed attestation, catalog
drift, unexpected output, or query error prevents the `COMPLETE` marker.

The runner disables `psqlrc`, refuses password prompts, and does not inherit a
generic `PGPASSWORD`. It rejects option-like database values and passwords in
PostgreSQL URI userinfo, then passes the database value only through the
explicit `--dbname` option. If the dedicated password variable is absent,
libpq must already have a reviewed noninteractive mechanism such as a protected
passfile, client certificate, or integrated authentication; otherwise the run
fails closed.

## Future protected-object migrations

Ordinary DDL that does not require ownership stays in Flyway. A release that
must alter a protected object needs a new, independently reviewed privileged
migration/finalizer version; never change V20260729150001 or weaken its catalog
checks.

The release procedure must:

1. enumerate the exact objects and expected owners before the change;
2. keep the application stopped and use the provisioner only through a
   checksum-pinned, `ON_ERROR_STOP` runner;
3. perform ownership-sensitive DDL and return every object to its designated
   `NOLOGIN` owner in the same transaction whenever possible;
4. if a separate Flyway step makes a temporary handoff unavoidable, use
   versioned pre-handoff and post-handoff scripts, transfer only the named
   objects (never role membership), and orchestrate
   `pre-handoff -> Flyway -> post-handoff` with no application start between;
5. have the post-handoff script normalize and verify exact role attributes,
   memberships, owners, ACLs, `SECURITY DEFINER`/`search_path`, FORCE RLS,
   policies, and enabled guards before commit;
6. pin the new script checksum in its runner and archive the runner output,
   Flyway result, catalog-verifier result, release ID, and operator identity.

On any failure, keep the application stopped. Do not retry with broader grants.
Inspect the actual catalogs, then execute a reviewed recovery or roll-forward
script that restores the designated owners and exact security postconditions.
Escalate to database/security owners before allowing traffic.
