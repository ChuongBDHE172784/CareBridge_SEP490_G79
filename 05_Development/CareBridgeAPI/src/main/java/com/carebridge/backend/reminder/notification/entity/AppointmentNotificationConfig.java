package com.carebridge.backend.reminder.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "appointment_notification_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationConfig {

    @Id
    @Column(name = "reminder_id", nullable = false, updatable = false)
    private UUID reminderId;

    @Builder.Default
    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    @Column(name = "config_revision", nullable = false)
    private long configRevision = 1L;

    /**
     * Notification offsets, in display and execution order.
     *
     * <p>Replaces the appointment_notification_rules child table. Any change to
     * this list must bump {@link #configRevision} in the same transaction, because
     * jobs snapshot the revision they were materialised from.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_jsonb", nullable = false, columnDefinition = "jsonb")
    private List<AppointmentNotificationRuleEntry> rules = new ArrayList<>();

    /** Offsets in list order — the shape every caller actually works with. */
    public List<Integer> offsetMinutes() {
        if (rules == null) {
            return List.of();
        }
        return rules.stream().map(AppointmentNotificationRuleEntry::getOffsetMinutes).toList();
    }

    /** Replaces the configured offsets. Callers must also bump configRevision. */
    public void replaceOffsetMinutes(List<Integer> offsets) {
        List<AppointmentNotificationRuleEntry> replacement = new ArrayList<>();
        for (Integer offset : offsets) {
            replacement.add(new AppointmentNotificationRuleEntry(offset));
        }
        this.rules = replacement;
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
