package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.dto.request.CreateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.request.RedFlagRuleFilter;
import com.carebridge.backend.triage.dto.request.UpdateRedFlagRuleRequest;
import com.carebridge.backend.triage.dto.response.RedFlagRulePageResponse;
import com.carebridge.backend.triage.dto.response.RedFlagRuleResponse;
import com.carebridge.backend.triage.exception.RedFlagRuleException;
import java.util.UUID;

/**
 * Service contract for CRUD operations on admin-managed red-flag detection rules.
 * Rules with isSystemDefault=true are protected per BR-SAFETY-RFR-003 (ADR-001) —
 * they cannot be deactivated or deleted through this interface.
 */
public interface RedFlagRuleService {

    /**
     * Creates a new red-flag rule. isSystemDefault is always false for admin-created rules.
     * @throws RedFlagRuleException (MOD-025) if keyword already exists (case-insensitive)
     */
    RedFlagRuleResponse createRule(CreateRedFlagRuleRequest request, UUID actorUserId);

    /**
     * Returns a paginated, filtered list of rules (filter by severity/isActive).
     */
    RedFlagRulePageResponse listRules(RedFlagRuleFilter filter);

    /**
     * Updates an existing rule's keyword/severity/action/isActive.
     * @throws RedFlagRuleException (MOD-026) if ruleId does not exist
     * @throws RedFlagRuleException (MOD-027) if rule.isSystemDefault=true AND request attempts isActive=false
     */
    RedFlagRuleResponse updateRule(UUID ruleId, UpdateRedFlagRuleRequest request, UUID actorUserId);

    /**
     * Hard-deletes a rule.
     * @throws RedFlagRuleException (MOD-026) if ruleId does not exist
     * @throws RedFlagRuleException (MOD-027) if rule.isSystemDefault=true
     */
    void deleteRule(UUID ruleId, UUID actorUserId);
}
