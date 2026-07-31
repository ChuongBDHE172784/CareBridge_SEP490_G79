package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.checklist.audit.ChecklistAuditActorType;
import com.carebridge.backend.checklist.audit.ChecklistAuditEvent;
import com.carebridge.backend.checklist.audit.ChecklistAuditResourceType;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistAuditWriterTest {

    @Mock private AuditLogRepository repository;
    @Mock private AuditEligibilityPolicy policy;
    @Mock private ObjectMapper objectMapper;

    private ChecklistAuditWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ChecklistAuditWriter(repository, policy, objectMapper);
    }

    @Test
    void persistsOnlyTypedAllowlistedChecklistFields() throws Exception {
        ChecklistAuditEvent event = event(AuditAction.CHECKLIST_COMPLETED);
        when(policy.shouldAudit(event.action())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"PENDING\"}", "{\"status\":\"COMPLETED\"}");

        writer.write(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(AuditAction.CHECKLIST_COMPLETED);
        assertThat(saved.getActorType()).isEqualTo("USER");
        assertThat(saved.getEntityType()).isEqualTo(ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE.name());
        assertThat(saved.getEntityId()).isEqualTo(event.resourceId());
        assertThat(saved.getSubjectUserId()).isEqualTo(event.recipientUserId());
        assertThat(saved.getCareContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
        assertThat(saved.getCareContextId()).isEqualTo(event.careContextId());
        assertThat(saved.getTemplateVersionId()).isEqualTo(event.templateVersionId());
        assertThat(saved.getChecklistTaskInstanceId()).isEqualTo(event.checklistTaskInstanceId());
        assertThat(saved.getReasonCode()).isEqualTo("USER_CONFIRMED");
        assertThat(saved.getCorrelationId()).isEqualTo(event.correlationId());
    }

    @Test
    void rejectsRequiredActionWhenPolicyIsNotEligible() {
        ChecklistAuditEvent event = event(AuditAction.CHECKLIST_COMPLETED);
        when(policy.shouldAudit(event.action())).thenReturn(false);

        assertThatThrownBy(() -> writer.write(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eligible");
        verify(repository, never()).save(any());
    }

    @Test
    void propagatesSerializationFailureBeforePersistence() throws Exception {
        ChecklistAuditEvent event = event(AuditAction.CHECKLIST_COMPLETED);
        when(policy.shouldAudit(event.action())).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });

        assertThatThrownBy(() -> writer.write(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
        verify(repository, never()).save(any());
    }

    @Test
    void completedActionRequiresTypedTaskSubjectAndStatusTransition() {
        ChecklistAuditEvent valid = event(AuditAction.CHECKLIST_COMPLETED);
        ChecklistAuditEvent incomplete = new ChecklistAuditEvent(
                valid.action(), valid.actorUserId(), valid.actorType(), valid.actorService(),
                valid.resourceType(), valid.resourceId(),
                null, valid.careContextType(), valid.careContextId(), valid.templateVersionId(),
                null, null, "FREE TEXT", valid.reasonCode(), valid.correlationId());
        when(policy.shouldAudit(incomplete.action())).thenReturn(true);

        assertThatThrownBy(() -> writer.write(incomplete))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnpairedOrActionMismatchedResourceBeforePersistence() {
        ChecklistAuditEvent valid = event(AuditAction.CHECKLIST_COMPLETED);
        ChecklistAuditEvent unpaired = new ChecklistAuditEvent(
                valid.action(), valid.actorUserId(), valid.actorType(), valid.actorService(),
                ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE, null,
                valid.recipientUserId(), valid.careContextType(), valid.careContextId(), valid.templateVersionId(),
                valid.checklistTaskInstanceId(), valid.beforeStatus(), valid.afterStatus(),
                valid.reasonCode(), valid.correlationId());
        ChecklistAuditEvent mismatched = new ChecklistAuditEvent(
                valid.action(), valid.actorUserId(), valid.actorType(), valid.actorService(),
                ChecklistAuditResourceType.CHECKLIST_INSTANCE, UUID.randomUUID(),
                valid.recipientUserId(), valid.careContextType(), valid.careContextId(), valid.templateVersionId(),
                valid.checklistTaskInstanceId(), valid.beforeStatus(), valid.afterStatus(),
                valid.reasonCode(), valid.correlationId());
        when(policy.shouldAudit(valid.action())).thenReturn(true);

        assertThatThrownBy(() -> writer.write(unpaired))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paired");
        assertThatThrownBy(() -> writer.write(mismatched))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CHECKLIST_TASK_INSTANCE");
        verify(repository, never()).save(any());
    }

    private static ChecklistAuditEvent event(AuditAction action) {
        UUID taskId = UUID.randomUUID();
        return new ChecklistAuditEvent(
                action,
                UUID.randomUUID(),
                ChecklistAuditActorType.USER,
                null,
                ChecklistAuditResourceType.CHECKLIST_TASK_INSTANCE,
                taskId,
                UUID.randomUUID(),
                ChecklistCareContextType.JOURNEY,
                UUID.randomUUID(),
                UUID.randomUUID(),
                taskId,
                "PENDING",
                "COMPLETED",
                "USER_CONFIRMED",
                UUID.randomUUID());
    }
}
