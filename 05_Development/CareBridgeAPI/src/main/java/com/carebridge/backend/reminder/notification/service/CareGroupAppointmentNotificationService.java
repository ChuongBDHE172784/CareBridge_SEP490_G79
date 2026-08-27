package com.carebridge.backend.reminder.notification.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fans out appointment lifecycle and milestone notifications to accepted
 * members that have the CALENDAR permission. No appointment copies are made.
 */
@Service
@Slf4j
public class CareGroupAppointmentNotificationService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final MotherJourneyRepository journeyRepository;
    private final BabyProfileRepository babyRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRepository;
    private final FcmService fcmService;
    private final AuditService auditService;
    private final NotificationJobRepository appointmentJobRepository;

    @Autowired
    public CareGroupAppointmentNotificationService(
            CareGroupRepository groupRepository,
            CareGroupMemberRepository memberRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            NotificationPreferenceRepository preferenceRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository notificationRepository,
            FcmService fcmService,
            AuditService auditService,
            NotificationJobRepository appointmentJobRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.authorizationPolicy = authorizationPolicy;
        this.journeyRepository = journeyRepository;
        this.babyRepository = babyRepository;
        this.preferenceRepository = preferenceRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
        this.fcmService = fcmService;
        this.auditService = auditService;
        this.appointmentJobRepository = appointmentJobRepository;
    }

    /** Compatibility constructor for focused unit tests that do not exercise the job lock. */
    public CareGroupAppointmentNotificationService(
            CareGroupRepository groupRepository,
            CareGroupMemberRepository memberRepository,
            CareGroupAuthorizationPolicy authorizationPolicy,
            MotherJourneyRepository journeyRepository,
            BabyProfileRepository babyRepository,
            NotificationPreferenceRepository preferenceRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository notificationRepository,
            FcmService fcmService,
            AuditService auditService) {
        this(groupRepository, memberRepository, authorizationPolicy, journeyRepository, babyRepository,
                preferenceRepository, deviceTokenRepository, notificationRepository, fcmService, auditService,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCreated(Reminder reminder) {
        notifyEvent(reminder, "CREATED", reminder.getId() + "|CREATED",
                "Lịch hẹn mới trong nhóm", "Mẹ đã thêm lịch hẹn \""
                        + reminder.getTitle() + "\" lúc " + format(reminder, null) + ".");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyUpdated(Reminder reminder) {
        String version = reminder.getUpdatedAt() == null
                ? "current" : reminder.getUpdatedAt().toString();
        notifyEvent(reminder, "UPDATED", reminder.getId() + "|UPDATED|" + version,
                "Lịch hẹn đã thay đổi", "Lịch hẹn \"" + reminder.getTitle()
                        + "\" đã được cập nhật.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCancelled(Reminder reminder) {
        String version = reminder.getUpdatedAt() == null
                ? "current" : reminder.getUpdatedAt().toString();
        notifyEvent(reminder, "CANCELLED", reminder.getId() + "|CANCELLED|" + version,
                "Lịch hẹn đã hủy", "Lịch hẹn \"" + reminder.getTitle()
                        + "\" đã bị hủy.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMilestone(Reminder reminder, NotificationJob job, String timeZone) {
        if (reminder == null || job == null || reminder.getReminderType() != ReminderType.APPOINTMENT) return;
        if (job.getId() == null) return;
        if (appointmentJobRepository != null
                && appointmentJobRepository.findByIdForUpdate(job.getId()).isEmpty()) {
            return;
        }
        String eventKey = "MILESTONE|" + job.getId();
        String title = milestoneTitle(job.getOffsetMinutes());
        String body = milestoneBody(reminder, job, timeZone);
        for (Recipient recipient : recipients(reminder)) {
            Map<String, String> values = metadata(
                    recipient, reminder, "MILESTONE", eventKey, job.getId());
            var existing = notificationRepository.findAppointmentMilestoneByRecipientAndJobShared(
                    recipient.userId(), job.getId());
            if (existing.isPresent()) {
                refreshSharedRoute(existing.get(), recipient, values);
                continue;
            }
            saveAndDeliver(recipient, reminder, eventKey, "MILESTONE", title, body,
                    values);
        }
    }

    private void notifyEvent(
            Reminder reminder,
            String eventType,
            String eventKey,
            String title,
            String body) {
        if (reminder == null || reminder.getReminderType() == null
                || !"APPOINTMENT".equals(reminder.getReminderType().name())) {
            return;
        }
        for (Recipient recipient : recipients(reminder)) {
            if (notificationRepository.findAppointmentEventByRecipientAndGroup(
                    recipient.userId(), recipient.careGroupId(), eventKey).isPresent()) {
                continue;
            }
            saveAndDeliver(recipient, reminder, eventKey, eventType, title, body,
                    metadata(recipient, reminder, eventType, eventKey, null));
        }
    }

    private List<Recipient> recipients(Reminder reminder) {
        if (reminder.getOwnerUserId() == null) return List.of();
        Map<UUID, Recipient> byUser = new TreeMap<>();
        groupRepository.findByOwnerUserIdAndStatus(
                        reminder.getOwnerUserId(), CareGroupStatus.ACTIVE).stream()
                .filter(group -> hasActiveLinkedContext(group, reminder))
                .flatMap(group -> memberRepository.findByCareGroupIdAndInviteStatusIn(
                        group.getId(), List.of(InviteStatus.ACCEPTED)).stream()
                        .filter(member -> member.getUserId() != null)
                        .filter(member -> !reminder.getOwnerUserId().equals(member.getUserId()))
                        .filter(member -> authorizationPolicy.hasPermission(
                                group.getId(), member.getUserId(), PermissionFlag.CALENDAR))
                        .map(member -> new Recipient(group.getId(), member.getUserId())))
                .forEach(recipient -> byUser.merge(
                        recipient.userId(), recipient,
                        CareGroupAppointmentNotificationService::preferDeterministicGroup));
        return List.copyOf(byUser.values());
    }

    private void refreshSharedRoute(
            NotificationRecord existing, Recipient recipient, Map<String, String> metadata) {
        if (!recipient.careGroupId().equals(existing.getCareGroupId())
                || !metadata.equals(existing.getMetadata())) {
            existing.setCareGroupId(recipient.careGroupId());
            existing.setMetadata(metadata);
            notificationRepository.save(existing);
        }
    }

    private static Recipient preferDeterministicGroup(Recipient first, Recipient second) {
        if (first.careGroupId() == null) return second;
        if (second.careGroupId() == null) return first;
        return first.careGroupId().compareTo(second.careGroupId()) <= 0 ? first : second;
    }

    private boolean hasActiveLinkedContext(CareGroup group, Reminder reminder) {
        boolean journeyMatches = reminder.getJourneyId() != null
                && group.getLinkedJourneyId() != null
                && reminder.getJourneyId().equals(group.getLinkedJourneyId())
                && journeyRepository.existsByIdAndOwnerUserIdAndStatus(
                        group.getLinkedJourneyId(), group.getOwnerUserId(), JourneyStatus.ACTIVE);
        boolean babyMatches = reminder.getBabyId() != null
                && group.getLinkedBabyProfileId() != null
                && reminder.getBabyId().equals(group.getLinkedBabyProfileId())
                && babyRepository.findByIdAndOwnerUserId(
                                group.getLinkedBabyProfileId(), group.getOwnerUserId())
                        .filter(baby -> baby.getStatus() == BabyProfileStatus.ACTIVE)
                        .isPresent();
        return journeyMatches || babyMatches;
    }

    private void saveAndDeliver(
            Recipient recipient,
            Reminder reminder,
            String eventKey,
            String eventType,
            String title,
            String body,
            Map<String, String> metadata) {
        boolean pushEnabled = false;
        try {
            pushEnabled = preferenceRepository.isPushEnabled(
                    recipient.userId(), NotificationType.REMINDER);
        } catch (RuntimeException exception) {
            log.warn("Appointment family preference lookup failed user={} group={} error={}",
                    recipient.userId(), recipient.careGroupId(), exception.getClass().getSimpleName());
        }
        List<DeviceToken> tokens = List.of();
        if (pushEnabled) {
            try {
                tokens = deviceTokenRepository.findByUserIdAndActiveTrue(recipient.userId());
            } catch (RuntimeException exception) {
                log.warn("Appointment family token lookup failed user={} group={} error={}",
                        recipient.userId(), recipient.careGroupId(), exception.getClass().getSimpleName());
            }
        }
        NotificationRecord record = NotificationRecord.builder()
                .userId(recipient.userId())
                .type(NotificationType.REMINDER)
                .title(title)
                .body(body)
                .referenceId(reminder.getId())
                .careGroupId(recipient.careGroupId())
                .referenceType("APPOINTMENT")
                .status(NotificationRecordStatus.SENT)
                .channel("IN_APP")
                .sentAt(Instant.now())
                .metadata(metadata)
                .build();

        boolean providerReady = false;
        if (pushEnabled && !tokens.isEmpty()) {
            try {
                providerReady = fcmService.isReady();
            } catch (RuntimeException exception) {
                log.warn("Appointment family provider readiness failed user={} group={} error={}",
                        recipient.userId(), recipient.careGroupId(), exception.getClass().getSimpleName());
            }
        }
        if (pushEnabled && providerReady && !tokens.isEmpty()) {
            int attempts = 0;
            int successes = 0;
            String firstMessageId = null;
            for (DeviceToken token : tokens) {
                try {
                    FcmDeliveryResult delivery = fcmService.sendWithRetry(
                            token.getToken(), title, body, metadata, 3);
                    if (delivery == null) {
                        attempts++;
                        log.warn("Appointment family provider returned no result user={} group={} platform={}",
                                recipient.userId(), recipient.careGroupId(), token.getPlatform());
                        continue;
                    }
                    attempts += delivery.attempts();
                    if (delivery.success()) {
                        successes++;
                        if (firstMessageId == null) firstMessageId = delivery.messageId();
                    }
                } catch (RuntimeException ignored) {
                    // A provider/token failure is isolated to this token. The
                    // authorized in-app record is still persisted below.
                    attempts++;
                    log.warn("Appointment family push failed user={} group={} platform={} error={}",
                            recipient.userId(), recipient.careGroupId(), token.getPlatform(),
                            ignored.getClass().getSimpleName());
                }
            }
            record.setAttemptCount(attempts);
            if (successes > 0) {
                record.setChannel("PUSH");
                record.setFcmMessageId(firstMessageId);
            }
        }

        NotificationRecord saved = notificationRepository.save(record);
        auditService.log(AuditAction.NOTIFICATION_SENT, recipient.userId(),
                "NotificationRecord", saved.getId().toString(), eventType);
    }

    private Map<String, String> metadata(
            Recipient recipient,
            Reminder reminder,
            String eventType,
            String eventKey,
            UUID jobId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("type", "REMINDER");
        values.put("referenceType", "APPOINTMENT");
        values.put("referenceId", reminder.getId().toString());
        values.put("reminderId", reminder.getId().toString());
        values.put("careGroupId", recipient.careGroupId().toString());
        values.put("eventType", eventType);
        values.put("eventKey", eventKey);
        if (jobId != null) values.put("milestoneJobId", jobId.toString());
        values.put("route", "/care-groups/" + recipient.careGroupId()
                + "/appointments/" + reminder.getId());
        return values;
    }

    private String format(Reminder reminder, String timeZone) {
        ZoneId zone = ZoneId.of(timeZone == null || timeZone.isBlank()
                ? "Asia/Ho_Chi_Minh" : timeZone);
        return DATE_TIME.format(reminder.getScheduledAt().atZone(zone));
    }

    private String milestoneTitle(int offsetMinutes) {
        if (offsetMinutes > 0) return "Lịch hẹn chưa hoàn thành";
        if (offsetMinutes == 0) return "Đã đến giờ lịch hẹn";
        return "Sắp đến lịch hẹn";
    }

    private String milestoneBody(Reminder reminder, NotificationJob job, String timeZone) {
        ZoneId zone = ZoneId.of(timeZone == null || timeZone.isBlank()
                ? "Asia/Ho_Chi_Minh" : timeZone);
        String time = DATE_TIME.format(job.getOccurrenceScheduledAt().atZone(zone));
        if (job.getOffsetMinutes() > 0) {
            return "Lịch hẹn \"" + reminder.getTitle() + "\" chưa được hoàn thành.";
        }
        return "Lịch hẹn \"" + reminder.getTitle() + "\" lúc " + time + ".";
    }

    private record Recipient(UUID careGroupId, UUID userId) {
    }
}
