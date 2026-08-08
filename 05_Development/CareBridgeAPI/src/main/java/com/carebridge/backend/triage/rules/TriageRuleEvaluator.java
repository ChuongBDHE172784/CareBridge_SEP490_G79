package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The deterministic clinical rule engine — the only component that sets an outcome.
 * Parity with {@code app/rules/evaluator.py}.
 */
/*
 * Deliberately NOT a @Component: it cannot exist without a valid registry, and a missing
 * evaluator is exactly the signal callers need. Obtain it via TriageV2ReadinessService,
 * which returns Optional.empty() whenever the rule set failed to load.
 */
public class TriageRuleEvaluator {

    public static final String ASK_CLARIFYING_QUESTIONS = "ASK_CLARIFYING_QUESTIONS";
    public static final String ROUTE_TO_HEALTHCARE_WORKER = "ROUTE_TO_HEALTHCARE_WORKER";

    private static final String PREDICATE_MISSING_FIELDS = "MISSING_REQUIRED_FIELDS";
    private static final String PREDICATE_OUT_OF_SCOPE = "NO_REPRODUCTIVE_RELEVANCE_AND_NO_GLOBAL_RED";
    private static final String PREDICATE_GREEN = "MINIMUM_DATASET_COMPLETE_AND_NO_HIGHER_RULE_MATCHED";

    static final List<String> REPRODUCTIVE_SIGNALS = List.of(
            "HEAVY_VAGINAL_BLEEDING", "VAGINAL_BLEEDING", "HEAVY_POSTPARTUM_BLEEDING",
            "LARGE_CLOTS", "REDUCED_FETAL_MOVEMENT", "FOUL_SMELLING_LOCHIA",
            "ABDOMINAL_PAIN", "SEVERE_ABDOMINAL_PAIN");

    static final List<String> REPRODUCTIVE_CONTEXT_FIELDS = List.of(
            "gestational_week", "postpartum_day", "possible_pregnancy", "delivery_method",
            "bleeding_amount", "last_menstrual_period_or_test");

    static final List<String> GLOBAL_SAFETY_SIGNAL_CODES = List.of(
            "ALTERED_CONSCIOUSNESS", "SEIZURE", "SEVERE_BREATHING_DIFFICULTY",
            "CYANOSIS", "SELF_HARM_IDEATION", "SELF_HARM_INTENT_OR_PLAN",
            "HARM_TO_BABY_IDEATION", "CANNOT_ENSURE_OWN_SAFETY");

    private final TriageRuleRegistry registry;

    public TriageRuleEvaluator(TriageRuleRegistry registry) {
        this.registry = registry;
    }

    /** One rule's role in the decision, for the internal audit trail. */
    public record MatchedRuleTrace(
            String ruleId,
            String outcome,
            int priority,
            String ruleVersion,
            String role,
            String suppressionReason,
            List<String> missingFields) {

        public MatchedRuleTrace(String ruleId, String outcome, int priority, String ruleVersion, String role) {
            this(ruleId, outcome, priority, ruleVersion, role, null, List.of());
        }
    }

    public record RuleEvaluation(
            String outcome,
            List<String> decisiveRuleIds,
            List<MatchedRuleTrace> allMatchedRules,
            boolean stopConversation,
            String actionCode,
            List<String> reasonCodes,
            List<String> questionIds,
            List<String> requiredFields,
            String rulesetVersion,
            String rulesetHash,
            List<String> greenBlockedBy,
            List<String> pendingRedRuleIds,
            List<String> unresolvedSignals,
            List<String> dataConflicts,
            String safetyScreenStatus,
            String contextDatasetStatus,
            String greenEligibilityDatasetStatus,
            String scopeStatus,
            List<String> pendingRiskStatuses,
            String primaryPendingRiskStatus,
            String completionReason,
            List<Map<String, Object>> auditMismatches,
            String triageV2Readiness) {

        public List<String> suppressedRuleIds() {
            return allMatchedRules.stream()
                    .filter(trace -> trace.role().startsWith("SUPPRESSED"))
                    .map(MatchedRuleTrace::ruleId)
                    .toList();
        }
    }

