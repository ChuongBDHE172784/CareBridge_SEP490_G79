package com.carebridge.backend.checklist.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ChecklistRetentionPurgeRepositoryTest {

    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void purgeFailsClosedWhenDedicatedOperationsTemplateIsUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcTemplate> dedicatedTemplateProvider = mock(ObjectProvider.class);
        JdbcTemplate sharedApplicationTemplate = mock(JdbcTemplate.class);
        when(dedicatedTemplateProvider.getIfAvailable()).thenReturn(null);

        ChecklistRetentionPurgeRepository repository =
                new ChecklistRetentionPurgeRepository(dedicatedTemplateProvider);

        assertThatThrownBy(() -> repository.purge(ACTOR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHECKLIST_RETENTION_DATASOURCE_NOT_CONFIGURED");
        verifyNoInteractions(sharedApplicationTemplate);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void purgeUsesOnlyTheDedicatedOperationsTemplate() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JdbcTemplate> dedicatedTemplateProvider = mock(ObjectProvider.class);
        JdbcTemplate dedicatedTemplate = mock(JdbcTemplate.class);
        ChecklistRetentionPurgeResult expected = new ChecklistRetentionPurgeResult(3, 1);
        when(dedicatedTemplateProvider.getIfAvailable()).thenReturn(dedicatedTemplate);
        when(dedicatedTemplate.queryForObject(
                        anyString(),
                        any(RowMapper.class),
                        eq(ACTOR_ID)))
                .thenReturn(expected);

        ChecklistRetentionPurgeRepository repository =
                new ChecklistRetentionPurgeRepository(dedicatedTemplateProvider);

        assertThat(repository.purge(ACTOR_ID)).isEqualTo(expected);
        verify(dedicatedTemplate).queryForObject(
                anyString(),
                any(RowMapper.class),
                eq(ACTOR_ID));
    }
}
