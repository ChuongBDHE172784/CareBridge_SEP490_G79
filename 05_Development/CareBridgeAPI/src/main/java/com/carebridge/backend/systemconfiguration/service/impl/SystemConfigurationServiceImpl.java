package com.carebridge.backend.systemconfiguration.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.systemconfiguration.dto.request.UpdateSystemConfigurationRequest;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import com.carebridge.backend.systemconfiguration.service.SystemConfigurationService;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SystemConfigurationServiceImpl implements SystemConfigurationService {
    private final SystemConfigurationRepository repository;
    private final AuditService auditService;
    private final SystemMaintenanceModeService maintenanceModeService;

    @Override
    @Transactional
    public SystemConfigurationResponse get(UUID actorId) {
        return toResponse(getOrCreateDefault(actorId));
    }

    @Override
    @Transactional
    public SystemConfigurationResponse update(UpdateSystemConfigurationRequest request, UUID actorId) {
        SystemConfiguration configuration = getOrCreateDefault(actorId);
        if (request.rowVersion() == null
                || configuration.getRowVersion() != request.rowVersion().longValue()) {
            throw configurationConflict();
        }
        configuration.setAiModerationEnabled(request.aiModerationEnabled());
        configuration.setMaintenanceModeEnabled(request.maintenanceModeEnabled());
        configuration.setUpdatedBy(actorId);
        SystemConfiguration saved;
        try {
            saved = repository.saveAndFlush(configuration);
        } catch (OptimisticLockingFailureException exception) {
            throw configurationConflict();
        }
        auditService.log(
                AuditAction.SYSTEM_CONFIGURATION_UPDATED,
                actorId,
                "SYSTEM_CONFIGURATION",
                saved.getId().toString(),
                request);
        publishMaintenanceModeAfterCommit(saved.isMaintenanceModeEnabled());
        return toResponse(saved);
    }

    private void publishMaintenanceModeAfterCommit(boolean enabled) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            maintenanceModeService.updateFrom(enabled);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                maintenanceModeService.updateFrom(enabled);
            }
        });
    }

    private SystemConfiguration getOrCreateDefault(UUID actorId) {
        return repository.findFirstByOrderByCreatedAtAsc().orElseGet(() -> repository.save(SystemConfiguration.builder()
                .apiRateLimit(5000).connectionTimeoutMs(30000).maxUploadSizeMb(25)
                .administratorEmail("admin@carebridge.dev").emailAlerts(true).smsAlerts(true)
                .webhookAlerts(false).aiModerationEnabled(true).maintenanceModeEnabled(false)
                .updatedBy(actorId).build()));
    }

    private BusinessException configurationConflict() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "SYS_CONFIG_CONFLICT",
                "System configuration was updated by another administrator. Reload and try again.");
    }

    private SystemConfigurationResponse toResponse(SystemConfiguration value) {
        return new SystemConfigurationResponse(value.getId(), value.isAiModerationEnabled(),
                value.isMaintenanceModeEnabled(), value.getRowVersion(), value.getUpdatedBy(), value.getUpdatedAt());
    }
}