    public RuleEvaluation evaluate(
            String stage,
            Map<String, Object> signals,
            Map<String, Object> context,
            int questionRound,
            boolean reproductiveRelevanceHint,
            boolean minimumDatasetComplete) {
        return evaluate(stage, signals, context, questionRound, reproductiveRelevanceHint,
                minimumDatasetComplete, "RESOLVED");
    }

    public RuleEvaluation evaluate(
            String stage,
            Map<String, Object> signals,
            Map<String, Object> context,
            int questionRound,
            boolean reproductiveRelevanceHint,
            boolean minimumDatasetComplete,
            String contextResolutionStatus) {

        List<MatchedRuleTrace> traces = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        List<Map<String, Object>> auditMismatches = new ArrayList<>();

        if ("CONFLICTED".equals(contextResolutionStatus)) {
            conflicts.add("CONTEXT_RESOLUTION_CONFLICTED");
        }
        for (Map.Entry<String, Object> entry : signals.entrySet()) {
            if (Presence.parse(entry.getValue()) == Presence.CONFLICTED) {
                conflicts.add("SIGNAL_CONFLICTED:" + entry.getKey());
            }
        }

        // Zero-Trust Dataset Calculations
        String safetyScreenStatus = calcSafetyScreenStatus(signals);
        String contextDatasetStatus = calcContextDatasetStatus(stage, context);
        List<TriageGreenBlocker> blockers = activeBlockers(stage, signals, context, conflicts);
        String greenEligibilityStatus = calcGreenEligibilityStatus(
                stage, signals, context, safetyScreenStatus, contextDatasetStatus, blockers);

        // Caller assertion audit checks
        if (minimumDatasetComplete && !DatasetStatus.COMPLETE.name().equals(greenEligibilityStatus)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("field", "minimumDatasetComplete");
            map.put("callerValue", true);
            map.put("engineValue", greenEligibilityStatus);
            map.put("role", "CALLER_ASSERTION_IGNORED");
            auditMismatches.add(map);
        }

        List<String> evidence = reproductiveEvidence(signals, context);
        boolean hasReproductiveContext = !evidence.isEmpty() ||
                context.get("gestational_week") != null ||
                context.get("postpartum_day") != null ||
                "YES".equals(Objects.toString(context.get("possible_pregnancy"), ""));
        if (!reproductiveRelevanceHint && hasReproductiveContext) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("field", "reproductiveRelevance");
            map.put("callerValue", false);
            map.put("engineValue", "CONFLICTED");
            map.put("role", "CALLER_ASSERTION_IGNORED");
            auditMismatches.add(map);
        }

        // --- 1. non-clinical safety policies + global RED --------------------------
        List<TriageSafetyPolicy> policyHits = registry.policiesForStage(stage).stream()
                .filter(policy -> RuleConditionEvaluator.evaluate(policy.condition(), signals, context) == Tri.TRUE)
                .sorted((left, right) -> {
                    int byOrder = Integer.compare(left.decisionOrder(), right.decisionOrder());
                    return byOrder != 0 ? byOrder : left.policyId().compareTo(right.policyId());
                })
                .toList();

        List<TriageRule> candidates = registry.forStage(stage);
        Map<String, TriageRule> engineRules = new LinkedHashMap<>();
        List<TriageRule> matched = new ArrayList<>();
        List<TriageRule> pending = new ArrayList<>();
        LinkedHashSet<String> unresolved = new LinkedHashSet<>();

