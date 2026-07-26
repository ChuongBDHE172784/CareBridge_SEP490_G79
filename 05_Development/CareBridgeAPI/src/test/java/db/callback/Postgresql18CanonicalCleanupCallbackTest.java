package db.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.callback.Statement;
import org.flywaydb.core.api.exception.FlywayBlockStatementExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class Postgresql18CanonicalCleanupCallbackTest {

    private final Postgresql18CanonicalCleanupCallback callback =
            new Postgresql18CanonicalCleanupCallback();
    private final Context context = mock(Context.class);
    private final MigrationInfo migrationInfo = mock(MigrationInfo.class);
    private final Statement flywayStatement = mock(Statement.class);
    private final Connection connection = mock(Connection.class);
    private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);

    @BeforeEach
    void setUp() throws Exception {
        when(context.getMigrationInfo()).thenReturn(migrationInfo);
        when(context.getStatement()).thenReturn(flywayStatement);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
    }

    @Test
    void supportsOnlyPatchedCleanupStatementsOnPostgresql18OrNewer() throws Exception {
        useMigration("20260722020400");
        when(flywayStatement.getSql()).thenReturn(roleCleanupSql());
        when(metadata.getDatabaseMajorVersion()).thenReturn(18);

        assertThat(callback.supports(Event.BEFORE_EACH_MIGRATE_STATEMENT, context)).isTrue();

        when(metadata.getDatabaseMajorVersion()).thenReturn(17);
        assertThat(callback.supports(Event.BEFORE_EACH_MIGRATE_STATEMENT, context)).isFalse();
    }

    @Test
    void excludesPostgresql18NotNullRowsFromConstraintCleanup() throws Exception {
        useMigration("20260722020400");
        when(flywayStatement.getSql()).thenReturn(roleCleanupSql());
        java.sql.Statement jdbcStatement = mock(java.sql.Statement.class);
        when(connection.createStatement()).thenReturn(jdbcStatement);

        assertThatThrownBy(() -> callback.handle(Event.BEFORE_EACH_MIGRATE_STATEMENT, context))
                .isInstanceOf(FlywayBlockStatementExecutionException.class);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcStatement).execute(sql.capture());
        assertThat(sql.getValue()).contains("AND contype <> 'n'");
        verify(jdbcStatement).close();
    }

    @Test
    void excludesPostgresql18NotNullRowsFromLegacyCatalogSignatures() {
        String original = "SELECT string_agg(conname, ',') FROM pg_constraint "
                + "WHERE conrelid = candidate_oid)";

        assertThat(Postgresql18CanonicalCleanupCallback.patchForPostgresql18(original))
                .contains("WHERE conrelid = candidate_oid AND contype <> 'n'");
    }

    @Test
    void excludesPostgresql18NotNullRowsFromTriageConstraintCleanup() {
        String original = """
                FROM pg_constraint constraint_definition
                WHERE constraint_definition.conrelid IN ('public.triage_answers'::regclass)
                ORDER BY CASE relation.relname WHEN 'triage_answers' THEN 0 ELSE 1 END,
                constraint_definition.conname
                LOOP
                  EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I', 'x', 'y');
                """;

        assertThat(Postgresql18CanonicalCleanupCallback.patchForPostgresql18(original))
                .contains("AND constraint_definition.contype <> 'n'");
    }

    private void useMigration(String version) {
        when(migrationInfo.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
    }

    private static String roleCleanupSql() {
        return """
                FROM pg_constraint
                WHERE conrelid IN ('public.user_roles'::regclass, 'public.roles'::regclass)
                ORDER BY CASE WHEN conrelid = 'public.user_roles'::regclass THEN 0 ELSE 1 END
                LOOP
                  EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', 'x', 'y');
                """;
    }
}
