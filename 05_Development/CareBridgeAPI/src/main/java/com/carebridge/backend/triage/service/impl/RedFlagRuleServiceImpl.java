package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.request.RedFlagRuleFilter;
import com.carebridge.backend.triage.dto.request.UpdateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.RedFlagRuleAuditDetails;
import com.carebridge.backend.triage.dto.response.RedFlagRulePageResponse;
import com.carebridge.backend.triage.dto.response.RedFlagRuleResponse;
import com.carebridge.backend.triage.entity.RedFlagRule;
import com.carebridge.backend.triage.exception.RedFlagRuleException;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.RedFlagRuleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RedFlagRuleServiceImpl implements RedFlagRuleService {

    private final RedFlagRuleRepository redFlagRuleRepository;
    private final AuditService auditService;

    @Override
    public RedFlagRuleResponse createRule(CreateRedFlagRuleRequest request, UUID actorUserId) {
        if (redFlagRuleRepository.existsByKeywordIgnoreCase(request.keyword())) {
            throw RedFlagRuleException.duplicateKeyword();
        }

        RedFlagRule rule = new RedFlagRule();
        rule.setKeyword(request.keyword());
        rule.setSeverity(request.severity());
        rule.setAction(request.action());
        rule.setActive(true);
        rule.setSystemDefault(false);
        rule.setCreatedBy(actorUserId);

        RedFlagRule saved = redFlagRuleRepository.save(rule);

        auditService.log(AuditAction.RED_FLAG_RULE_CREATED, actorUserId, "RedFlagRule",
                saved.getId().toString(), toAuditDetails(saved, "CREATED"));

        return toResponse(saved);
    }

    @Override
    public RedFlagRulePageResponse listRules(RedFlagRuleFilter filter) {
        Pageable pageable = PageRequest.of(filter.page(), filter.size());
        Page<RedFlagRule> page;
        if (filter.severity() != null && filter.isActive() != null) {
            page = redFlagRuleRepository.findBySeverityAndActive(filter.severity(), filter.isActive(), pageable);
        } else {
            page = redFlagRuleRepository.findAll(pageable);
        }

        List<RedFlagRuleResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new RedFlagRulePageResponse(content, page.getTotalElements(), filter.page(), filter.size());
    }

    @Override
    public RedFlagRuleResponse updateRule(UUID ruleId, UpdateRedFlagRuleRequest request, UUID actorUserId) {
        RedFlagRule rule = redFlagRuleRepository.findById(ruleId)
                .orElseThrow(RedFlagRuleException::ruleNotFound);

        // C4/BR-SAFETY-RFR-003: guard runs BEFORE any mutation.
        boolean attemptsDeactivate = request.isActive() != null && !request.isActive();
        if (rule.isSystemDefault() && attemptsDeactivate) {
            throw RedFlagRuleException.systemDefaultProtected();
        }

        if (request.keyword() != null) {
            rule.setKeyword(request.keyword());
        }
        if (request.severity() != null) {
            rule.setSeverity(request.severity());
        }
        if (request.action() != null) {
            rule.setAction(request.action());
        }
        if (request.isActive() != null) {
            rule.setActive(request.isActive());
        }
        rule.setUpdatedBy(actorUserId);

        RedFlagRule saved = redFlagRuleRepository.save(rule);

        auditService.log(AuditAction.RED_FLAG_RULE_UPDATED, actorUserId, "RedFlagRule",
                saved.getId().toString(), toAuditDetails(saved, "UPDATED"));

        return toResponse(saved);
    }

    @Override
    public void deleteRule(UUID ruleId, UUID actorUserId) {
        RedFlagRule rule = redFlagRuleRepository.findById(ruleId)
                .orElseThrow(RedFlagRuleException::ruleNotFound);

        // C4/BR-SAFETY-RFR-003: guard runs BEFORE any mutation.
        if (rule.isSystemDefault()) {
            throw RedFlagRuleException.systemDefaultProtected();
        }

        redFlagRuleRepository.delete(rule);

        auditService.log(AuditAction.RED_FLAG_RULE_DELETED, actorUserId, "RedFlagRule",
                ruleId.toString(), toAuditDetails(rule, "DELETED"));
    }

    private RedFlagRuleResponse toResponse(RedFlagRule rule) {
        return new RedFlagRuleResponse(
                rule.getId(),
                rule.getKeyword(),
                rule.getSeverity(),
                rule.getAction(),
                rule.isActive(),
                rule.isSystemDefault(),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }

    private RedFlagRuleAuditDetails toAuditDetails(RedFlagRule rule, String changeType) {
        return new RedFlagRuleAuditDetails(
                rule.getId(),
                rule.getKeyword(),
                rule.getSeverity().name(),
                rule.getAction().name(),
                rule.isActive(),
                rule.isSystemDefault(),
                changeType);
    }
}