        for (TriageRule rule : candidates) {
            if (rule.isEngineRule()) {
                engineRules.putIfAbsent(rule.enginePredicate(), rule);
                continue;
            }
            ExclusionResult exRes = checkExclusion(rule, context, conflicts);
            if (exRes.isExcluded()) {
                traces.add(new MatchedRuleTrace(
                        rule.ruleId(), rule.outcome(), rule.priority(), rule.ruleVersion(),
                        "SUPPRESSED_BY_EXCLUSION", exRes.reason(), exRes.missingFields()));
                continue;
            }
            Tri state = RuleConditionEvaluator.evaluate(rule.condition(), signals, context);
            if (state == Tri.TRUE) {
                matched.add(rule);
            } else if (state == Tri.UNKNOWN && ("RED".equals(rule.outcome()) || "YELLOW".equals(rule.outcome()))) {
                if (RuleConditionEvaluator.satisfiedAtLeastOne(rule.condition(), signals, context)) {
                    pending.add(rule);
                    unresolved.addAll(collectUnresolvedSignals(rule.condition(), signals));
                    traces.add(new MatchedRuleTrace(
                            rule.ruleId(), rule.outcome(), rule.priority(), rule.ruleVersion(),
                            "PENDING_UNRESOLVED_SIGNAL"));
                }
            }
        }

        List<TriageRule> redMatched = TriageRuleRegistry.sortedByPrecedence(matched.stream()
                .filter(rule -> "RED".equals(rule.outcome()))
                .toList());

        List<TriageRule> yellowMatched = TriageRuleRegistry.sortedByPrecedence(matched.stream()
                .filter(rule -> "YELLOW".equals(rule.outcome()))
                .toList());

        List<TriageRule> pendingRed = pending.stream()
                .filter(rule -> "RED".equals(rule.outcome()))
                .toList();

        // Scope Status
        String scopeStatus = calcScopeStatus(
                stage, signals, context, safetyScreenStatus, reproductiveRelevanceHint,
                hasReproductiveContext, !policyHits.isEmpty(), !redMatched.isEmpty(),
                !pendingRed.isEmpty(), !yellowMatched.isEmpty(), !blockers.isEmpty());

        // Pending Risks
        List<String> pendingRisks = calcPendingRisks(
                safetyScreenStatus, contextDatasetStatus, !pendingRed.isEmpty(), !blockers.isEmpty());
        String primaryPending = determinePrimaryPendingRisk(pendingRisks);

        if (!policyHits.isEmpty()) {
            TriageSafetyPolicy leading = policyHits.get(0);
            for (TriageSafetyPolicy policy : policyHits) {
                traces.add(new MatchedRuleTrace(policy.policyId(), policy.outcome(),
                        policy.priority(), policy.status(), "DECISIVE"));
            }
            return verdict("RED", List.of(leading.policyId()), traces, true, leading.actionCode(),
                    List.of(leading.reasonCode()), List.of(), List.of(), List.of(), List.of(),
                    List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    null, "OK", auditMismatches);
        }

        if (!redMatched.isEmpty()) {
            TriageRule leading = redMatched.get(0);
            for (TriageRule rule : redMatched) {
                traces.add(new MatchedRuleTrace(rule.ruleId(), rule.outcome(), rule.priority(),
                        rule.ruleVersion(), rule == leading ? "DECISIVE" : "CONCURRING"));
            }
            for (TriageRule rule : matched) {
                if (!"RED".equals(rule.outcome())) {
                    traces.add(new MatchedRuleTrace(rule.ruleId(), rule.outcome(), rule.priority(),
                            rule.ruleVersion(), "SUPPRESSED_BY_HIGHER_SEVERITY"));
                }
            }
            List<String> decisiveIds = redMatched.stream().map(TriageRule::ruleId).toList();
            List<String> reasons = redMatched.stream().map(TriageRule::reasonCode).distinct().toList();
            return verdict("RED", decisiveIds, traces, true, leading.actionCode(),
                    reasons, List.of(), unionRequiredFields(redMatched), List.of(), List.of(),
                    List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    null, "OK", auditMismatches);
        }

