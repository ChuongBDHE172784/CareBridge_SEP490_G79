package db.callback;

import java.sql.SQLException;
import java.util.Set;
import org.flywaydb.core.api.CoreErrorCode;
import org.flywaydb.core.api.ErrorDetails;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.exception.FlywayBlockStatementExecutionException;

/**
 * Keeps immutable canonical cleanup migrations compatible with PostgreSQL 18.
 *
 * <p>PostgreSQL 18 exposes NOT NULL constraints through {@code pg_constraint}. Cleanup migrations
 * authored against older PostgreSQL versions either include those new rows in catalog signatures
 * or try to drop them explicitly before dropping legacy tables. This callback executes an
 * equivalent statement that ignores {@code contype = 'n'} and blocks only the original statement.
 * Existing migration checksums and the canonical target schema remain unchanged.
 */
public final class Postgresql18CanonicalCleanupCallback implements Callback {

    private static final Set<String> TARGET_MIGRATION_VERSIONS = Set.of(
            "20260722020400",
            "20260722020500",
            "20260722020800",
            "20260722020900",
            "20260722021000");

    private static final String ROLE_CONSTRAINT_FILTER =
            "WHERE conrelid IN ('public.user_roles'::regclass, 'public.roles'::regclass)";
    private static final String ROLE_CONSTRAINT_ORDER =
            "ORDER BY CASE WHEN conrelid = 'public.user_roles'::regclass THEN 0 ELSE 1 END";
    private static final String TRIAGE_CONSTRAINT_ORDER =
            "ORDER BY CASE relation.relname WHEN 'triage_answers' THEN 0 ELSE 1 END,";
    private static final String CATALOG_SIGNATURE_CONSTRAINTS =
            "FROM pg_constraint WHERE conrelid = candidate_oid)";

    @Override
    public boolean supports(Event event, Context context) {
        if (event != Event.BEFORE_EACH_MIGRATE_STATEMENT
                || context.getMigrationInfo() == null
                || context.getMigrationInfo().getVersion() == null
                || !TARGET_MIGRATION_VERSIONS.contains(
                        context.getMigrationInfo().getVersion().toString())
                || context.getStatement() == null
                || patchForPostgresql18(context.getStatement().getSql()) == null) {
            return false;
        }

        try {
            return context.getConnection().getMetaData().getDatabaseMajorVersion() >= 18;
        } catch (SQLException exception) {
            throw new FlywayException("Unable to detect PostgreSQL version", exception);
        }
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        String compatibleSql = patchForPostgresql18(context.getStatement().getSql());
        if (compatibleSql == null) {
            throw new FlywayException("PostgreSQL 18 cleanup callback received an unsupported statement");
        }

        try (java.sql.Statement statement = context.getConnection().createStatement()) {
            statement.execute(compatibleSql);
        } catch (SQLException exception) {
            throw new FlywayException("PostgreSQL 18 canonical cleanup failed", exception);
        }

        throw new FlywayBlockStatementExecutionException(
                new ErrorDetails(
                        CoreErrorCode.ERROR,
                        "Replaced canonical cleanup with PostgreSQL 18-compatible SQL"),
                context.getStatement().getSql());
    }

    @Override
    public String getCallbackName() {
        return "Postgresql18CanonicalCleanup";
    }

    static String patchForPostgresql18(String sql) {
        String patched = sql;

        if (patched.contains(ROLE_CONSTRAINT_FILTER)
                && patched.contains(ROLE_CONSTRAINT_ORDER)
                && patched.contains("DROP CONSTRAINT %I")) {
            patched = patched.replace(
                    ROLE_CONSTRAINT_ORDER,
                    "AND contype <> 'n'\n         " + ROLE_CONSTRAINT_ORDER);
        }

        if (patched.contains("FROM pg_constraint constraint_definition")
                && patched.contains(TRIAGE_CONSTRAINT_ORDER)
                && patched.contains("DROP CONSTRAINT %I")) {
            patched = patched.replace(
                    TRIAGE_CONSTRAINT_ORDER,
                    "AND constraint_definition.contype <> 'n'\n         "
                            + TRIAGE_CONSTRAINT_ORDER);
        }

        patched = patched.replace(
                CATALOG_SIGNATURE_CONSTRAINTS,
                "FROM pg_constraint WHERE conrelid = candidate_oid AND contype <> 'n')");

        return patched.equals(sql) ? null : patched;
    }
}
