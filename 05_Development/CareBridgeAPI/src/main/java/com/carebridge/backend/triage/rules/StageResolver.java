package com.carebridge.backend.triage.rules;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage resolution and entity–stage validation.
 *
 * <p>Two jobs in one place because they are the same question asked twice: which stage
 * applies, and is that stage even possible for the entity we resolved?
 *
 * <p>A stage that does not belong to the entity is not a rounding error.
 * {@code targetEntity=BABY} with {@code stage=PREGNANCY} means the engine is about to reason
 * about a pregnancy belonging to someone who is not the subject of the session. That is
 * CONFLICTED, never silently corrected.
 *
 * <p>Behavioural parity with {@code app/context/stage_resolver.py}.
 */
@Component
public class StageResolver {

    private static final String NUMBER_WORD =
            "(?:khong|mot|hai|ba|bon|tu|nam|sau|bay|tam|chin|muoi|linh|le|lam)";
    private static final String NUMBER =
            "(?:\\d{1,2}|" + NUMBER_WORD + "(?:\\s+" + NUMBER_WORD + "){0,3})";
    private static final Pattern GESTATIONAL_WEEK = Pattern.compile(
            "\\b(?:bau|mang thai)(?:\\s+(?:duoc|khoang|gan|hon))?\\s+"
                    + "(?<number>" + NUMBER + ")\\s+tuan\\b");
    private static final Pattern BABY_AGE_MONTHS = Pattern.compile(
            "(?:\\b(?:be|con)(?:\\s+(?:nha\\s+em|cua\\s+(?:toi|em)))?"
                    + "(?:\\s+(?:duoc|moi|da|hien))?\\s+(?<subjectNumber>" + NUMBER + ")"
                    + "\\s+thang(?:\\s+tuoi)?\\b|"
                    + "\\b(?<ageNumber>" + NUMBER + ")\\s+thang\\s+tuoi\\b)");
    private static final Pattern POSTPARTUM = Pattern.compile(
            "\\b(?:sau sinh|hau san|moi sinh|sinh em be duoc)\\b");
    private static final Pattern NEGATED_STAGE_PREFIX = Pattern.compile(
            "(?:^|\\s)(?:khong|chua)(?:\\s+phai)?(?:\\s+o)?"
                    + "(?:\\s+giai\\s+doan)?\\s*$");
    private static final Map<String, Integer> ONES = Map.ofEntries(
            Map.entry("khong", 0), Map.entry("mot", 1), Map.entry("hai", 2),
            Map.entry("ba", 3), Map.entry("bon", 4), Map.entry("tu", 4),
            Map.entry("nam", 5), Map.entry("lam", 5), Map.entry("sau", 6),
            Map.entry("bay", 7), Map.entry("tam", 8), Map.entry("chin", 9));

    public record StageResolution(CareStage stage, ResolutionSource source, List<String> conflicts) {
        public boolean isResolved() {
            return stage.isResolved();
        }
    }

    public record ContextStatus(ContextResolutionStatus status, List<String> conflicts) {
    }

    public StageResolution resolve(
            TargetEntity entity,
            CareStage explicitStage,
            String legacyStageName,
            CareStage journeyStage,
            Integer babyAgeMonths,
            Integer gestationalWeek,
            Integer postpartumDay) {
        return resolve(entity, explicitStage, legacyStageName, journeyStage, babyAgeMonths,
                gestationalWeek, postpartumDay, null);
    }

    public StageResolution resolve(
            TargetEntity entity,
            CareStage explicitStage,
            String legacyStageName,
            CareStage journeyStage,
            Integer babyAgeMonths,
            Integer gestationalWeek,
            Integer postpartumDay,
            String latestUserMessage) {
        return resolve(entity, explicitStage, legacyStageName, journeyStage, babyAgeMonths,
                gestationalWeek, postpartumDay, latestUserMessage, null);
    }

    public StageResolution resolve(
            TargetEntity entity,
            CareStage explicitStage,
            String legacyStageName,
            CareStage journeyStage,
            Integer babyAgeMonths,
            Integer gestationalWeek,
            Integer postpartumDay,
            String latestUserMessage,
            List<String> submittedOptionCodes) {

        if (!entity.isResolved()) {
            // Without a subject a stage is meaningless — PREGNANCY for whom?
            return new StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE, List.of());
        }

