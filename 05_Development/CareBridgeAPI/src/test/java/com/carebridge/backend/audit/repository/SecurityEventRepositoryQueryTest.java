package com.carebridge.backend.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.SecurityEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class SecurityEventRepositoryQueryTest {

    @Mock private JdbcTemplate jdbcTemplate;
    private SecurityEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SecurityEventRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusFilteringAndCountUseTheLatestReviewInOneQuery() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        Page<SecurityEvent> result = repository.search(
                null, null, null, "CLOSED", null, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(countSql.capture(), eq(Long.class), any(Object[].class));
        assertThat(countSql.getValue())
                .contains("LEFT JOIN LATERAL")
                .contains("coalesce(latest.status, base.status) = ?");

        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(pageSql.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(pageSql.getValue())
                .contains("coalesce(latest.status, base.status) AS effective_status")
                .contains("ORDER BY base.occurred_at DESC, base.audit_event_id DESC");
        assertThat(result.getTotalElements()).isZero();
    }
}
