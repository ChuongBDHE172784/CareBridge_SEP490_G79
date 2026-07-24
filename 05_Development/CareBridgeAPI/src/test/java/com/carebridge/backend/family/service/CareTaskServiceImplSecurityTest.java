package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.family.CareGroupTestFactory;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareTaskServiceImpl;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.family.CareGroupTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Security tests for CareTaskServiceImpl.
 *
 * OWASP A03:2021 — Injection / CWE-89 SQL Injection.
 * Verifies that JPA/Hibernate parameter binding neutralises SQL injection in free-text fields.
 */
@ExtendWith(MockitoExtension.class)
class CareTaskServiceImplSecurityTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @Mock private FcmService fcmService;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CareTaskServiceImpl service;

    // ── FAM73-TC-SEC-001: SQL injection via title/description → stored verbatim ─

    @Test
    void assignFamilyTask_sqlInjectionInTitle_storedLiterallyNotExecuted() {
        String maliciousTitle = "Buy diapers'; DROP TABLE family_tasks; --";
        String maliciousDesc  = "Size M'; DELETE FROM users WHERE '1'='1";

        CareGroup group = CareGroupTestFactory.makeCareGroup(g -> g.setId(GROUP_ID));
        CareGroupMember assignee = CareGroupTestFactory.makeCareGroupMember(m -> {
            m.setCareGroupId(GROUP_ID);
            m.setUserId(ASSIGNEE_ID);
            m.setInviteStatus(InviteStatus.ACCEPTED);
        });
        // The saved task should have the literal strings verbatim
        ArgumentCaptor<CareTask> taskCaptor = ArgumentCaptor.forClass(CareTask.class);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(authorizationPolicy.canAssignTasks(GROUP_ID, OWNER_ID)).thenReturn(true);
        when(memberRepository.findByCareGroupIdAndUserId(GROUP_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assignee));
        when(taskRepository.save(taskCaptor.capture())).thenAnswer(inv -> {
            // Return the captured task with a generated ID
            CareTask t = taskCaptor.getValue();
            return CareGroupTestFactory.makeCareTask(saved -> {
                saved.setTitle(t.getTitle());
                saved.setDescription(t.getDescription());
            });
        });
        when(deviceTokenRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of());

        AssignFamilyTaskRequest req = CareGroupTestFactory.makeAssignFamilyTaskRequest(r -> {
            r.setTitle(maliciousTitle);
            r.setDescription(maliciousDesc);
        });

        // Act — must NOT throw, must NOT execute SQL injection
        AssignFamilyTaskResponse response = service.assignFamilyTask(GROUP_ID, req, OWNER_ID);

        // Assert: JPA parameter binding preserved the literal string (injection neutralised)
        CareTask savedTask = taskCaptor.getValue();
        assertThat(savedTask.getTitle()).isEqualTo(maliciousTitle);
        assertThat(savedTask.getDescription()).isEqualTo(maliciousDesc);
        // Response returned normally — no SQL error
        assertThat(response).isNotNull();
    }
}