        Set<CareStage> clarified = new LinkedHashSet<>();
        if (submittedOptionCodes != null) {
            for (String code : submittedOptionCodes) {
                CareStage stage = switch (code) {
                    case "STAGE_PRECONCEPTION" -> CareStage.PRECONCEPTION;
                    case "STAGE_POSSIBLE_PREGNANCY" -> CareStage.POSSIBLE_PREGNANCY;
                    case "STAGE_PREGNANCY" -> CareStage.PREGNANCY;
                    case "STAGE_POSTPARTUM_MOTHER" -> CareStage.POSTPARTUM_MOTHER;
                    default -> null;
                };
                if (stage != null) clarified.add(stage);
            }
        }
        if (clarified.size() > 1) {
            return conflicted(ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                    "STAGE_CLARIFICATION_ANSWERS_CONFLICT");
        }
        if (!clarified.isEmpty()) {
            CareStage stage = clarified.iterator().next();
            if (!CareStage.isValidFor(entity, stage)) {
                return conflicted(ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                        "STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + stage);
            }
            return new StageResolution(stage, ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                    List.of());
        }

        if (explicitStage != null && explicitStage.isResolved()) {
            if (!CareStage.isValidFor(entity, explicitStage)) {
                return conflicted(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                        "STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + explicitStage);
            }
            return new StageResolution(explicitStage, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                    List.of());
        }

        StageResolution latest = stageFromLatestMessage(entity, latestUserMessage);
        if (latest != null) {
            return latest;
        }

        if (legacyStageName != null && !legacyStageName.isBlank()) {
            CareStage mapped = CareStage.mapLegacy(legacyStageName, entity);
            if (mapped == null) {
                // Legacy POSTPARTUM with a BABY target lands here on purpose: the name is
                // ambiguous and guessing would pick the wrong subject's stage.
                return conflicted(ResolutionSource.EXPLICIT_SELECTED_PROFILE,
                        "LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY:" + entity + "/" + legacyStageName);
            }
            return new StageResolution(mapped, ResolutionSource.EXPLICIT_SELECTED_PROFILE, List.of());
        }

        if (journeyStage != null && journeyStage.isResolved()) {
            if (!CareStage.isValidFor(entity, journeyStage)) {
                return conflicted(ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                        "JOURNEY_STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + journeyStage);
            }
            return new StageResolution(journeyStage, ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                    List.of());
        }

        // Derive from measurements only as a last resort, within the entity's own range.
        if (entity == TargetEntity.BABY && babyAgeMonths != null) {
            if (babyAgeMonths < 0 || babyAgeMonths >= 24) {
                return conflicted(ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                        "BABY_AGE_OUT_OF_SUPPORTED_RANGE:" + babyAgeMonths);
            }
            CareStage derived = babyAgeMonths < 12 ? CareStage.INFANT_0_12M : CareStage.TODDLER_12_24M;
            return new StageResolution(derived, ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
        }

        if (entity == TargetEntity.MOTHER) {
            if (gestationalWeek != null && postpartumDay != null) {
                // Both cannot be true at once; do not pick the more convenient one.
                return conflicted(ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                        "GESTATIONAL_WEEK_AND_POSTPARTUM_DAY_BOTH_PRESENT");
            }
            if (gestationalWeek != null) {
                return new StageResolution(CareStage.PREGNANCY,
                        ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
            }
            if (postpartumDay != null) {
                return new StageResolution(CareStage.POSTPARTUM_MOTHER,
                        ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
            }
        }

        return new StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE, List.of());
    }

    private static StageResolution stageFromLatestMessage(
            TargetEntity entity, String latestUserMessage) {
        String folded = TargetEntityResolver.fold(latestUserMessage);
        if (folded.isEmpty()) {
            return null;
        }

        Set<CareStage> stages = new LinkedHashSet<>();
        List<String> conflicts = new ArrayList<>();
        if (entity == TargetEntity.MOTHER) {
            Matcher weeks = GESTATIONAL_WEEK.matcher(folded);
            while (weeks.find()) {
                if (!isNegated(folded, weeks.start())
                        && parseVietnameseNumber(weeks.group("number")) != null) {
                    stages.add(CareStage.PREGNANCY);
                }
            }
            Matcher postpartum = POSTPARTUM.matcher(folded);
            while (postpartum.find()) {
                if (!isNegated(folded, postpartum.start())) {
                    stages.add(CareStage.POSTPARTUM_MOTHER);
                }
            }
        } else if (entity == TargetEntity.BABY) {
            Matcher ages = BABY_AGE_MONTHS.matcher(folded);
            while (ages.find()) {
                String number = ages.group("subjectNumber") != null
                        ? ages.group("subjectNumber") : ages.group("ageNumber");
                Integer months = parseVietnameseNumber(number);
                if (months == null) {
                    continue;
                }
                if (months >= 24) {
                    conflicts.add("BABY_AGE_OUT_OF_SUPPORTED_RANGE:" + months);
                } else {
                    stages.add(months < 12 ? CareStage.INFANT_0_12M : CareStage.TODDLER_12_24M);
                }
            }
        }

        if (!conflicts.isEmpty() || stages.size() > 1) {
            List<String> codes = new ArrayList<>();
            codes.add("LATEST_MESSAGE_STAGE_CONFLICT");
            codes.addAll(conflicts);
            return new StageResolution(CareStage.CONFLICTED,
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.copyOf(codes));
        }
        if (!stages.isEmpty()) {
            return new StageResolution(stages.iterator().next(),
                    ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, List.of());
        }
        return null;
    }

    private static boolean isNegated(String message, int matchStart) {
        return NEGATED_STAGE_PREFIX.matcher(message.substring(0, matchStart)).find();
    }

    private static Integer parseVietnameseNumber(String value) {
        if (value.matches("\\d{1,2}")) {
            return Integer.parseInt(value);
        }
        String[] tokens = value.split("\\s+");
        if (tokens.length == 1) {
            return "muoi".equals(tokens[0]) ? 10 : ONES.get(tokens[0]);
        }
        if ("muoi".equals(tokens[0])) {
            Integer tail = tailValue(tokens, 1);
            return tail == null ? null : 10 + tail;
        }
        if (tokens.length >= 2 && "muoi".equals(tokens[1]) && ONES.containsKey(tokens[0])) {
            int number = ONES.get(tokens[0]) * 10;
            Integer tail = tailValue(tokens, 2);
            return tail == null && tokens.length > 2 ? null : number + (tail == null ? 0 : tail);
        }
        return null;
    }

    private static Integer tailValue(String[] tokens, int start) {
        for (int index = start; index < tokens.length; index++) {
            if (!Set.of("linh", "le").contains(tokens[index])) {
                return ONES.get(tokens[index]);
            }
        }
        return null;
    }

    /** Conflict codes for an entity–stage pair; empty when the pair is coherent. */
    public List<String> validateEntityStage(TargetEntity entity, CareStage stage) {
        if (!entity.isResolved() || !stage.isResolved()) {
            return List.of();
        }
        if (CareStage.isValidFor(entity, stage)) {
            return List.of();
        }
        return List.of("STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + stage);
    }

    /**
     * Combine the three resolutions into one status. A conflict is reported before a mere gap,
     * because a contradiction cannot be fixed by asking one more question — the user must choose.
     */
    public ContextStatus resolveContextStatus(
            TargetEntity entity, CareStage stage, IntentType intent, List<String> extraConflicts) {

        List<String> conflicts = new ArrayList<>();
        if (extraConflicts != null) {
            conflicts.addAll(extraConflicts);
        }
        conflicts.addAll(validateEntityStage(entity, stage));
        if (entity == TargetEntity.CONFLICTED) conflicts.add("TARGET_ENTITY_CONFLICTED");
        if (stage == CareStage.CONFLICTED) conflicts.add("CARE_STAGE_CONFLICTED");
        if (intent == IntentType.CONFLICTED) conflicts.add("INTENT_CONFLICTED");

        if (!conflicts.isEmpty()) {
            return new ContextStatus(ContextResolutionStatus.CONFLICTED, List.copyOf(conflicts));
        }
        if (!entity.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_TARGET_ENTITY, List.of());
        }
        if (!intent.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_INTENT, List.of());
        }
        if (!intent.mayProduceTriageOutcome()) {
            // A general or source question needs no stage; it is resolved as it stands.
            return new ContextStatus(ContextResolutionStatus.RESOLVED, List.of());
        }
        if (!stage.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_STAGE, List.of());
        }
        return new ContextStatus(ContextResolutionStatus.RESOLVED, List.of());
    }

    private static StageResolution conflicted(ResolutionSource source, String code) {
        return new StageResolution(CareStage.CONFLICTED, source, List.of(code));
    }
}
