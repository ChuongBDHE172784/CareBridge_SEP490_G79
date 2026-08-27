package com.carebridge.backend.audit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * UC117-TC-014 (Track A characterization) — AuditLogResponse exposes the safe
 * actor identity needed by administrators, but never leaks forensic-only fields.
 */
class AuditLogMapperTest {

    private final AuditLogMapper mapper = new AuditLogMapper();

    @Test
    void toResponse_exposesSafeContractFields() {
        AuditLog log = AuditLog.builder()
                .auditLogId(UUID.randomUUID())
                .createdAt(Instant.now())
                .actorUserId(UUID.randomUUID())
                .action(AuditAction.MODERATION_ACTION)
                .entityType("CommunityAnswer")
                .entityId(UUID.randomUUID())
                .newValueJson("{\"decision\":\"REMOVED\"}")
                .oldValueJson("{\"decision\":\"PENDING\"}")
                .ipAddress("203.0.113.10")
                .build();

        AuditLogResponse dto = mapper.toResponse(log, "Quản trị viên Huy", "admin@carebridge.dev");

        Set<String> fieldNames = Arrays.stream(AuditLogResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(fieldNames).containsExactlyInAnyOrder(
                "id", "timestamp", "userId", "actorName", "actorEmail",
                "action", "resourceType", "resourceId", "details");
        assertThat(fieldNames).doesNotContain("ipAddress", "oldValueJson");

        assertThat(dto.getDetails()).isEqualTo("{\"decision\":\"REMOVED\"}");
        assertThat(dto.getActorName()).isEqualTo("Quản trị viên Huy");
        assertThat(dto.getActorEmail()).isEqualTo("admin@carebridge.dev");
    }
}