        // --- 3. pending RED --------------------------------------------------------
        if (!pendingRed.isEmpty()) {
            boolean exhausted = questionRound >= registry.maximumQuestionRounds();
            String compReason = exhausted
                    ? CompletionReason.UNRESOLVED_HIGH_RISK_SIGNAL.name()
                    : CompletionReason.DATA_REQUIRED.name();
            String readiness = exhausted ? "BLOCKED_CLINICAL_DISPOSITION" : "OK";
            return verdict(
                    "NEEDS_MORE_INFO",
                    List.of(),
                    traces,
                    exhausted,
                    exhausted ? ROUTE_TO_HEALTHCARE_WORKER : ASK_CLARIFYING_QUESTIONS,
                    List.of("UNRESOLVED_RED_SIGNAL"),
                    unionQuestionIds(pendingRed),
                    unionRequiredFields(pendingRed),
                    List.of(),
                    pendingRed.stream().map(TriageRule::ruleId).toList(),
                    new ArrayList<>(unresolved),
                    conflicts,
                    safetyScreenStatus,
                    contextDatasetStatus,
                    greenEligibilityStatus,
                    scopeStatus,
                    pendingRisks,
                    primaryPending,
                    compReason,
                    readiness,
                    auditMismatches);
        }

        // --- 4. YELLOW -------------------------------------------------------------
        if (!yellowMatched.isEmpty()) {
            TriageRule leading = yellowMatched.get(0);
            for (TriageRule rule : yellowMatched) {
                traces.add(new MatchedRuleTrace(rule.ruleId(), rule.outcome(), rule.priority(),
                        rule.ruleVersion(), rule == leading ? "DECISIVE" : "CONCURRING"));
            }
            List<String> decisiveIds = yellowMatched.stream().map(TriageRule::ruleId).toList();
            List<String> reasons = yellowMatched.stream().map(TriageRule::reasonCode).distinct().toList();
            return verdict("YELLOW", decisiveIds, traces, leading.stopOnMatch(), leading.actionCode(),
                    reasons, unionQuestionIds(yellowMatched), unionRequiredFields(yellowMatched), List.of(),
                    List.of(), List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    null, "OK", auditMismatches);
        }

        // --- 5. OUT_OF_SCOPE & SCOPE CONFLICT ---------------------------------------
        TriageRule outOfScope = engineRules.get(PREDICATE_OUT_OF_SCOPE);
        if (ScopeStatus.CONFIRMED_OUT_OF_SCOPE.name().equals(scopeStatus) && outOfScope != null) {
            traces.add(new MatchedRuleTrace(outOfScope.ruleId(), outOfScope.outcome(),
                    outOfScope.priority(), outOfScope.ruleVersion(), "DECISIVE"));
            return verdict("OUT_OF_SCOPE", List.of(outOfScope.ruleId()), traces, outOfScope.stopOnMatch(),
                    outOfScope.actionCode(), List.of(outOfScope.reasonCode()), List.of(), List.of(), List.of(),
                    List.of(), List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    null, "OK", auditMismatches);
        } else if (ScopeStatus.CONFLICTED.name().equals(scopeStatus)) {
            List<String> confList = new ArrayList<>(conflicts);
            confList.add("SCOPE_CLASSIFICATION_CONFLICT");
            List<String> reasons = new ArrayList<>();
            reasons.add("SCOPE_CLASSIFICATION_CONFLICT");
            reasons.addAll(conflicts);
            return verdict("NEEDS_MORE_INFO", List.of(), traces, questionRound >= registry.maximumQuestionRounds(),
                    ASK_CLARIFYING_QUESTIONS, reasons.stream().distinct().toList(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), confList, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    CompletionReason.SCOPE_CONFLICT.name(), "OK", auditMismatches);
        }

        // --- 6. NEEDS_MORE_INFO ----------------------------------------------------
        List<TriageRule> needsInfo = new ArrayList<>(matched.stream()
                .filter(rule -> "NEEDS_MORE_INFO".equals(rule.outcome()))
                .toList());

        TriageRule missingFieldsRule = engineRules.get(PREDICATE_MISSING_FIELDS);
        if (missingFieldsRule != null && !DatasetStatus.COMPLETE.name().equals(greenEligibilityStatus)) {
            needsInfo.add(missingFieldsRule);
        }
        needsInfo = TriageRuleRegistry.sortedByPrecedence(needsInfo);

