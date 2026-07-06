package com.carebridge.backend.health;

import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.entity.RecordType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class HealthRecord42TestFactory {

    public static final UUID ACC_001   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ACC_999   = UUID.fromString("00000000-0000-0000-0000-000000000999");
    public static final UUID HR_A      = UUID.fromString("00000000-0000-0000-0000-000000420001");
    public static final UUID HR_B      = UUID.fromString("00000000-0000-0000-0000-000000420002");
    public static final UUID HR_C      = UUID.fromString("00000000-0000-0000-0000-000000420003");
    public static final UUID HR_D      = UUID.fromString("00000000-0000-0000-0000-000000420004");
    public static final UUID JOURNEY_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID BABY_001  = UUID.fromString("00000000-0000-0000-0002-000000000001");

    public static HealthRecord makeActiveLabResult() {
        return HealthRecord.builder()
                .id(HR_A)
                .ownerUserId(ACC_001)
                .recordType(RecordType.LAB_RESULT)
                .title("Blood Test Q2")
                .recordDate(LocalDate.of(2026, 6, 20))
                .journeyId(JOURNEY_1)
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-06-20T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeActiveUltrasound() {
        return HealthRecord.builder()
                .id(HR_B)
                .ownerUserId(ACC_001)
                .recordType(RecordType.ULTRASOUND)
                .title("Week 20 Ultrasound")
                .recordDate(LocalDate.of(2026, 5, 10))
                .journeyId(JOURNEY_1)
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-05-10T08:00:00Z"))
                .updatedAt(Instant.parse("2026-05-10T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeArchivedRecord() {
        return HealthRecord.builder()
                .id(HR_C)
                .ownerUserId(ACC_001)
                .recordType(RecordType.LAB_RESULT)
                .title("Old Lab Test")
                .recordDate(LocalDate.of(2026, 4, 1))
                .status(HealthRecordStatus.ARCHIVED)
                .createdAt(Instant.parse("2026-04-01T08:00:00Z"))
                .updatedAt(Instant.parse("2026-04-01T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeOtherUserRecord() {
        return HealthRecord.builder()
                .id(HR_D)
                .ownerUserId(ACC_999)
                .recordType(RecordType.LAB_RESULT)
                .title("Other User Lab")
                .recordDate(LocalDate.of(2026, 6, 25))
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-06-25T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-25T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeActivePrescriptionForBaby() {
        return HealthRecord.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000420005"))
                .ownerUserId(ACC_001)
                .recordType(RecordType.PRESCRIPTION)
                .title("Baby Prescription")
                .recordDate(LocalDate.of(2026, 6, 15))
                .babyId(BABY_001)
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-06-15T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-15T08:00:00Z"))
                .build();
    }
}
