package com.carebridge.backend.health;

import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.entity.RecordType;

import java.time.Instant;
import java.util.UUID;

public class HealthRecord41TestFactory {

    public static final UUID ACC_001 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ACC_999 = UUID.fromString("00000000-0000-0000-0000-000000000999");
    public static final UUID HR_001  = UUID.fromString("00000000-0000-0000-0000-000000041001");
    public static final UUID HR_002  = UUID.fromString("00000000-0000-0000-0000-000000041002");
    public static final UUID HR_003  = UUID.fromString("00000000-0000-0000-0000-000000041003");

    public static HealthRecord makeActiveRecord() {
        return HealthRecord.builder()
                .id(HR_001)
                .ownerUserId(ACC_001)
                .recordType(RecordType.LAB_RESULT)
                .title("Blood Test")
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-06-20T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeArchivedRecord() {
        return HealthRecord.builder()
                .id(HR_002)
                .ownerUserId(ACC_001)
                .recordType(RecordType.LAB_RESULT)
                .title("Blood Test")
                .status(HealthRecordStatus.ARCHIVED)
                .createdAt(Instant.parse("2026-06-20T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T08:00:00Z"))
                .build();
    }

    public static HealthRecord makeOtherUserActiveRecord() {
        return HealthRecord.builder()
                .id(HR_003)
                .ownerUserId(ACC_999)
                .recordType(RecordType.LAB_RESULT)
                .title("Blood Test")
                .status(HealthRecordStatus.ACTIVE)
                .createdAt(Instant.parse("2026-06-20T08:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T08:00:00Z"))
                .build();
    }
}
