package com.carebridge.backend.triage.rules;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hard eligibility filter for the fixed question catalogue.
 *
 * <p>This is the gate that makes "never ask the wrong entity" an enforced property rather than
 * a convention. Everything the planner may consider passes through here first.
 *
 * <p>When the context is unresolved the filter collapses to clarification questions only, and
 * a caller passing symptom question ids cannot bypass that. Asking "bé mấy tháng tuổi?" of a
 * mother, or "bạn mang thai bao nhiêu tuần?" while assessing her newborn, is not a cosmetic
 * error — it tells the user the system has misunderstood who is unwell.
 *
 * <p>Behavioural parity with {@code app/questions/catalog_filter.py}.
 */
@Component
public class QuestionCatalogFilter {

    /** Everything the filter needs to know about the current turn. */
    public record FilterContext(
            TargetEntity targetEntity,
            CareStage stage,
            IntentType intent,
            ContextResolutionStatus contextStatus,
            Set<String> missingFields,
            Set<String> missingSignals,
            Set<String> answeredQuestionIds,
            Set<String> askedQuestionIds,
            Map<String, Object> signals,
            String safetyScreenStatus) {

        public FilterContext {
            safetyScreenStatus = safetyScreenStatus == null ? "INCOMPLETE" : safetyScreenStatus;
        }

        public static FilterContext of(
                TargetEntity entity, CareStage stage, IntentType intent,
                ContextResolutionStatus status, Set<String> missingFields,
                Set<String> missingSignals) {
            return new FilterContext(entity, stage, intent, status,
                    missingFields == null ? Set.of() : missingFields,
                    missingSignals == null ? Set.of() : missingSignals,
                    Set.of(), Set.of(), Map.of(), "INCOMPLETE");
        }
    }

    private final QuestionCatalog catalog;

    public QuestionCatalogFilter(QuestionCatalog catalog) {
        this.catalog = catalog;
    }

    /** Whether a single question may be asked in this context. */
    public boolean isEligible(QuestionCatalog.Question question, FilterContext context) {
        if (context.answeredQuestionIds().contains(question.questionId())
                || context.askedQuestionIds().contains(question.questionId())) {
            return false;
        }
        if (question.requiresResolvedTarget()) {
            if (!context.targetEntity().isResolved()) {
                return false;
            }
            if (!question.targetEntities().contains(context.targetEntity())) {
                // The core guard: a MOTHER-only question in a BABY session, or vice versa.
                return false;
            }
        }
        if (!question.applicableStages().isEmpty()
                && !question.applicableStages().contains(context.stage())) {
            return false;
        }
        if (question.requiresResolvedStage() && !context.stage().isResolved()) {
            return false;
        }
        if (!question.applicableIntents().isEmpty()
                && !question.applicableIntents().contains(context.intent())) {
            return false;
        }
        if (isUnmeasurable(question, context.signals())) {
            return false;
        }
        if (question.isClarification()) {
            // Selected by context status, not by missing fields — and only when the context
            // actually needs clarifying. Offering "whose symptom is this?" after the subject
            // is settled would just make the system look like it was not listening.
            return context.contextStatus().blocksSymptomQuestions();
        }
        return resolvesSomethingMissing(question, context);
    }

    /**
     * All questions askable in this context, highest priority first. While the context is
     * unresolved this returns clarification questions only, whatever the caller passed in.
     */
    public List<QuestionCatalog.Question> eligibleQuestions(
            FilterContext context, Collection<String> candidateQuestionIds) {

        List<String> clarifications = clarificationFor(context);
        if (!clarifications.isEmpty()) {
            List<QuestionCatalog.Question> forced = new ArrayList<>();
            for (String questionId : clarifications) {
                catalog.question(questionId)
                        .filter(question -> !context.answeredQuestionIds().contains(questionId))
                        .filter(question -> !context.askedQuestionIds().contains(questionId))
                        .ifPresent(forced::add);
            }
            catalog.questions().values().stream()
                    .filter(QuestionCatalog.Question::isGlobalDangerScreen)
                    .filter(QuestionCatalog.Question::mayRunWithoutResolvedTarget)
                    .filter(question -> !"COMPLETE".equals(context.safetyScreenStatus()))
                    .filter(question -> !context.answeredQuestionIds().contains(question.questionId()))
                    .filter(question -> !context.askedQuestionIds().contains(question.questionId()))
                    .filter(question -> forced.stream().noneMatch(existing ->
                            existing.questionId().equals(question.questionId())))
                    .forEach(forced::add);
            return List.copyOf(forced);
        }

        Collection<String> candidates = candidateQuestionIds == null || candidateQuestionIds.isEmpty()
                ? catalog.questions().keySet()
                : new LinkedHashSet<>(candidateQuestionIds);

        List<QuestionCatalog.Question> eligible = new ArrayList<>();
        for (String questionId : candidates) {
            catalog.question(questionId)
                    .filter(question -> isEligible(question, context))
                    .ifPresent(eligible::add);
        }
        eligible.sort(Comparator.comparingInt(QuestionCatalog.Question::priority)
                .thenComparing(QuestionCatalog.Question::questionId));
        return List.copyOf(eligible);
    }

    /** The only questions permitted while the context is unresolved. */
    private static List<String> clarificationFor(FilterContext context) {
        if (context.targetEntity() == TargetEntity.CONFLICTED) {
            // Both named: the user picks which to assess first. One session, one subject.
            return List.of("Q_CLARIFY_TARGET_FIRST");
        }
        return switch (context.contextStatus()) {
            case NEEDS_TARGET_ENTITY -> List.of("Q_CLARIFY_TARGET_ENTITY");
            case NEEDS_STAGE -> List.of(
                    context.targetEntity() == TargetEntity.MOTHER
                            ? "Q_CLARIFY_STAGE" : "Q_BABY_AGE_MONTHS");
            case NEEDS_INTENT -> List.of("Q_CLARIFY_INTENT");
            case CONFLICTED -> List.of("Q_CLARIFY_TARGET_FIRST");
            default -> List.of();
        };
    }

    private static boolean resolvesSomethingMissing(
            QuestionCatalog.Question question, FilterContext context) {
        for (String field : question.resolvesFields()) {
            if (context.missingFields().contains(field)) {
                return true;
            }
        }
        for (String signal : question.resolvesSignals()) {
            if (context.missingSignals().contains(signal)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnmeasurable(
            QuestionCatalog.Question question, Map<String, Object> signals) {
        if (!question.measurement() || signals == null || signals.isEmpty()) {
            return false;
        }
        List<String> codes = new ArrayList<>();
        codes.add(question.questionId());
        codes.addAll(question.resolvesSignals());
        codes.addAll(question.resolvesFields());
        for (String code : codes) {
            if (Presence.parse(signals.get(code)) == Presence.UNAWARE_OR_UNMEASURABLE) {
                return true;
            }
        }
        return false;
    }
}
