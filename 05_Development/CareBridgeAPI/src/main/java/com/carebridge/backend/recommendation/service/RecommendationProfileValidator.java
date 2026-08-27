package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Strict, privacy-safe JSON V1 validator and deterministic signal mapper. */
@Component
public class RecommendationProfileValidator {

    private static final MathContext BMI_CONTEXT = MathContext.DECIMAL128;
    private static final Set<String> ROOT_ACCEPT_KEYS = Set.of(
            "submissionId", "schemaVersion", "policyVersion", "consentAccepted", "profile");
    private static final Set<String> ROOT_DECLINE_KEYS = Set.of(
            "submissionId", "schemaVersion", "policyVersion", "consentAccepted");
    private static final Set<String> LIFESTYLE_KEYS = Set.of("smoking", "alcohol", "physicalActivity", "sleep", "flags");
    private static final Set<String> LIFESTYLE_FLAG_CODES = Set.of("SUBSTANCE_USE", "STRESS", "UNHEALTHY_DIET");
    private static final Set<String> BMI_KEYS = Set.of("state", "heightCm", "weightKg", "weightContext", "measuredOn");
    private static final Set<String> VACCINATION_KEYS = Set.of("answers", "flags");
    private static final Set<String> VACCINATION_FLAG_CODES = Set.of("NOT_ASSESSED");
    private static final Set<String> VACCINATION_ANSWER_KEYS = Set.of("code", "state", "value");
    private static final Set<String> STI_KEYS = Set.of("state", "status", "infectionCodes");
    private static final Set<String> BASIC_STATE_KEYS = Set.of("state");
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public RecommendationProfileValidator(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    public RecommendationProfileValidator(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ValidatedRecommendationProfile validateAccept(JsonNode root, JourneyType stage, LocalDate dateOfBirth) {
        ensureObject(root, "body");
        ensureKeys(root, ROOT_ACCEPT_KEYS, "body");
        UUID submissionId = uuid(root, "submissionId");
        exactInteger(root, "schemaVersion", RecommendationConstants.SCHEMA_VERSION);
        exactText(root, "policyVersion", RecommendationConstants.POLICY_VERSION);
        if (!root.path("consentAccepted").isBoolean() || !root.path("consentAccepted").asBoolean()) {
            throw RecommendationException.invalid("consentAccepted", "must be true for an accepted profile");
        }
        JsonNode profileNode = required(root, "profile", "profile");
        ensureObject(profileNode, "profile");
        ensureKeys(profileNode, RecommendationConstants.DOMAINS, "profile");
        if (containsExplicitNull(profileNode)) {
            throw RecommendationException.invalid("profile", "JSON null is not allowed; omit an undisclosed field");
        }

        LocalDate businessDate = LocalDate.now(clock.withZone(RecommendationConstants.BUSINESS_ZONE));
        Map<String, Object> profile = new LinkedHashMap<>();
        Map<String, Object> derived = new LinkedHashMap<>();
        Set<String> signals = new LinkedHashSet<>();

        validateAge(profileNode.get("age"), dateOfBirth, businessDate, profile, derived, signals);
        validateBmi(profileNode.get("bmi"), stage, businessDate, profile, derived, signals);
        validateCodeSetDomain(profileNode.get("reproductiveHistory"), "reproductiveHistory",
                new LinkedHashSet<>(java.util.stream.Stream.concat(
                        RecommendationConstants.REPRODUCTIVE_SIGNALS.keySet().stream(),
                        java.util.stream.Stream.of("NO_LISTED_REPRODUCTIVE_HISTORY")).toList()),
                Set.of("NO_PRIOR_PREGNANCY", "NO_LISTED_REPRODUCTIVE_HISTORY"),
                RecommendationConstants.REPRODUCTIVE_SIGNALS, profile, signals);
        validateCodeSetDomain(profileNode.get("underlyingConditions"), "underlyingConditions",
                new LinkedHashSet<>(java.util.stream.Stream.concat(
                        RecommendationConstants.CONDITION_SIGNALS.keySet().stream(),
                        java.util.stream.Stream.of("NONE_KNOWN")).toList()), Set.of("NONE_KNOWN"),
                RecommendationConstants.CONDITION_SIGNALS, profile, signals);
        validateLifestyle(profileNode.get("lifestyle"), profile, signals);
        validateCodeSetDomain(profileNode.get("nutrition"), "nutrition",
                new LinkedHashSet<>(java.util.stream.Stream.concat(
                        RecommendationConstants.NUTRITION_SIGNALS.keySet().stream(),
                        java.util.stream.Stream.of("NO_CURRENT_CONCERN")).toList()),
                Set.of("NO_CURRENT_CONCERN"), RecommendationConstants.NUTRITION_SIGNALS, profile, signals);
        Object normalizedNutrition = profile.get("nutrition");
        if (normalizedNutrition instanceof Map<?, ?> nutrition
                && nutrition.get("codes") instanceof List<?> codes
                && codes.contains("VEGETARIAN") && codes.contains("VEGAN")) {
            throw RecommendationException.invalid("profile.nutrition.codes", "vegetarian and vegan values are mutually exclusive");
        }
        validateVaccination(profileNode.get("vaccination"), profile, signals);
        validateCodeSetDomain(profileNode.get("currentMedications"), "currentMedications",
                new LinkedHashSet<>(java.util.stream.Stream.concat(
                        RecommendationConstants.MEDICATION_SIGNALS.keySet().stream(),
                        java.util.stream.Stream.of("NONE")).toList()),
                Set.of("NONE"),
                RecommendationConstants.MEDICATION_SIGNALS, profile, signals);
        validateSexualHealth(profileNode.get("sexualHealth"), profile, signals);
        validateSti(profileNode.get("sti"), profile, signals);

        Map<String, Object> normalizedProfile = castMap(canonicalize(profile));
        String canonicalJson;
        try {
            canonicalJson = objectMapper.writeValueAsString(canonicalize(normalizedProfile));
        } catch (JsonProcessingException ex) {
            throw RecommendationException.contextUnavailable();
        }
        return new ValidatedRecommendationProfile(
                submissionId, normalizedProfile, castMap(canonicalize(derived)), Set.copyOf(signals), canonicalJson);
    }

    public UUID validateDecline(JsonNode root) {
        ensureObject(root, "body");
        ensureKeys(root, ROOT_DECLINE_KEYS, "body");
        UUID id = uuid(root, "submissionId");
        exactInteger(root, "schemaVersion", RecommendationConstants.SCHEMA_VERSION);
        exactText(root, "policyVersion", RecommendationConstants.POLICY_VERSION);
        if (!root.path("consentAccepted").isBoolean() || root.path("consentAccepted").asBoolean()) {
            throw RecommendationException.invalid("consentAccepted", "must be false for a decline");
        }
        return id;
    }

    private void validateAge(JsonNode node, LocalDate dob, LocalDate today, Map<String, Object> profile,
                             Map<String, Object> derived, Set<String> signals) {
        if (dob != null && dob.isAfter(today)) {
            throw RecommendationException.invalid("account.dateOfBirth", "must not be in the future");
        }
        ensureObject(node, "profile.age");
        ensureAllowedKeys(node, BASIC_STATE_KEYS, "profile.age");
        String state = state(node, "profile.age", false);
        Map<String, Object> value = stateMap(state);
        if ("KNOWN".equals(state)) {
            if (dob == null || dob.isAfter(today)) throw RecommendationException.invalid("profile.age", "account date of birth is unavailable");
            int age = Period.between(dob, today).getYears();
            String band = age < 18 ? "UNDER_18" : age <= 24 ? "AGE_18_24" : age <= 34 ? "AGE_25_34" : age <= 39 ? "AGE_35_39" : "AGE_40_PLUS";
            derived.put("ageBand", band);
            signals.add(RecommendationConstants.AGE_SIGNALS.get(band));
        } else {
            derived.put("ageBand", null);
        }
        profile.put("age", value);
    }

    private void validateBmi(JsonNode node, JourneyType stage, LocalDate today, Map<String, Object> profile,
                             Map<String, Object> derived, Set<String> signals) {
        ensureObject(node, "profile.bmi");
        ensureAllowedKeys(node, BMI_KEYS, "profile.bmi");
        String state = state(node, "profile.bmi", false);
        Map<String, Object> value = stateMap(state);
        if (!"KNOWN".equals(state)) {
            rejectPresent(node, Set.of("heightCm", "weightKg", "weightContext", "measuredOn"), "profile.bmi");
            profile.put("bmi", value);
            derived.put("bmi", null);
            derived.put("bmiCategory", null);
            derived.put("bmiSignalEligible", false);
            return;
        }
        BigDecimal height = decimal(node, "heightCm", "profile.bmi.heightCm");
        BigDecimal weight = decimal(node, "weightKg", "profile.bmi.weightKg");
        if (height.compareTo(BigDecimal.valueOf(100)) < 0 || height.compareTo(BigDecimal.valueOf(250)) > 0) {
            throw RecommendationException.invalid("profile.bmi.heightCm", "must be between 100.0 and 250.0 cm");
        }
        if (weight.compareTo(BigDecimal.valueOf(20)) < 0 || weight.compareTo(BigDecimal.valueOf(300)) > 0) {
            throw RecommendationException.invalid("profile.bmi.weightKg", "must be between 20.0 and 300.0 kg");
        }
        LocalDate measuredOn = date(node, "measuredOn", "profile.bmi.measuredOn");
        if (measuredOn.isAfter(today)) throw RecommendationException.invalid("profile.bmi.measuredOn", "must not be in the future");
        String context = textEnum(node, "weightContext", "profile.bmi.weightContext",
                Set.of("PRE_PREGNANCY", "CURRENT_NON_PREGNANT", "CURRENT_PREGNANCY", "CURRENT_POSTPARTUM"));
        Set<String> allowed = switch (stage) {
            case PRE_PREGNANCY -> Set.of("PRE_PREGNANCY", "CURRENT_NON_PREGNANT");
            case PREGNANCY -> Set.of("PRE_PREGNANCY", "CURRENT_PREGNANCY");
            case POSTPARTUM -> Set.of("PRE_PREGNANCY", "CURRENT_POSTPARTUM");
            case BABY_CARE -> Set.of();
        };
        if (!allowed.contains(context)) throw RecommendationException.invalid("profile.bmi.weightContext", "context is not valid for the current stage");
        BigDecimal bmi = weight.divide(height.divide(BigDecimal.valueOf(100), BMI_CONTEXT).pow(2, BMI_CONTEXT), BMI_CONTEXT);
        String category = bmi.compareTo(BigDecimal.valueOf(18.5)) < 0 ? "UNDERWEIGHT"
                : bmi.compareTo(BigDecimal.valueOf(25)) < 0 ? "HEALTHY_RANGE"
                : bmi.compareTo(BigDecimal.valueOf(30)) < 0 ? "OVERWEIGHT" : "OBESITY";
        boolean eligible = "PRE_PREGNANCY".equals(context)
                || (stage == JourneyType.PRE_PREGNANCY && "CURRENT_NON_PREGNANT".equals(context));
        value.put("heightCm", height.setScale(1, RoundingMode.UNNECESSARY));
        value.put("weightKg", weight.setScale(1, RoundingMode.UNNECESSARY));
        value.put("weightContext", context);
        value.put("measuredOn", measuredOn.toString());
        profile.put("bmi", value);
        derived.put("bmi", bmi.setScale(2, RoundingMode.HALF_UP));
        derived.put("bmiCategory", eligible ? category : null);
        derived.put("bmiSignalEligible", eligible);
        if (eligible) signals.add(RecommendationConstants.BMI_SIGNALS.get(category));
    }

    private void validateCodeSetDomain(JsonNode node, String domain, Set<String> allowed, Set<String> exclusive,
                                       Map<String, String> signalMap, Map<String, Object> profile, Set<String> signals) {
        ensureObject(node, "profile." + domain);
        ensureAllowedKeys(node, Set.of("state", "codes"), "profile." + domain);
        String state = state(node, "profile." + domain, false);
        Map<String, Object> value = stateMap(state);
        if ("KNOWN".equals(state)) {
            List<String> codes = codeList(node, "codes", "profile." + domain + ".codes", allowed);
            if (codes.stream().anyMatch(exclusive::contains) && codes.size() > 1) {
                throw RecommendationException.invalid("profile." + domain + ".codes", "exclusive negative code cannot be combined");
            }
            value.put("codes", codes);
            codes.stream().map(signalMap::get).filter(java.util.Objects::nonNull).forEach(signals::add);
        } else {
            rejectPresent(node, Set.of("codes"), "profile." + domain);
        }
        profile.put(domain, value);
    }

    private void validateLifestyle(JsonNode node, Map<String, Object> profile, Set<String> signals) {
        ensureObject(node, "profile.lifestyle");
        ensureAllowedKeys(node, LIFESTYLE_KEYS, "profile.lifestyle");
        Map<String, Object> lifestyle = new LinkedHashMap<>();
        validateAnswer(node.get("smoking"), "profile.lifestyle.smoking", Set.of("NEVER", "FORMER", "CURRENT"), false, lifestyle, signals, "smoking");
        validateAnswer(node.get("alcohol"), "profile.lifestyle.alcohol", Set.of("NONE", "LESS_THAN_WEEKLY", "WEEKLY_OR_MORE", "ANY_USE"), false, lifestyle, signals, "alcohol");
        validateAnswer(node.get("physicalActivity"), "profile.lifestyle.physicalActivity", Set.of("LOW", "MODERATE", "HIGH"), true, lifestyle, signals, "physicalActivity");
        validateAnswer(node.get("sleep"), "profile.lifestyle.sleep", Set.of("NO_CONCERN", "CONCERN"), false, lifestyle, signals, "sleep");
        JsonNode flags = node.get("flags");
        if (flags != null && !flags.isNull()) {
            List<String> normalizedFlags = flagList(flags, "profile.lifestyle.flags", LIFESTYLE_FLAG_CODES);
            lifestyle.put("flags", normalizedFlags);
            normalizedFlags.stream().map(RecommendationConstants.LIFESTYLE_FLAG_SIGNALS::get)
                    .filter(java.util.Objects::nonNull).forEach(signals::add);
        }
        profile.put("lifestyle", lifestyle);
    }

    private void validateAnswer(JsonNode node, String path, Set<String> allowed, boolean allowNotApplicable,
                                Map<String, Object> parent, Set<String> signals, String key) {
        ensureObject(node, path);
        ensureKeys(node, Set.of("state", "value"), path, true);
        String state = state(node, path, allowNotApplicable);
        Map<String, Object> answer = stateMap(state);
        if ("KNOWN".equals(state)) {
            String value = textEnum(node, "value", path + ".value", allowed);
            answer.put("value", value);
            String slug = RecommendationConstants.tagFor(key, value);
            if (slug != null) signals.add(slug);
        } else {
            rejectPresent(node, Set.of("value"), path);
        }
        parent.put(key, answer);
    }

    private void validateVaccination(JsonNode node, Map<String, Object> profile, Set<String> signals) {
        ensureObject(node, "profile.vaccination");
        ensureAllowedKeys(node, VACCINATION_KEYS, "profile.vaccination");
        List<String> flags = node.has("flags") && !node.get("flags").isNull()
                ? flagList(node.get("flags"), "profile.vaccination.flags", VACCINATION_FLAG_CODES)
                : List.of();
        flags.stream().map(RecommendationConstants.VACCINATION_FLAG_SIGNALS::get)
                .filter(java.util.Objects::nonNull).forEach(signals::add);
        JsonNode answers = required(node, "answers", "profile.vaccination.answers");
        if (!answers.isArray() || answers.size() != RecommendationConstants.VACCINE_CODES.size()) throw RecommendationException.invalid("profile.vaccination.answers", "must contain exactly five vaccine answers");
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (JsonNode answerNode : answers) {
            ensureObject(answerNode, "profile.vaccination.answers[]");
            ensureKeys(answerNode, VACCINATION_ANSWER_KEYS, "profile.vaccination.answers[]", true);
            String code = textEnum(answerNode, "code", "profile.vaccination.answers[].code", RecommendationConstants.VACCINE_CODES);
            if (!seen.add(code)) throw RecommendationException.invalid("profile.vaccination.answers", "duplicate vaccine code");
            String state = state(answerNode, "profile.vaccination.answers[].state", true);
            Map<String, Object> normalizedAnswer = new LinkedHashMap<>();
            normalizedAnswer.put("code", code);
            normalizedAnswer.put("state", state);
            if ("KNOWN".equals(state)) {
                String vaccineStatus = textEnum(answerNode, "value", "profile.vaccination.answers[].value", Set.of("UP_TO_DATE", "DUE", "NOT_RECEIVED"));
                normalizedAnswer.put("value", vaccineStatus);
                if (!"UP_TO_DATE".equals(vaccineStatus)) signals.add(RecommendationConstants.VACCINATION_SIGNALS.get(code));
            } else {
                rejectPresent(answerNode, Set.of("value"), "profile.vaccination.answers[].value");
            }
            normalized.add(normalizedAnswer);
        }
        if (!seen.equals(RecommendationConstants.VACCINE_CODES)) throw RecommendationException.invalid("profile.vaccination.answers", "all five vaccine codes are required");
        if (flags.contains("NOT_ASSESSED") && normalized.stream().anyMatch(answer -> "KNOWN".equals(answer.get("state")))) {
            throw RecommendationException.invalid("profile.vaccination.flags", "not-assessed cannot contain known vaccine values");
        }
        normalized.sort(Comparator.comparing(value -> value.get("code").toString()));
        Map<String, Object> vaccination = new LinkedHashMap<>();
        vaccination.put("answers", normalized);
        if (!flags.isEmpty()) vaccination.put("flags", flags);
        profile.put("vaccination", vaccination);
    }

    private void validateSexualHealth(JsonNode node, Map<String, Object> profile, Set<String> signals) {
        ensureObject(node, "profile.sexualHealth");
        ensureKeys(node, Set.of("state", "codes"), "profile.sexualHealth", true);
        String state = state(node, "profile.sexualHealth", true);
        Map<String, Object> value = stateMap(state);
        if ("KNOWN".equals(state)) {
            List<String> codes = codeList(node, "codes", "profile.sexualHealth.codes",
                    new LinkedHashSet<>(java.util.stream.Stream.concat(
                            RecommendationConstants.SEXUAL_HEALTH_SIGNALS.keySet().stream(),
                            java.util.stream.Stream.of("NO_CURRENT_INFORMATION_NEED")).toList()));
            if (codes.contains("NO_CURRENT_INFORMATION_NEED") && codes.size() > 1) throw RecommendationException.invalid("profile.sexualHealth.codes", "no-current-need is exclusive");
            value.put("codes", codes);
            codes.stream().map(RecommendationConstants.SEXUAL_HEALTH_SIGNALS::get).filter(java.util.Objects::nonNull).forEach(signals::add);
        } else {
            rejectPresent(node, Set.of("codes"), "profile.sexualHealth");
        }
        profile.put("sexualHealth", value);
    }

    private void validateSti(JsonNode node, Map<String, Object> profile, Set<String> signals) {
        ensureObject(node, "profile.sti");
        ensureKeys(node, STI_KEYS, "profile.sti", true);
        String state = state(node, "profile.sti", true);
        Map<String, Object> value = stateMap(state);
        if ("KNOWN".equals(state)) {
            String status = textEnum(node, "status", "profile.sti.status",
                    Set.of("NO_KNOWN_HISTORY", "SCREENING_INFORMATION", "PAST_HISTORY", "CURRENT_OR_UNDER_TREATMENT", "AT_RISK", "SUSPECTED_OR_KNOWN"));
            value.put("status", status);
            String statusSignal = RecommendationConstants.STI_STATUS_SIGNALS.get(status);
            if (statusSignal != null) signals.add(statusSignal);
            if (status.equals("PAST_HISTORY") || status.equals("CURRENT_OR_UNDER_TREATMENT")) {
                List<String> infections = codeList(node, "infectionCodes", "profile.sti.infectionCodes", RecommendationConstants.STI_INFECTION_CODES);
                if (infections.size() > 9) throw RecommendationException.invalid("profile.sti.infectionCodes", "must contain at most nine codes");
                value.put("infectionCodes", infections);
                infections.stream().map(RecommendationConstants.STI_INFECTION_SIGNALS::get).forEach(signals::add);
            } else {
                rejectPresent(node, Set.of("infectionCodes"), "profile.sti");
            }
        } else {
            rejectPresent(node, Set.of("status", "infectionCodes"), "profile.sti");
        }
        profile.put("sti", value);
    }

    private String state(JsonNode node, String path, boolean allowNotApplicable) {
        ensureObject(node, path);
        String state = textEnum(node, "state", path + ".state", allowNotApplicable ? RecommendationConstants.ANSWER_STATES : Set.of("KNOWN", "UNKNOWN", "PREFER_NOT_TO_SAY"));
        return state;
    }

    private Map<String, Object> stateMap(String state) {
        return new LinkedHashMap<>(Map.of("state", state));
    }

    private List<String> codeList(JsonNode node, String field, String path, Set<String> allowed) {
        JsonNode values = required(node, field, path);
        if (!values.isArray() || values.size() < 1 || values.size() > 20) throw RecommendationException.invalid(path, "must contain one to twenty codes");
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual() || !allowed.contains(value.textValue()) || !result.add(value.textValue())) throw RecommendationException.invalid(path, "contains an unknown or duplicate controlled code");
        }
        Collections.sort(result);
        return result;
    }

