-- =============================================================================
-- supabase-preflight.sql — CareBridge Shared-Dev Supabase Pre-Reset Preflight
-- Purpose : READ-ONLY diagnostic. Run before supabase-reset-guarded.sql.
--           Produces a report that a human must review before approving reset.
-- Safety  : Contains NO DROP / TRUNCATE / DELETE / ALTER TABLE statements.
--           Safe to run at any time without data risk.
-- =============================================================================

-- 1. Database identity
SELECT current_database()    AS "db_name",
       current_user          AS "connected_as",
       version()             AS "pg_version",
       now()                 AS "report_generated_at";

-- 2. Flyway migration history (what the DB thinks has been applied)
SELECT version,
       description,
       type,
       installed_on,
       success,
       checksum
FROM   flyway_schema_history
ORDER  BY installed_rank;

-- 3. Table inventory: counts all tables in public schema
SELECT tablename,
       pg_size_pretty(pg_total_relation_size(quote_ident(tablename))) AS "size"
FROM   pg_tables
WHERE  schemaname = 'public'
ORDER  BY tablename;

-- 4. Row counts for core tables (spot data before wipe decision)
SELECT 'users'           AS "table", COUNT(*) AS "rows" FROM users
UNION ALL
SELECT 'refresh_tokens',                COUNT(*) FROM refresh_tokens
UNION ALL
SELECT 'otp_verifications',             COUNT(*) FROM otp_verifications
UNION ALL
SELECT 'consent_grants',                COUNT(*) FROM consent_grants
UNION ALL
SELECT 'audit_logs',                    COUNT(*) FROM audit_logs
UNION ALL
SELECT 'community_topics',              COUNT(*) FROM community_topics
UNION ALL
SELECT 'community_questions',           COUNT(*) FROM community_questions
UNION ALL
SELECT 'community_answers',             COUNT(*) FROM community_answers
UNION ALL
SELECT 'content_items',                 COUNT(*) FROM content_items
UNION ALL
SELECT 'partner_organizations',         COUNT(*) FROM partner_organizations
ORDER  BY 1;

-- 5. Legacy V2-V12 history detection
--    If this returns rows the DB is in pre-Phase-C state and MUST be reset.
SELECT 'LEGACY_MIGRATION_DETECTED' AS "status",
       version,
       description,
       installed_on
FROM   flyway_schema_history
WHERE  version IN ('2','3','4','5','6','7','8','9','10','11','12')
ORDER  BY version::int;

-- If the above query returns 0 rows, the DB is already in post-Phase-C state.

-- 6. Reset recommendation
SELECT CASE
         WHEN EXISTS (
           SELECT 1 FROM flyway_schema_history
           WHERE version IN ('2','3','4','5','6','7','8','9','10','11','12')
         )
         THEN 'RESET REQUIRED — legacy V2-V12 migrations detected in flyway_schema_history'
         ELSE 'NO RESET NEEDED — DB is already in clean Phase-C state'
       END AS "recommendation";
