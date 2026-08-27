package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Condition DSL: a fixed, non-executable tree evaluated with three-valued logic.
 *
 * <p>No expression parser, no scripting engine — a malformed or hostile registry entry
 * cannot execute anything. Behavioural parity with {@code app/rules/condition.py}; the
 * shared parity vectors assert that both evaluators agree.
 */
public final class RuleConditionEvaluator {

    private static final Set<String> OPERATORS = Set.of(
            "EQ", "NEQ", "GT", "GTE", "LT", "LTE", "IN", "EXISTS", "NOT_EXISTS");

    static final Set<String> ENGINE_PREDICATES = Set.of(
            "MISSING_REQUIRED_FIELDS",
            "NO_REPRODUCTIVE_RELEVANCE_AND_NO_GLOBAL_RED",
            "MINIMUM_DATASET_COMPLETE_AND_NO_HIGHER_RULE_MATCHED");

    static final Set<String> EXCLUSION_PREDICATES = Set.of(
            "BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED", "CONTEXT_RESOLUTION_CONFLICTED");

    private RuleConditionEvaluator() {
    }

    public static final class ConditionSchemaException extends RuntimeException {
        public ConditionSchemaException(String message) {
            super(message);
        }
    }

    public static void validateCondition(JsonNode node, String path) {
        if (node == null || !node.isObject()) {
            throw new ConditionSchemaException(path + ": expected an object");
        }
        List<String> keys = fieldNames(node);

        if (keys.equals(List.of("all")) || keys.equals(List.of("any"))) {
            String key = keys.get(0);
            JsonNode children = node.get(key);
            if (!children.isArray() || children.isEmpty()) {
                throw new ConditionSchemaException(path + "." + key + ": expected a non-empty array");
            }
            for (int index = 0; index < children.size(); index++) {
                validateCondition(children.get(index), path + "." + key + "[" + index + "]");
            }
            return;
        }
        if (keys.equals(List.of("not"))) {
            validateCondition(node.get("not"), path + ".not");
            return;
        }

        boolean signalLeaf = keys.size() == 3 && keys.containsAll(List.of("signal", "operator", "value"));
        boolean contextLeaf = keys.size() == 3 && keys.containsAll(List.of("context", "operator", "value"));
        if (signalLeaf || contextLeaf) {
            String referenceKey = signalLeaf ? "signal" : "context";
            JsonNode reference = node.get(referenceKey);
            if (!reference.isTextual() || reference.asText().isEmpty()) {
                throw new ConditionSchemaException(path + "." + referenceKey + ": expected a non-empty string");
            }
            String operator = node.get("operator").asText("");
            if (!OPERATORS.contains(operator)) {
                throw new ConditionSchemaException(path + ".operator: unsupported operator " + operator);
            }
            if ("IN".equals(operator) && !node.get("value").isArray()) {
                throw new ConditionSchemaException(path + ".value: IN requires an array");
            }
            return;
        }
        throw new ConditionSchemaException(path + ": unrecognised node with keys " + keys);
    }

    public static void validateEnginePredicate(JsonNode node, String path) {
        if (node == null || !node.isObject() || !fieldNames(node).equals(List.of("predicate"))) {
            throw new ConditionSchemaException(path + ": expected an object with exactly a 'predicate' key");
        }
        String predicate = node.get("predicate").asText("");
        if (!ENGINE_PREDICATES.contains(predicate)) {
            throw new ConditionSchemaException(path + ".predicate: unknown predicate " + predicate);
        }
    }

    public static Tri evaluate(JsonNode node, Map<String, Object> signals, Map<String, Object> context) {
        if (node.has("all")) {
            Tri result = Tri.TRUE;
            for (JsonNode child : node.get("all")) {
                result = result.and(evaluate(child, signals, context));
            }
            return result;
        }
        if (node.has("any")) {
            Tri result = Tri.FALSE;
            for (JsonNode child : node.get("any")) {
                result = result.or(evaluate(child, signals, context));
            }
            return result;
        }
        if (node.has("not")) {
            return evaluate(node.get("not"), signals, context).negate();
        }

        String operator = node.get("operator").asText();
        if (node.has("signal")) {
            return signalLeaf(operator, Presence.parse(signals.get(node.get("signal").asText())),
                    node.get("value"));
        }
        return contextLeaf(operator, context, node.get("context").asText(), node.get("value"));
    }

    /**
     * Collect, for one condition tree, which signals are already TRUE and which are merely
     * unanswered. Used when a RED rule evaluates UNKNOWN: such a case must never be parked —
     * it drives the next question, or escalates once the round budget is gone.
     */
    public static LeafStates collectLeafStates(
            JsonNode node, Map<String, Object> signals, Map<String, Object> context) {
        Set<String> satisfied = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        walk(node, signals, context, satisfied, unresolved);
        return new LeafStates(List.copyOf(satisfied), List.copyOf(unresolved));
    }

    public record LeafStates(List<String> satisfied, List<String> unresolved) {
    }

