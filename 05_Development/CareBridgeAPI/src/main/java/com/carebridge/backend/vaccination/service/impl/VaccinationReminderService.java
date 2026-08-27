package com.carebridge.backend.vaccination.service.impl;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.notification.dto.VaccinationReminderCommand;
import com.carebridge.backend.notification.service.IVaccinationNotificationService;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.service.IVaccinationReminderService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaccinationReminderService implements IVaccinationReminderService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationReminderService.class);

    /**
     * A POSTPONED dose still has a date the mother agreed to, so it is still worth a reminder.
     * COMPLETED and DELETED doses are not.
     */
    private static final List<VaccinationRecordStatus> DUE_STATUSES =
            List.of(VaccinationRecordStatus.SCHEDULED, VaccinationRecordStatus.POSTPONED);

    private final VaccinationRecordRepository recordRepository;
    private final BabyProfileRepository babyRepository;
    private final IVaccinationNotificationService notificationService;
    private final VaccinationProperties properties;

    @Override
    public int dispatchDueReminders() {
        return dispatchDueReminders(LocalDate.now(properties.getReminder().getZone()));
    }

    @Override
    @Transactional(readOnly = true)
    public int dispatchDueReminders(LocalDate today) {
        VaccinationProperties.Reminder config = properties.getReminder();
        if (!config.isEnabled()) {
            return 0;
        }

        List<Integer> leads = config.getLeadDays().stream()
                .filter(lead -> lead != null && lead >= 0)
                .distinct()
                .sorted()
                .toList();
        if (leads.isEmpty()) {
            return 0;
        }

        // One scan covers every lead: a dose scheduled `lead` days from today is exactly the
        // dose whose `lead` milestone fires today.
        List<VaccinationRecord> due = recordRepository.findByStatusInAndScheduledDateBetween(
                DUE_STATUSES, today, today.plusDays(config.maxLeadDays()));
        if (due.isEmpty()) {
            return 0;
        }

        Set<Integer> leadSet = Set.copyOf(leads);
        Map<UUID, BabyProfile> babies = loadBabies(due);

        int dispatched = 0;
        for (VaccinationRecord record : due) {
            int daysBefore = (int) ChronoUnit.DAYS.between(today, record.getScheduledDate());
            if (!leadSet.contains(daysBefore)) {
                continue;
            }

            BabyProfile baby = babies.get(record.getBabyId());
            // Reminders follow the baby's owner — the mother. An archived profile stops
            // producing them; the book itself is preserved.
            if (baby == null || baby.getStatus() != BabyProfileStatus.ACTIVE || baby.getOwnerUserId() == null) {
                continue;
            }

            try {
                var sent = notificationService.sendVaccinationReminder(new VaccinationReminderCommand(
                        record.getId(),
                        baby.getId(),
                        baby.getOwnerUserId(),
                        baby.getNickname(),
                        record.getVaccineName(),
                        record.getDoseNumber(),
                        record.getScheduledDate(),
                        daysBefore));
                if (sent != null) {
                    dispatched++;
                }
            } catch (RuntimeException exception) {
                // One undeliverable dose must not stop the rest of the run.
                log.warn("Vaccination reminder failed for record {} (lead {} days)",
                        record.getId(), daysBefore, exception);
            }
        }
        return dispatched;
    }

    private Map<UUID, BabyProfile> loadBabies(List<VaccinationRecord> records) {
        List<UUID> babyIds = records.stream().map(VaccinationRecord::getBabyId).distinct().toList();
        Map<UUID, BabyProfile> byId = new HashMap<>();
        for (BabyProfile baby : babyRepository.findAllById(babyIds)) {
            byId.put(baby.getId(), baby);
        }
        return byId;
    }
}
