package com.carebridge.backend.systemconfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.systemconfiguration.dto.request.UpdateSystemConfigurationRequest;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import com.carebridge.backend.systemconfiguration.service.impl.SystemConfigurationServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class SystemConfigurationServiceImplTest {

    private static final UUID CONFIGURATION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Mock
    private SystemConfigurationRepository repository;

    @Mock
    private AuditService auditService;

    @Mock
    private SystemMaintenanceModeService maintenanceModeService;

    @InjectMocks
    private SystemConfigurationServiceImpl service;

    @Test
    void get_existingConfiguration_returnsVersionAndMetadata() {
        SystemConfiguration configuration = configuration(3L);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));

        SystemConfigurationResponse response = service.get(ACTOR_ID);

        assertThat(response.id()).isEqualTo(CONFIGURATION_ID);
        assertThat(response.rowVersion()).isEqualTo(3L);
        assertThat(response.updatedBy()).isEqualTo(ACTOR_ID);
    }

    @Test
    void update_matchingVersion_persistsOperationalFlagsAndAudits() {
        SystemConfiguration configuration = configuration(4L);
        UpdateSystemConfigurationRequest request = request(4L, false, true);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));
        when(repository.saveAndFlush(any(SystemConfiguration.class))).thenAnswer(invocation -> {
            SystemConfiguration saved = invocation.getArgument(0);
            saved.setRowVersion(5L);
            return saved;
        });

        SystemConfigurationResponse response = service.update(request, ACTOR_ID);

        assertThat(response.aiModerationEnabled()).isFalse();
        assertThat(response.maintenanceModeEnabled()).isTrue();
        assertThat(response.rowVersion()).isEqualTo(5L);
        verify(repository).saveAndFlush(configuration);
        verify(maintenanceModeService).updateFrom(true);
        verify(auditService).log(
                eq(AuditAction.SYSTEM_CONFIGURATION_UPDATED),
                eq(ACTOR_ID),
                eq("SYSTEM_CONFIGURATION"),
                eq(CONFIGURATION_ID.toString()),
                eq(request));
    }

    @Test
    void update_activeTransaction_publishesMaintenanceCacheOnlyAfterCommit() {
        SystemConfiguration configuration = configuration(4L);
        UpdateSystemConfigurationRequest request = request(4L, false, true);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));
        when(repository.saveAndFlush(any(SystemConfiguration.class))).thenReturn(configuration);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.update(request, ACTOR_ID);

            verify(maintenanceModeService, never()).updateFrom(any(Boolean.class));
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(maintenanceModeService).updateFrom(true);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void update_staleVersion_returnsConflictWithoutPersistenceOrAudit() {
        SystemConfiguration configuration = configuration(5L);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));

        assertThatThrownBy(() -> service.update(request(4L, true, false), ACTOR_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo("SYS_CONFIG_CONFLICT");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                });

        verify(repository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    @Test
    void update_nullVersion_returnsConflictWithoutPersistence() {
        SystemConfiguration configuration = configuration(0L);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));

        assertThatThrownBy(() -> service.update(
                new UpdateSystemConfigurationRequest(true, false, null), ACTOR_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("SYS_CONFIG_CONFLICT"));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_repositoryOptimisticLockFailure_returnsConflictWithoutAuditOrCachePublication() {
        SystemConfiguration configuration = configuration(5L);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));
        when(repository.saveAndFlush(any(SystemConfiguration.class)))
                .thenThrow(new OptimisticLockingFailureException("stale update"));

        assertThatThrownBy(() -> service.update(request(5L, false, true), ACTOR_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo("SYS_CONFIG_CONFLICT");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                });

        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
        verify(maintenanceModeService, never()).updateFrom(any(Boolean.class));
    }

    private SystemConfiguration configuration(long rowVersion) {
        return SystemConfiguration.builder()
                .id(CONFIGURATION_ID)
                .apiRateLimit(5000)
                .connectionTimeoutMs(30000)
                .maxUploadSizeMb(25)
                .administratorEmail("admin@carebridge.dev")
                .emailAlerts(true)
                .smsAlerts(true)
                .webhookAlerts(false)
                .aiModerationEnabled(true)
                .maintenanceModeEnabled(false)
                .updatedBy(ACTOR_ID)
                .rowVersion(rowVersion)
                .createdAt(Instant.parse("2026-07-28T01:00:00Z"))
                .updatedAt(Instant.parse("2026-07-28T02:00:00Z"))
                .build();
    }

    private UpdateSystemConfigurationRequest request(
            long rowVersion, boolean aiModerationEnabled, boolean maintenanceModeEnabled) {
        return new UpdateSystemConfigurationRequest(aiModerationEnabled, maintenanceModeEnabled, rowVersion);
    }
}
