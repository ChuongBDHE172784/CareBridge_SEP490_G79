package com.carebridge.backend.checklist.operations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class ChecklistRetentionOwnerIsolationVerifierTest {

    @Test
    void startupFailsClosedWhileRetentionOwnerHasAnyMember() {
        JdbcTemplate dedicatedTemplate = mock(JdbcTemplate.class);
        when(dedicatedTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        ChecklistRetentionOwnerIsolationVerifier verifier =
                new ChecklistRetentionOwnerIsolationVerifier(dedicatedTemplate);

        assertThatThrownBy(() -> verifier.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHECKLIST_RETENTION_OWNER_ROLE_REACHABLE");
    }

    @Test
    void startupContinuesOnlyAfterPrivilegedMembershipCleanup() {
        JdbcTemplate dedicatedTemplate = mock(JdbcTemplate.class);
        when(dedicatedTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        ChecklistRetentionOwnerIsolationVerifier verifier =
                new ChecklistRetentionOwnerIsolationVerifier(dedicatedTemplate);

        assertThatCode(() -> verifier.run(mock(ApplicationArguments.class)))
                .doesNotThrowAnyException();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(dedicatedTemplate).queryForObject(sql.capture(), eq(Boolean.class));
        assertThatCode(() -> {
            org.assertj.core.api.Assertions.assertThat(sql.getValue())
                    .contains(
                            "session_user = 'checklist_operations'",
                            "current_user = 'checklist_operations'",
                            "purge_function.prosecdef = true",
                            "purge_function.proconfig =",
                            "carebridge_checklist_schema_owner",
                            "pg_database_owner",
                            "to_regclass('public.checklist_migration_quarantine') IS NULL",
                            "CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1",
                            "DELETE FROM public.checklist_migration_quarantine",
                            "trigger_entry.tgrelid = expected.table_oid",
                            "trigger_entry.tgfoid = expected.function_oid",
                            "trigger_entry.tgtype = expected.trigger_type",
                            "trigger_entry.tgqual IS NULL",
                            "expected_table_acl",
                            "trigger_entry.tgenabled = 'O'",
                            "has_function_privilege(");
        }).doesNotThrowAnyException();
    }
}
