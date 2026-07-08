package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.request.RedFlagRuleFilter;
import com.carebridge.backend.triage.dto.request.UpdateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.response.RedFlagRulePageResponse;
import com.carebridge.backend.triage.dto.response.RedFlagRuleResponse;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.exception.RedFlagRuleException;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.impl.RedFlagRuleServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RedFlagRuleServiceImplTest {

    @Mock
    private RedFlagRuleRepository redFlagRuleRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RedFlagRuleServiceImpl service;

    private static final UUID RULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNKNOWN_ID = UUID.fromString("ffffffff-0000-0000-0000-000000000000");

    // RFR-TC-001
    @Test
    void createRule_validRequest_persistsAndLogsAudit() {
        CreateRedFlagRuleRequest request = RedFlagRuleTestFactory.makeCreateRequest();
        when(redFlagRuleRepository.existsByKeywordIgnoreCase(request.keyword())).thenReturn(false);
        when(redFlagRuleRepository.save(any(RedFlagRule.class))).thenAnswer(inv -> {
            RedFlagRule saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RedFlagRuleResponse response = service.createRule(request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID);

        assertThat(response.isActive()).isTrue();
        assertThat(response.isSystemDefault()).isFalse();
        assertThat(response.keyword()).isEqualTo(request.keyword());
        verify(redFlagRuleRepository).save(any(RedFlagRule.class));
        verify(auditService).log(
                eq(AuditAction.RED_FLAG_RULE_CREATED),
                eq(RedFlagRuleTestFactory.SYSTEM_ADMIN_ID),
                eq("RedFlagRule"),
                any(),
                any());
    }

    // RFR-TC-002
    @Test
    void createRule_duplicateKeyword_throwsMod025() {
        CreateRedFlagRuleRequest request = new CreateRedFlagRuleRequest(
                "CHẢY MÁU NHIỀU", RedFlagSeverity.RED, com.carebridge.backend.triage.entity.RedFlagAction.ESCALATE);
        when(redFlagRuleRepository.existsByKeywordIgnoreCase("CHẢY MÁU NHIỀU")).thenReturn(true);

        RedFlagRuleException ex = assertThrows(RedFlagRuleException.class,
                () -> service.createRule(request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID));

        assertEquals("MOD-025", ex.getCode());
        verify(redFlagRuleRepository, never()).save(any());
    }

    // RFR-TC-004
    @Test
    void listRules_withSeverityAndActiveFilter_returnsFilteredPage() {
        RedFlagRuleFilter filter = new RedFlagRuleFilter(RedFlagSeverity.RED, true, 0, 20);
        RedFlagRule r1 = RedFlagRuleTestFactory.makeSystemDefaultRule();
        RedFlagRule r2 = RedFlagRuleTestFactory.makeAdminRule();
        Page<RedFlagRule> page = new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 20), 2);
        when(redFlagRuleRepository.findBySeverityAndActive(RedFlagSeverity.RED, true, PageRequest.of(0, 20)))
                .thenReturn(page);

        RedFlagRulePageResponse response = service.listRules(filter);

        assertEquals(2, response.totalElements());
        assertThat(response.content()).allMatch(r -> r.severity() == RedFlagSeverity.RED && r.isActive());
    }

    // RFR-TC-005
    @Test
    void updateRule_nonDefaultRule_updatesAndLogsAudit() {
        when(redFlagRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(RedFlagRuleTestFactory.makeAdminRule()));
        when(redFlagRuleRepository.save(any(RedFlagRule.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateRedFlagRuleRequest request = new UpdateRedFlagRuleRequest("từ khoá đã sửa", null, null, null);

        RedFlagRuleResponse response = service.updateRule(RULE_ID, request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID);

        assertEquals("từ khoá đã sửa", response.keyword());
        verify(auditService).log(
                eq(AuditAction.RED_FLAG_RULE_UPDATED),
                eq(RedFlagRuleTestFactory.SYSTEM_ADMIN_ID),
                eq("RedFlagRule"),
                any(),
                any());
    }

    // RFR-TC-006
    @Test
    void updateRule_unknownRuleId_throwsMod026() {
        when(redFlagRuleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        RedFlagRuleException ex = assertThrows(RedFlagRuleException.class,
                () -> service.updateRule(UNKNOWN_ID, RedFlagRuleTestFactory.makeDeactivateRequest(),
                        RedFlagRuleTestFactory.SYSTEM_ADMIN_ID));

        assertEquals("MOD-026", ex.getCode());
    }

    // RFR-TC-007 — CRITICAL BR-SAFETY
    @Test
    void updateRule_deactivateSystemDefault_throwsMod027AndNeverSaves() {
        when(redFlagRuleRepository.findById(RULE_ID))
                .thenReturn(Optional.of(RedFlagRuleTestFactory.makeSystemDefaultRule()));
        UpdateRedFlagRuleRequest request = RedFlagRuleTestFactory.makeDeactivateRequest();

        RedFlagRuleException ex = assertThrows(RedFlagRuleException.class,
                () -> service.updateRule(RULE_ID, request, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID));

        assertEquals("MOD-027", ex.getCode());
        verify(redFlagRuleRepository, never()).save(any());
    }

    // RFR-TC-008
    @Test
    void deleteRule_nonDefaultRule_removesAndLogsAudit() {
        when(redFlagRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(RedFlagRuleTestFactory.makeAdminRule()));

        service.deleteRule(RULE_ID, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID);

        verify(redFlagRuleRepository).delete(any(RedFlagRule.class));
        verify(auditService).log(
                eq(AuditAction.RED_FLAG_RULE_DELETED),
                eq(RedFlagRuleTestFactory.SYSTEM_ADMIN_ID),
                eq("RedFlagRule"),
                any(),
                any());
    }

    // RFR-TC-009 — CRITICAL BR-SAFETY
    @Test
    void deleteRule_systemDefault_throwsMod027() {
        when(redFlagRuleRepository.findById(RULE_ID))
                .thenReturn(Optional.of(RedFlagRuleTestFactory.makeSystemDefaultRule()));

        RedFlagRuleException ex = assertThrows(RedFlagRuleException.class,
                () -> service.deleteRule(RULE_ID, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID));

        assertEquals("MOD-027", ex.getCode());
        verify(redFlagRuleRepository, never()).delete(any(RedFlagRule.class));
    }

    // RFR-TC-010
    @Test
    void deleteRule_unknownRuleId_throwsMod026() {
        when(redFlagRuleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        RedFlagRuleException ex = assertThrows(RedFlagRuleException.class,
                () -> service.deleteRule(UNKNOWN_ID, RedFlagRuleTestFactory.SYSTEM_ADMIN_ID));

        assertEquals("MOD-026", ex.getCode());
    }
}