    private static void walk(JsonNode node, Map<String, Object> signals, Map<String, Object> context,
                             Set<String> satisfied, Set<String> unresolved) {
        for (String key : List.of("all", "any")) {
            if (node.has(key)) {
                node.get(key).forEach(child -> walk(child, signals, context, satisfied, unresolved));
                return;
            }
        }
        if (node.has("not")) {
            walk(node.get("not"), signals, context, satisfied, unresolved);
            return;
        }
        if (!node.has("signal")) {
            return;
        }
        String code = node.get("signal").asText();
        Tri state = evaluate(node, signals, context);
        if (state == Tri.TRUE) {
            satisfied.add(code);
        } else if (state == Tri.UNKNOWN) {
            unresolved.add(code);
        }
    }

    private static Tri signalLeaf(String operator, Presence presence, JsonNode expected) {
        if ("EXISTS".equals(operator)) {
            return presence.isUnresolved() ? Tri.UNKNOWN : Tri.TRUE;
        }
        if ("NOT_EXISTS".equals(operator)) {
            // Reports "we never captured a value" — NOT proof of absence.
            return presence.isUnresolved() ? Tri.TRUE : Tri.FALSE;
        }
        if (presence.isUnresolved()) {
            return Tri.UNKNOWN;
        }
        boolean actual = presence == Presence.PRESENT;
        return switch (operator) {
            case "EQ" -> Tri.of(expected.isBoolean() && actual == expected.asBoolean());
            case "NEQ" -> Tri.of(!(expected.isBoolean() && actual == expected.asBoolean()));
            case "IN" -> {
                for (JsonNode candidate : expected) {
                    if (candidate.isBoolean() && actual == candidate.asBoolean()) yield Tri.TRUE;
                }
                yield Tri.FALSE;
            }
            // Numeric comparisons are meaningless against a presence flag.
            default -> Tri.FALSE;
        };
    }

    private static Tri contextLeaf(
            String operator, Map<String, Object> context, String code, JsonNode expected) {
        boolean present = context.containsKey(code) && context.get(code) != null;
        Object actual = present ? context.get(code) : null;

        if ("NOT_EXISTS".equals(operator)) {
            return Tri.of(!present);
        }
        if ("EXISTS".equals(operator)) {
            return present ? Tri.TRUE : Tri.UNKNOWN;
        }
        if (!present) {
            return Tri.UNKNOWN;
        }
        // A context enum literally valued UNKNOWN is unanswered data, unless the rule
        // explicitly tests for "UNKNOWN" (PRE_INFO_001 does exactly that).
        if ("UNKNOWN".equals(actual) && !(expected.isTextual() && "UNKNOWN".equals(expected.asText()))) {
            return Tri.UNKNOWN;
        }
        if ("CONFLICTED".equals(actual)) {
            return Tri.UNKNOWN;
        }

        return switch (operator) {
            case "EQ" -> Tri.of(equals(actual, expected));
            case "NEQ" -> Tri.of(!equals(actual, expected));
            case "IN" -> {
                for (JsonNode candidate : expected) {
                    if (equals(actual, candidate)) yield Tri.TRUE;
                }
                yield Tri.FALSE;
            }
            default -> compare(operator, actual, expected);
        };
    }

    private static boolean equals(Object actual, JsonNode expected) {
        // Booleans and numbers stay disjoint so a numeric 1 never satisfies a boolean leaf.
        if (expected.isBoolean() || actual instanceof Boolean) {
            return actual instanceof Boolean flag && expected.isBoolean() && flag == expected.asBoolean();
        }
        if (expected.isNumber() && actual instanceof Number number) {
            return number.doubleValue() == expected.asDouble();
        }
        if (expected.isTextual()) {
            return actual instanceof String text && text.equals(expected.asText());
        }
        return false;
    }

    private static Tri compare(String operator, Object actual, JsonNode expected) {
        if (actual instanceof Boolean || !expected.isNumber()) {
            return Tri.FALSE;
        }
        if (!(actual instanceof Number number)) {
            return Tri.UNKNOWN;
        }
        double left = number.doubleValue();
        double right = expected.asDouble();
        return switch (operator) {
            case "GT" -> Tri.of(left > right);
            case "GTE" -> Tri.of(left >= right);
            case "LT" -> Tri.of(left < right);
            case "LTE" -> Tri.of(left <= right);
            default -> Tri.FALSE;
        };
    }

    public static boolean satisfiedAtLeastOne(JsonNode node, Map<String, Object> signals, Map<String, Object> context) {
        if (node == null || !node.isObject()) return false;
        if (node.has("signal") || node.has("context")) {
            return evaluate(node, signals, context) == Tri.TRUE;
        }
        if (node.has("all")) {
            for (JsonNode child : node.get("all")) {
                if (satisfiedAtLeastOne(child, signals, context)) return true;
            }
        }
        if (node.has("any")) {
            for (JsonNode child : node.get("any")) {
                if (satisfiedAtLeastOne(child, signals, context)) return true;
            }
        }
        if (node.has("not")) {
            return evaluate(node.get("not"), signals, context) == Tri.FALSE;
        }
        return false;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        for (Iterator<String> iterator = node.fieldNames(); iterator.hasNext(); ) {
            names.add(iterator.next());
        }
        names.sort(String::compareTo);
        return names;
    }
}
