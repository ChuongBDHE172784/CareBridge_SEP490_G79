package com.carebridge.backend.systemconfiguration.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.systemconfiguration.dto.request.UpdateSystemConfigurationRequest;
import com.carebridge.backend.systemconfiguration.dto.response.SystemConfigurationResponse;
import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import com.carebridge.backend.systemconfiguration.service.SystemConfigurationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemConfigurationServiceImpl implements SystemConfigurationService {
    private final SystemConfigurationRepository repository;
    private final AuditService auditService;

    @Override
    @Transactional
    public SystemConfigurationResponse get(UUID actorId) {
        return toResponse(getOrCreateDefault(actorId));
    }

    @Override
    @Transactional
    public SystemConfigurationResponse update(UpdateSystemConfigurationRequest request, UUID actorId) {
        SystemConfiguration configuration = getOrCreateDefault(actorId);
        configuration.setApiRateLimit(request.apiRateLimit());
        configuration.setConnectionTimeoutMs(request.connectionTimeoutMs());
        configuration.setMaxUploadSizeMb(request.maxUploadSizeMb());
        configuration.setAdministratorEmail(request.administratorEmail());
        configuration.setEmailAlerts(request.emailAlerts());
        configuration.setSmsAlerts(request.smsAlerts());
        configuration.setWebhookAlerts(request.webhookAlerts());
        configuration.setAiModerationEnabled(request.aiModerationEnabled());
        configuration.setMaintenanceModeEnabled(request.maintenanceModeEnabled());
        configuration.setUpdatedBy(actorId);
        SystemConfiguration saved = repository.save(configuration);
        auditService.log(AuditAction.SECURITY_EVENT, actorId, "SYSTEM_CONFIGURATION", saved.getId().toString(), request);
        return toResponse(saved);
    }

    private SystemConfiguration getOrCreateDefault(UUID actorId) {
        return repository.findFirstByOrderByCreatedAtAsc().orElseGet(() -> repository.save(SystemConfiguration.builder()
                .apiRateLimit(5000).connectionTimeoutMs(30000).maxUploadSizeMb(25)
                .administratorEmail("admin@carebridge.dev").emailAlerts(true).smsAlerts(true)
                .webhookAlerts(false).aiModerationEnabled(true).maintenanceModeEnabled(false)
                .updatedBy(actorId).build()));
    }

    private SystemConfigurationResponse toResponse(SystemConfiguration value) {
        return new SystemConfigurationResponse(value.getId(), value.getApiRateLimit(), value.getConnectionTimeoutMs(),
                value.getMaxUploadSizeMb(), value.getAdministratorEmail(), value.isEmailAlerts(), value.isSmsAlerts(),
                value.isWebhookAlerts(), value.isAiModerationEnabled(), value.isMaintenanceModeEnabled(),
                value.getUpdatedBy(), value.getUpdatedAt());
    }
}
