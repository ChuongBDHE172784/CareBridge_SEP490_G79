package com.carebridge.backend.reminder.notification.entity;

import java.util.Objects;

/**
 * One element of {@code appointment_notification_configs.rules_jsonb}.
 *
 * <p>Schema v1 is closed: {@code offsetMinutes} is the only supported key, and the
 * database CHECK rejects anything else. Position in the list replaces the retired
 * {@code sort_order} column.
 *
 * <p>Deliberately a mutable bean rather than a record: Hibernate builds its own
 * ObjectMapper for JSON columns and cannot be relied on to have the parameter-names
 * module registered, which record deserialization needs.
 */
public class AppointmentNotificationRuleEntry {

    private Integer offsetMinutes;

    public AppointmentNotificationRuleEntry() {
    }

    public AppointmentNotificationRuleEntry(Integer offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    public Integer getOffsetMinutes() {
        return offsetMinutes;
    }

    public void setOffsetMinutes(Integer offsetMinutes) {
        this.offsetMinutes = offsetMinutes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AppointmentNotificationRuleEntry entry)) return false;
        return Objects.equals(offsetMinutes, entry.offsetMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(offsetMinutes);
    }

    @Override
    public String toString() {
        return "AppointmentNotificationRuleEntry{offsetMinutes=" + offsetMinutes + '}';
    }
}