        if (!needsInfo.isEmpty() || !blockers.isEmpty() || !conflicts.isEmpty() || !DatasetStatus.COMPLETE.name().equals(safetyScreenStatus)) {
            boolean exhausted = questionRound >= registry.maximumQuestionRounds();
            TriageRule leading = needsInfo.isEmpty() ? null : needsInfo.get(0);
            for (TriageRule rule : needsInfo) {
                traces.add(new MatchedRuleTrace(rule.ruleId(), rule.outcome(), rule.priority(),
                        rule.ruleVersion(), rule == leading ? "DECISIVE" : "CONCURRING"));
            }
            LinkedHashSet<String> reasons = new LinkedHashSet<>();
            for (TriageRule rule : needsInfo) {
                reasons.add(rule.reasonCode());
            }
            for (TriageGreenBlocker blocker : blockers) {
                reasons.add(blocker.reasonCode());
            }
            reasons.addAll(conflicts);
            if (DatasetStatus.INCOMPLETE.name().equals(safetyScreenStatus)) {
                reasons.add("SAFETY_SCREEN_INCOMPLETE");
            }
            String action = ASK_CLARIFYING_QUESTIONS;
            String compReason = CompletionReason.DATA_REQUIRED.name();
            if (exhausted) {
                action = ROUTE_TO_HEALTHCARE_WORKER;
                reasons.add("MAX_QUESTION_ROUNDS_EXHAUSTED");
                compReason = DatasetStatus.INCOMPLETE.name().equals(safetyScreenStatus)
                        ? CompletionReason.UNRESOLVED_GLOBAL_SAFETY_SCREEN.name()
                        : CompletionReason.MAX_QUESTION_ROUNDS_REACHED.name();
            }
            LinkedHashSet<String> questions = new LinkedHashSet<>(unionQuestionIds(needsInfo));
            for (TriageGreenBlocker blocker : blockers) {
                questions.addAll(blocker.questionIds());
            }
            List<String> greenBlocked = blockers.stream().map(TriageGreenBlocker::blockerId).toList();
            List<String> decisiveIds = needsInfo.stream().map(TriageRule::ruleId).toList();
            List<String> pendingRedIds = pendingRed.stream().map(TriageRule::ruleId).toList();
            return verdict("NEEDS_MORE_INFO", decisiveIds, traces, exhausted, action,
                    new ArrayList<>(reasons), new ArrayList<>(questions), unionRequiredFields(needsInfo),
                    greenBlocked, pendingRedIds, new ArrayList<>(unresolved), conflicts, safetyScreenStatus,
                    contextDatasetStatus, greenEligibilityStatus, scopeStatus, pendingRisks,
                    primaryPending, compReason, "OK", auditMismatches);
        }

        // --- 7. GREEN --------------------------------------------------------------
        TriageRule green = engineRules.get(PREDICATE_GREEN);
        if (green != null && DatasetStatus.COMPLETE.name().equals(greenEligibilityStatus)) {
            if (!registry.greenEnabled()) {
                traces.add(new MatchedRuleTrace(green.ruleId(), green.outcome(), green.priority(),
                        green.ruleVersion(), "SUPPRESSED_BY_RELEASE_GATE"));
                return verdict("NEEDS_MORE_INFO", List.of(), traces, true, ROUTE_TO_HEALTHCARE_WORKER,
                        List.of("GREEN_RELEASE_GATE_DISABLED"), List.of(), List.of(), List.of(), List.of(),
                        List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                        greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                        CompletionReason.RULESET_COVERAGE_LIMITATION.name(), "OK", auditMismatches);
            }
            traces.add(new MatchedRuleTrace(green.ruleId(), green.outcome(), green.priority(),
                    green.ruleVersion(), "DECISIVE"));
            return verdict("GREEN", List.of(green.ruleId()), traces, green.stopOnMatch(), green.actionCode(),
                    List.of(green.reasonCode()), List.of(), List.of(), List.of(), List.of(),
                    List.of(), conflicts, safetyScreenStatus, contextDatasetStatus,
                    greenEligibilityStatus, scopeStatus, pendingRisks, primaryPending,
                    null, "OK", auditMismatches);
        }

