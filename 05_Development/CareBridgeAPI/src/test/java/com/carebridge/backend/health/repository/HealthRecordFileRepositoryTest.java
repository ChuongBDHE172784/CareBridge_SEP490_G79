package com.carebridge.backend.health.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.entity.HealthRecordFile;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthRecordFileRepositoryTest {
    @Test
    void attachmentCannotBeStolenFromAnotherRecord() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(), any(), any())).thenReturn(0);
        when(jdbc.queryForObject(any(String.class), any(Class.class), any()))
                .thenReturn(Boolean.TRUE);
        var repository = new HealthRecordFileRepository(jdbc);

        assertThatThrownBy(() -> repository.save(HealthRecordFile.builder()
                .healthRecordId(UUID.randomUUID())
                .fileId(UUID.randomUUID())
                .displayOrder(7)
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another health record");
        verify(jdbc).update(contains("health_record_id IS NULL"), any(), any(), any());
    }
}
