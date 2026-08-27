package com.carebridge.backend.vaccination.config;

import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the automatic vaccination book (MF-03 Baby Care Journey &amp; Vaccination).
 *
 * <p>{@code scheduleVersion} selects which catalogue in {@code vaccination_schedules} is
 * scanned when a baby is registered. The table holds several versions at once — the seven
 * pre-existing rows each carry their own {@code legacy-*} version — so a single active
 * version is what makes "scan the catalogue" a well-defined set operation.
 */
@Component
@ConfigurationProperties(prefix = "carebridge.vaccination")
public class VaccinationProperties {

    private String scheduleVersion = "vn-2026";
    private final Reminder reminder = new Reminder();

    public String getScheduleVersion() {
        return scheduleVersion;
    }

    public void setScheduleVersion(String scheduleVersion) {
        this.scheduleVersion = scheduleVersion;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public static class Reminder {

        private boolean enabled = true;

        /**
         * How many days before the scheduled date each reminder fires. One notification is
         * sent per lead value per dose, deduplicated on (record, leadDays), so a mother gets
         * a week's notice, a nudge three days out, one the day before, and one on the day.
         */
        private List<Integer> leadDays = List.of(7, 3, 1, 0);

        /** Reminder-day arithmetic is done in the mother's local calendar, not UTC. */
        private ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        /** Cron for the dispatch sweep; consumed directly by the job's @Scheduled. */
        private String cron = "0 0 7 * * *";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<Integer> getLeadDays() {
            return leadDays;
        }

        public void setLeadDays(List<Integer> leadDays) {
            this.leadDays = leadDays;
        }

        public ZoneId getZone() {
            return zone;
        }

        public void setZone(ZoneId zone) {
            this.zone = zone;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        /** Largest lead value, i.e. how far ahead the dispatch scan has to look. */
        public int maxLeadDays() {
            return leadDays.stream().filter(d -> d != null && d >= 0).mapToInt(Integer::intValue).max().orElse(0);
        }
    }
}