        // --- 8. Default Fallback ----------------------------------------------------
        return verdict("NEEDS_MORE_INFO", List.of(), traces, true, ROUTE_TO_HEALTHCARE_WORKER,
                List.of("RULESET_CANNOT_STRATIFY"), List.of(), List.of(), List.of(), List.of(), List.of(),
                conflicts, safetyScreenStatus, contextDatasetStatus, greenEligibilityStatus,
                scopeStatus, pendingRisks, primaryPending,
                CompletionReason.RULESET_COVERAGE_LIMITATION.name(), "OK", auditMismatches);
    }

    private String calcSafetyScreenStatus(Map<String, Object> signals) {
        boolean hasConflicted = false;
        boolean allResolved = true;
        for (String code : GLOBAL_SAFETY_SIGNAL_CODES) {
            Presence pres = Presence.parse(signals.get(code));
            if (pres == Presence.CONFLICTED) {
                hasConflicted = true;
            }
            if (pres == Presence.UNKNOWN || pres == Presence.CONFLICTED || pres == Presence.UNAWARE_OR_UNMEASURABLE) {
                allResolved = false;
            }
        }
        if (hasConflicted) return DatasetStatus.CONFLICTED.name();
        if (allResolved) return DatasetStatus.COMPLETE.name();
        return DatasetStatus.INCOMPLETE.name();
    }

    private String calcContextDatasetStatus(String stage, Map<String, Object> context) {
        if (!TriageRuleRegistry.V2_STAGES.contains(stage)) {
            return DatasetStatus.INCOMPLETE.name();
        }
        if ("PREGNANCY".equals(stage) && context.get("postpartum_day") != null) {
            return DatasetStatus.CONFLICTED.name();
        }
        if ("POSTPARTUM_MOTHER".equals(stage) && context.get("gestational_week") != null) {
            return DatasetStatus.CONFLICTED.name();
        }
        if ("POSTPARTUM_MOTHER".equals(stage) && "YES".equals(Objects.toString(context.get("possible_pregnancy"), ""))) {
            return DatasetStatus.CONFLICTED.name();
        }

        List<String> reqFields = switch (stage) {
            case "PRECONCEPTION" -> List.of("stage");
            case "POSSIBLE_PREGNANCY" -> List.of("stage", "possible_pregnancy");
            case "PREGNANCY" -> List.of("stage", "gestational_week");
            case "POSTPARTUM_MOTHER" -> List.of("stage", "postpartum_day");
            default -> List.of();
        };
        for (String field : reqFields) {
            if ("stage".equals(field)) continue;
            Object val = context.get(field);
            if (val == null || "UNKNOWN".equals(val)) {
                return DatasetStatus.INCOMPLETE.name();
            }
        }
        return DatasetStatus.COMPLETE.name();
    }

    private String calcGreenEligibilityStatus(
            String stage, Map<String, Object> signals, Map<String, Object> context,
            String safetyScreenStatus, String contextDatasetStatus, List<TriageGreenBlocker> blockers) {
        if (!DatasetStatus.COMPLETE.name().equals(safetyScreenStatus) || !DatasetStatus.COMPLETE.name().equals(contextDatasetStatus)) {
            return (DatasetStatus.CONFLICTED.name().equals(safetyScreenStatus) || DatasetStatus.CONFLICTED.name().equals(contextDatasetStatus))
                    ? DatasetStatus.CONFLICTED.name()
                    : DatasetStatus.INCOMPLETE.name();
        }
        List<String> reqGreen = switch (stage) {
            case "PRECONCEPTION" -> List.of("stage", "possible_pregnancy");
            case "POSSIBLE_PREGNANCY" -> List.of("stage", "possible_pregnancy", "last_menstrual_period_or_test");
            case "PREGNANCY" -> List.of("stage", "gestational_week", "pain_severity", "bleeding_amount");
            case "POSTPARTUM_MOTHER" -> List.of("stage", "postpartum_day", "delivery_method", "bleeding_amount");
            default -> List.of();
        };
        for (String fName : reqGreen) {
            if ("stage".equals(fName)) continue;
            Object val = context.get(fName);
            if (val == null || "UNKNOWN".equals(val)) {
                return DatasetStatus.INCOMPLETE.name();
            }
        }
        if (!blockers.isEmpty()) {
            return DatasetStatus.INCOMPLETE.name();
        }
        return DatasetStatus.COMPLETE.name();
    }

    private String calcScopeStatus(
            String stage, Map<String, Object> signals, Map<String, Object> context,
            String safetyScreenStatus, boolean reproductiveRelevanceHint, boolean hasReproductiveContext,
            boolean policyHits, boolean redHits, boolean pendingRedHits, boolean yellowHits, boolean blockers) {
        if (!reproductiveRelevanceHint && hasReproductiveContext) {
            return ScopeStatus.CONFLICTED.name();
        }
        boolean canBeOos = DatasetStatus.COMPLETE.name().equals(safetyScreenStatus) &&
                !policyHits && !redHits && !pendingRedHits && !yellowHits &&
                !hasReproductiveContext && !blockers &&
                !"UNKNOWN".equals(Objects.toString(context.get("possible_pregnancy"), "")) &&
                !reproductiveRelevanceHint;
        if (canBeOos) {
            return ScopeStatus.CONFIRMED_OUT_OF_SCOPE.name();
        }
        if (Set.of("POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM_MOTHER").contains(stage) || hasReproductiveContext) {
            return ScopeStatus.POSSIBLY_IN_SCOPE.name();
        }
        return ScopeStatus.IN_SCOPE.name();
    }

    private List<String> calcPendingRisks(
            String safetyScreenStatus, String contextDatasetStatus, boolean pendingRed, boolean blockers) {
        List<String> risks = new ArrayList<>();
        if (pendingRed) {
            risks.add(PendingRiskStatus.UNRESOLVED_RED_CONDITION.name());
        }
        if (DatasetStatus.INCOMPLETE.name().equals(safetyScreenStatus)) {
            risks.add(PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.name());
        }
        if (blockers) {
            risks.add(PendingRiskStatus.UNRESOLVED_SAFETY_BLOCKER.name());
        }
        if (DatasetStatus.INCOMPLETE.name().equals(contextDatasetStatus)) {
            risks.add(PendingRiskStatus.UNRESOLVED_CONTEXT.name());
        }
        return risks;
    }

    private String determinePrimaryPendingRisk(List<String> pendingRisks) {
        List<String> order = List.of(
                PendingRiskStatus.UNRESOLVED_RED_CONDITION.name(),
                PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.name(),
                PendingRiskStatus.UNRESOLVED_SAFETY_BLOCKER.name(),
                PendingRiskStatus.UNRESOLVED_CONTEXT.name());
        for (String target : order) {
            if (pendingRisks.contains(target)) {
                return target;
            }
        }
        return null;
    }

    private record ExclusionResult(boolean isExcluded, String reason, List<String> missingFields) {}

    private ExclusionResult checkExclusion(TriageRule rule, Map<String, Object> context, List<String> conflicts) {
        for (String predicate : rule.exclusionPredicates()) {
            if ("BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED".equals(predicate)) {
                Object amount = context.get("bleeding_amount");
                if (amount == null || "UNKNOWN".equals(amount)) {
                    return new ExclusionResult(true, SuppressionReason.DATA_AMBIGUITY.name(), List.of("bleeding_amount"));
                } else if ("CONFLICTED".equals(amount)) {
                    return new ExclusionResult(true, SuppressionReason.DATA_CONFLICT.name(), List.of("bleeding_amount"));
                }
            } else if ("CONTEXT_RESOLUTION_CONFLICTED".equals(predicate)) {
                if (conflicts.contains("CONTEXT_RESOLUTION_CONFLICTED")) {
                    return new ExclusionResult(true, SuppressionReason.DATA_CONFLICT.name(), List.of());
                }
            }
        }
        return new ExclusionResult(false, null, List.of());
    }

    private List<TriageGreenBlocker> activeBlockers(
            String stage, Map<String, Object> signals, Map<String, Object> context, List<String> conflicts) {
        List<TriageGreenBlocker> hits = new ArrayList<>();
        for (TriageGreenBlocker blocker : registry.blockersForStage(stage)) {
            if (blocker.condition() != null) {
                if (RuleConditionEvaluator.evaluate(blocker.condition(), signals, context) == Tri.TRUE) {
                    hits.add(blocker);
                }
            } else if ("BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED".equals(blocker.predicate())) {
                Object amount = context.get("bleeding_amount");
                if ("UNKNOWN".equals(amount) || "CONFLICTED".equals(amount)) {
                    hits.add(blocker);
                }
            } else if ("CONTEXT_RESOLUTION_CONFLICTED".equals(blocker.predicate())) {
                if (conflicts.contains("CONTEXT_RESOLUTION_CONFLICTED")) {
                    hits.add(blocker);
                }
            }
        }
        return hits;
    }

    private List<String> reproductiveEvidence(Map<String, Object> signals, Map<String, Object> context) {
        List<String> evidence = new ArrayList<>();
        for (Map.Entry<String, Object> entry : signals.entrySet()) {
            if (REPRODUCTIVE_SIGNALS.contains(entry.getKey())) {
                Presence pres = Presence.parse(entry.getValue());
                if (pres == Presence.PRESENT) {
                    evidence.add(entry.getKey());
                }
            }
        }
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (REPRODUCTIVE_CONTEXT_FIELDS.contains(entry.getKey())) {
                Object val = entry.getValue();
                if (val != null && !"UNKNOWN".equals(val) && !"NONE".equals(val) && !"NO".equals(val)) {
                    evidence.add(entry.getKey());
                }
            }
        }
        return evidence.stream().distinct().toList();
    }

    private List<String> unionRequiredFields(List<TriageRule> rules) {
        return rules.stream()
                .flatMap(r -> r.requiredFields().stream())
                .distinct()
                .toList();
    }

    private List<String> unionQuestionIds(List<TriageRule> rules) {
        return rules.stream()
                .flatMap(r -> r.questionIds().stream())
                .distinct()
                .toList();
    }

    private List<String> collectUnresolvedSignals(JsonNode condition, Map<String, Object> signals) {
        List<String> result = new ArrayList<>();
        collectUnresolvedRec(condition, signals, result);
        return result.stream().distinct().toList();
    }

    private void collectUnresolvedRec(JsonNode node, Map<String, Object> signals, List<String> result) {
        if (node == null || !node.isObject()) return;
        if (node.has("signal")) {
            String signalCode = node.get("signal").asText();
            Presence pres = Presence.parse(signals.get(signalCode));
            if (pres == Presence.UNKNOWN || pres == Presence.UNAWARE_OR_UNMEASURABLE) {
                result.add(signalCode);
            }
        } else if (node.has("all")) {
            for (JsonNode child : node.get("all")) collectUnresolvedRec(child, signals, result);
        } else if (node.has("any")) {
            for (JsonNode child : node.get("any")) collectUnresolvedRec(child, signals, result);
        } else if (node.has("not")) {
            collectUnresolvedRec(node.get("not"), signals, result);
        }
    }

    private RuleEvaluation verdict(
            String outcome,
            List<String> decisiveRuleIds,
            List<MatchedRuleTrace> traces,
            boolean stop,
            String action,
            List<String> reasons,
            List<String> questions,
            List<String> required,
            List<String> greenBlocked,
            List<String> pendingRed,
            List<String> unresolved,
            List<String> conflicts,
            String safetyStatus,
            String contextStatus,
            String greenStatus,
            String scopeStatus,
            List<String> pendingRisks,
            String primaryPending,
            String compReason,
            String readiness,
            List<Map<String, Object>> auditMismatches) {
        return new RuleEvaluation(
                outcome,
                decisiveRuleIds,
                traces,
                "RED".equals(outcome) || stop,
                action,
                reasons,
                questions,
                required,
                registry.rulesetVersion(),
                registry.rulesetSha256(),
                greenBlocked,
                pendingRed,
                unresolved,
                conflicts,
                safetyStatus,
                contextStatus,
                greenStatus,
                scopeStatus,
                pendingRisks,
                primaryPending,
                compReason,
                auditMismatches,
                readiness);
    }
}
