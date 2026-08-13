package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.health.event.EpdsScreeningCompleted;
import com.carebridge.backend.health.policy.EpdsSeverityPolicy;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers EPDS screening results to consented Family members of the mother's care group
 * (CB-EPDS-IMP-001).
 *
 * <p>Runs after commit and off the request thread, so a notification failure can never roll back
 * or slow down the mother's screening submission (TDS ADR-001).
 *
 * <p>Message content is produced entirely by {@link EpdsSeverityPolicy}; this class never renders
 * the Question-10 score itself (TDS INV-2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpdsFamilyNotificationService {

    private final NotificationService notificationService;
    private final CareGroupMemberRepository memberRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEpdsScreeningCompleted(EpdsScreeningCompleted event) {
        try {
            List<CareGroupMember> consented = consentedRecipients(event.motherUserId());
            if (consented.isEmpty()) {
                log.debug("No consented family recipients for EPDS screening of mother {}",
                        event.motherUserId());
                return;
            }

            String title = EpdsSeverityPolicy.familyTitle(event.totalScore(), event.question10Score());
            String body = EpdsSeverityPolicy.familyBody(event.totalScore(), event.question10Score());

            for (CareGroupMember member : consented) {
                dispatch(member, title, body);
            }

            // Log counts and identifiers only — never the score or Question 10 (NFR-EPDS-N-05).
            log.info("EPDS family notification dispatched careGroupId={} recipients={} band={}",
                    consented.get(0).getCareGroupId(), consented.size(),
                    EpdsSeverityPolicy.band(event.totalScore()));

        } catch (Exception e) {
            log.error("Failed to send EPDS family notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves eligible recipients, applying the consent gate <strong>before</strong>
     * de-duplication.
     *
     * <p>Order matters: a Family user may belong to several of the mother's ACTIVE groups and hold
     * {@code QUICK_NOTE_EPDS} in only some of them. De-duplicating first could retain a
     * non-consented row and drop the consented one, silently suppressing a notification the mother
     * did authorise (TDS §5.2). Filtering first and collapsing the survivors keeps the first
     * consented group — deterministic, because the query orders by {@code care_group_id ASC}.
     */
    private List<CareGroupMember> consentedRecipients(UUID motherUserId) {
        List<CareGroupMember> candidates =
                memberRepository.findAcceptedFamilyMembersForEpdsAlerts(motherUserId);

        List<CareGroupMember> consented = new ArrayList<>();
        Set<UUID> seenUserIds = new LinkedHashSet<>();

        for (CareGroupMember member : candidates) {
            UUID userId = member.getUserId();
            if (userId == null || userId.equals(motherUserId)) {
                // The mother is the subject of her own screening and is never a recipient.
                continue;
            }
            if (!hasEpdsConsent(member.getCareGroupId(), userId)) {
                continue;
            }
            if (seenUserIds.add(userId)) {
                consented.add(member);
            }
        }
        return consented;
    }

    /** Reuses the same two-flag gate the family dashboard and quick-note history apply. */
    private boolean hasEpdsConsent(UUID groupId, UUID userId) {
        return authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTES)
                && authorizationPolicy.hasPermission(groupId, userId, PermissionFlag.QUICK_NOTE_EPDS);
    }

    /**
     * Sends to a single recipient, absorbing per-recipient failures.
     *
     * <p>The catch sits inside the loop so one recipient's FCM failure cannot drop every later
     * recipient (TDS §6.2).
     */
    private void dispatch(CareGroupMember member, String title, String body) {
        try {
            notificationService.send(new SendNotificationRequest(
                    member.getUserId(),
                    NotificationType.EPDS_RESULT,
                    title,
                    body,
                    member.getCareGroupId(),
                    "CARE_GROUP"));
        } catch (Exception e) {
            log.error("Failed to send EPDS notification to user {}: {}",
                    member.getUserId(), e.getMessage(), e);
        }
    }
}