    private List<String> flagList(JsonNode values, String path, Set<String> allowed) {
        if (values == null || !values.isArray() || values.size() > 20) {
            throw RecommendationException.invalid(path, "must contain zero to twenty controlled flags");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual() || !allowed.contains(value.textValue()) || !result.add(value.textValue())) {
                throw RecommendationException.invalid(path, "contains an unknown or duplicate controlled flag");
            }
        }
        Collections.sort(result);
        return result;
    }

    private BigDecimal decimal(JsonNode node, String field, String path) {
        JsonNode value = required(node, field, path);
        if (!value.isNumber() || value.asText().contains("e") || value.asText().contains("E")) throw RecommendationException.invalid(path, "must be a decimal number with at most one fractional digit");
        String text = value.asText();
        int point = text.indexOf('.');
        if (point >= 0 && text.length() - point - 1 > 1) throw RecommendationException.invalid(path, "must have at most one fractional digit");
        return value.decimalValue();
    }

    private LocalDate date(JsonNode node, String field, String path) {
        String value = text(node, field, path);
        try { return LocalDate.parse(value); } catch (DateTimeParseException ex) { throw RecommendationException.invalid(path, "must be an ISO calendar date"); }
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field, field);
        try { return UUID.fromString(value); } catch (IllegalArgumentException ex) { throw RecommendationException.invalid(field, "must be a UUID"); }
    }

    private int exactInteger(JsonNode node, String field, int expected) {
        JsonNode value = required(node, field, field);
        if (!value.isInt() || value.intValue() != expected) {
            throw RecommendationException.conflict("RECOMMENDATION_POLICY_MISMATCH",
                    "The recommendation schema version is no longer current");
        }
        return expected;
    }

    private String exactText(JsonNode node, String field, String expected) {
        String value = text(node, field, field);
        if (!expected.equals(value)) {
            throw RecommendationException.conflict("RECOMMENDATION_POLICY_MISMATCH",
                    "The recommendation policy version is no longer current");
        }
        return expected;
    }

    private String textEnum(JsonNode node, String field, String path, Set<String> allowed) {
        JsonNode value = required(node, field, path);
        if (!value.isTextual() || !allowed.contains(value.textValue())) throw RecommendationException.invalid(path, "must use a controlled value");
        return value.textValue();
    }

    private String text(JsonNode node, String field, String path) {
        JsonNode value = required(node, field, path);
        if (!value.isTextual() || value.textValue().isBlank()) throw RecommendationException.invalid(path, "must be a nonblank value");
        return value.textValue();
    }

    private JsonNode required(JsonNode node, String field, String path) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) throw RecommendationException.invalid(path, "is required");
        return value;
    }

    private void ensureObject(JsonNode node, String path) {
        if (node == null || !node.isObject()) throw RecommendationException.invalid(path, "must be an object");
    }

    private void ensureKeys(JsonNode node, Set<String> expected, String path) {
        ensureKeys(node, expected, path, false);
    }

    private void ensureKeys(JsonNode node, Set<String> expected, String path, boolean allowOptional) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new HashSet<>(actual);
        extra.removeAll(expected);
        if (!allowOptional && !missing.isEmpty()) throw RecommendationException.invalid(path, "required field is missing");
        if (!extra.isEmpty()) throw RecommendationException.invalid(path, "contains an unsupported field");
    }

    private void ensureAllowedKeys(JsonNode node, Set<String> allowed, String path) {
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        actual.removeAll(allowed);
        if (!actual.isEmpty()) throw RecommendationException.invalid(path, "contains an unsupported field");
    }

    private void rejectPresent(JsonNode node, Set<String> fields, String path) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                throw RecommendationException.invalid(path + "." + field, "must be absent unless state is KNOWN");
            }
        }
    }

    private boolean containsExplicitNull(JsonNode node) {
        if (node == null || node.isNull()) return true;
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                if (containsExplicitNull(child)) return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) { return (Map<String, Object>) value; }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(this::canonicalize).toList();
        return value;
    }
}
