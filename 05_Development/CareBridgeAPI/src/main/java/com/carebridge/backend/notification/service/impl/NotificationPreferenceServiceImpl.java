package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.NotificationPreferenceItemDto;
import com.carebridge.backend.notification.dto.NotificationPreferencesResponse;
import com.carebridge.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.carebridge.backend.notification.entity.NotificationPreference;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC-10: Manages notification preferences using the V1 notification_preferences table.
 *
 * <p>The table stores one row per (user_id, notification_type) with separate
 * boolean flags for each delivery channel (push_enabled, email_enabled, in_app_enabled).
 *
 * <p>Constraints enforced:
 * <ul>
 *   <li>C1: userId always taken from JWT — never from request body</li>
 *   <li>C2: Upsert — idempotent (no duplicates per user+type)</li>
 *   <li>C6: AuditService.log called within the same @Transactional boundary</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final AuditService auditService;

    // -------------------------------------------------------------------------
    // UC-10: Get preferences
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(UUID userId) {
        log.debug("Getting notification preferences for userId={}", userId);

        List<NotificationPreference> prefs = preferenceRepository.findByUserId(userId);

        auditService.log(
                AuditAction.NOTIFICATION_PREFERENCES_VIEWED,
                userId,
                "NotificationPreference",
                userId.toString(),
                Map.of("count", prefs.size()));

        return toResponse(userId, prefs);
    }

    // -------------------------------------------------------------------------
    // UC-10: Update preferences (upsert)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public NotificationPreferencesResponse updatePreferences(
            UUID userId, UpdateNotificationPreferencesRequest request) {

        log.debug("Updating notification preferences for userId={}, items={}",
                userId, request.preferences().size());

        for (NotificationPreferenceItemDto item : request.preferences()) {
            // Upsert: find existing row or create a new one
            NotificationPreference pref = preferenceRepository
                    .findByUserIdAndNotificationType(userId, item.notificationType())
                    .orElseGet(() -> NotificationPreference.builder()
                            .userId(userId)
                            .notificationType(item.notificationType())
                            .build());

            if (item.pushEnabled() != null) {
                pref.setPushEnabled(item.pushEnabled());
            }
            if (item.emailEnabled() != null) {
                pref.setEmailEnabled(item.emailEnabled());
            }
            if (item.inAppEnabled() != null) {
                pref.setInAppEnabled(item.inAppEnabled());
            }
            pref.setUpdatedAt(Instant.now());

            preferenceRepository.save(pref);
        }

        // Reload to return the full current state
        List<NotificationPreference> updated = preferenceRepository.findByUserId(userId);

        // C6: audit within same @Transactional
        auditService.log(
                AuditAction.NOTIFICATION_PREFERENCES_UPDATED,
                userId,
                "NotificationPreference",
                userId.toString(),
                Map.of("updatedTypes", request.preferences().stream()
                        .map(i -> i.notificationType().name())
                        .toList()));

        return toResponse(userId, updated);
    }

    // -------------------------------------------------------------------------
    // UC-158/159/160/161: Preference gate
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean isPushEnabled(UUID userId, NotificationType type) {
        return preferenceRepository.findByUserIdAndNotificationType(userId, type)
                .map(pref -> Boolean.TRUE.equals(pref.getPushEnabled()))
                .orElse(true); // default = enabled when no row exists
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private NotificationPreferencesResponse toResponse(UUID userId, List<NotificationPreference> prefs) {
        List<NotificationPreferenceItemDto> items = prefs.stream()
                .map(p -> new NotificationPreferenceItemDto(
                        p.getNotificationType(),
                        p.getPushEnabled(),
                        p.getEmailEnabled(),
                        p.getInAppEnabled()))
                .toList();
        return new NotificationPreferencesResponse(userId, items);
    }
}
