package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlTopLevelStatementParserTest {

    @Test
    void parsesNestedCommentsDollarBodiesAndEscapedStringsWithoutFalseBoundaries() {
        var statements = SqlTopLevelStatementParser.parse("""
                /* outer comment ; INSERT INTO users VALUES (1);
                   /* nested comment ; TRUNCATE users; */
                */
                DO $body$
                BEGIN
                    PERFORM 'semi;''colon';
                END
                $body$;
                SELECT 'it''s still one; statement' AS "escaped""identifier";
                """);

        assertThat(statements)
                .extracting(SqlTopLevelStatementParser.SqlStatement::type)
                .containsExactly("DO", "SELECT");
        assertThat(statements.getFirst().dollarQuotedBody())
                .contains("PERFORM 'semi;''colon';");
    }

    @Test
    void detectsTopLevelTruncateAndMutationBearingDynamicExecute() {
        assertThat(SqlTopLevelStatementParser.containsExecutableDataMutation(
                "TRUNCATE TABLE public.users")).isTrue();
        assertThat(SqlTopLevelStatementParser.containsExecutableDataMutation("""
                BEGIN
                    EXECUTE 'DELETE FROM public.users WHERE user_id = $1';
                END
                """)).isTrue();
        assertThat(SqlTopLevelStatementParser.containsExecutableDataMutation("""
                BEGIN
                    EXECUTE format('UPDATE public.%I SET enabled = false', table_name);
                END
                """)).isTrue();
        assertThat(SqlTopLevelStatementParser.containsExecutableDataMutation("""
                BEGIN
                    EXECUTE $sql$TRUNCATE TABLE public.users$sql$;
                END
                """)).isTrue();
    }

    @Test
    void permitsCurrentDynamicRoleAndAclDdl() {
        assertThat(SqlTopLevelStatementParser.containsExecutableDataMutation("""
                BEGIN
                    EXECUTE format(
                        'GRANT carebridge_checklist_schema_owner TO %I', current_user);
                    EXECUTE 'GRANT CREATE ON SCHEMA public '
                        'TO carebridge_checklist_retention_owner';
                    EXECUTE 'REVOKE INSERT, DELETE ON TABLE '
                        'public.checklist_migration_quarantine FROM carebridge_application';
                    EXECUTE format(
                        'REVOKE carebridge_checklist_retention_owner FROM %I', current_user);
                END
                """)).isFalse();
    }
}
