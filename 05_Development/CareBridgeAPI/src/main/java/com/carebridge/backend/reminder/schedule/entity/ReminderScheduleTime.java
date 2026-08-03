package com.carebridge.backend.reminder.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reminder_schedule_times")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderScheduleTime {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "time_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "schedule_id", nullable = false, updatable = false)
    private UUID scheduleId;

    @Column(name = "local_time", nullable = false)
    private LocalTime localTime;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
