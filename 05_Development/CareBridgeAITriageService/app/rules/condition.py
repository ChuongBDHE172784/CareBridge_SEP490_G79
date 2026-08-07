"""Condition DSL: a fixed, non-executable tree evaluated with THREE-VALUED logic.

There is deliberately no expression parser and no ``eval``. A condition is a tree of
``all`` / ``any`` / ``not`` nodes over leaves that read either an extracted clinical
signal or a resolved context field.

Why three-valued: data the user has not given is UNKNOWN, not FALSE. Collapsing the
two lets ``not(missing)`` evaluate to true, which would let an unanswered question
"prove" a symptom is absent. Kleene semantics keep that impossible:

    TRUE  AND UNKNOWN = UNKNOWN        TRUE  OR UNKNOWN = TRUE
    FALSE AND UNKNOWN = FALSE          FALSE OR UNKNOWN = UNKNOWN
    NOT UNKNOWN       = UNKNOWN

A rule fires only on TRUE. UNKNOWN never fires a rule, but — unlike FALSE — it is
reported back so the engine can block GREEN and ask instead of reassuring.

Signal presence is richer than a boolean:
    PRESENT    user confirmed, or a measurement crossed a defined threshold
    ABSENT     user explicitly denied
    UNKNOWN    not asked, or answered "not sure"
    CONFLICTED user gave contradictory answers across turns
CONFLICTED evaluates as UNKNOWN for matching and is surfaced as a data conflict.
"""

from __future__ import annotations

from enum import Enum
from typing import Any, Mapping

OPERATORS = frozenset(
    {"EQ", "NEQ", "GT", "GTE", "LT", "LTE", "IN", "EXISTS", "NOT_EXISTS"}
)

ENGINE_PREDICATES = frozenset(
    {
        "MISSING_REQUIRED_FIELDS",
        "NO_REPRODUCTIVE_RELEVANCE_AND_NO_GLOBAL_RED",
        "MINIMUM_DATASET_COMPLETE_AND_NO_HIGHER_RULE_MATCHED",
    }
)

EXCLUSION_PREDICATES = frozenset(
    {"BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED", "CONTEXT_RESOLUTION_CONFLICTED"}
)


class Tri(Enum):
    """Kleene three-valued logic."""

    TRUE = "TRUE"
    FALSE = "FALSE"
    UNKNOWN = "UNKNOWN"

    def __and__(self, other: "Tri") -> "Tri":
        if self is Tri.FALSE or other is Tri.FALSE:
            return Tri.FALSE
        if self is Tri.UNKNOWN or other is Tri.UNKNOWN:
            return Tri.UNKNOWN
        return Tri.TRUE

    def __or__(self, other: "Tri") -> "Tri":
        if self is Tri.TRUE or other is Tri.TRUE:
            return Tri.TRUE
        if self is Tri.UNKNOWN or other is Tri.UNKNOWN:
            return Tri.UNKNOWN
        return Tri.FALSE

    def __invert__(self) -> "Tri":
        if self is Tri.UNKNOWN:
            return Tri.UNKNOWN
        return Tri.FALSE if self is Tri.TRUE else Tri.TRUE


class Presence(str, Enum):
    PRESENT = "PRESENT"
    ABSENT = "ABSENT"
    UNKNOWN = "UNKNOWN"
    CONFLICTED = "CONFLICTED"
    #: The user cannot answer at all — no blood-pressure cuff, no thermometer, no way to
    #: tell. Logically identical to UNKNOWN (never inferred as ABSENT), but kept distinct
    #: so the Question Planner stops re-asking for a number and pivots to symptoms the
    #: person can actually perceive.
    UNAWARE_OR_UNMEASURABLE = "UNAWARE_OR_UNMEASURABLE"


#: Presence values that carry no answer and must never be read as a denial.
UNRESOLVED_PRESENCES = frozenset(
    {Presence.UNKNOWN, Presence.CONFLICTED, Presence.UNAWARE_OR_UNMEASURABLE}
)


class ConditionSchemaError(ValueError):
    """Raised when a condition tree does not match the closed DSL schema."""


def validate_condition(node: Any, *, path: str = "condition") -> None:
    """Validate a condition tree, raising ``ConditionSchemaError`` on any deviation."""

    if not isinstance(node, Mapping):
        raise ConditionSchemaError(f"{path}: expected an object, got {type(node).__name__}")

    keys = set(node.keys())

    if keys == {"all"} or keys == {"any"}:
        key = next(iter(keys))
        children = node[key]
        if not isinstance(children, list) or not children:
            raise ConditionSchemaError(f"{path}.{key}: expected a non-empty array")
        for index, child in enumerate(children):
            validate_condition(child, path=f"{path}.{key}[{index}]")
        return

    if keys == {"not"}:
        validate_condition(node["not"], path=f"{path}.not")
        return

    if keys == {"signal", "operator", "value"} or keys == {"context", "operator", "value"}:
        reference_key = "signal" if "signal" in keys else "context"
        reference = node[reference_key]
        if not isinstance(reference, str) or not reference:
            raise ConditionSchemaError(f"{path}.{reference_key}: expected a non-empty string")
        operator = node["operator"]
        if operator not in OPERATORS:
            raise ConditionSchemaError(f"{path}.operator: unsupported operator {operator!r}")
        if operator == "IN" and not isinstance(node["value"], list):
            raise ConditionSchemaError(f"{path}.value: IN requires an array")
        return

    raise ConditionSchemaError(f"{path}: unrecognised node with keys {sorted(keys)}")


def validate_engine_predicate(node: Any, *, path: str = "condition") -> None:
    """Validate an ENGINE-type condition, which names one hand-written predicate."""

    if not isinstance(node, Mapping) or set(node.keys()) != {"predicate"}:
        raise ConditionSchemaError(f"{path}: expected an object with exactly a 'predicate' key")
    predicate = node["predicate"]
    if predicate not in ENGINE_PREDICATES:
        raise ConditionSchemaError(f"{path}.predicate: unknown predicate {predicate!r}")


def signal_presence(signals: Mapping[str, Any], code: str) -> Presence:
    """Read a signal's presence, tolerating the plain-boolean shorthand used in tests."""

    raw = signals.get(code)
    if raw is None:
        return Presence.UNKNOWN
    if isinstance(raw, bool):
        return Presence.PRESENT if raw else Presence.ABSENT
    if isinstance(raw, Presence):
        return raw
    if isinstance(raw, Mapping):
        value = raw.get("presence")
        try:
            return Presence(value)
        except ValueError:
            return Presence.UNKNOWN
    if isinstance(raw, str):
        try:
            return Presence(raw)
        except ValueError:
            return Presence.UNKNOWN
    return Presence.UNKNOWN


def evaluate_condition(
    node: Mapping[str, Any],
    signals: Mapping[str, Any],
    context: Mapping[str, Any],
) -> Tri:
    """Evaluate a validated condition tree under Kleene three-valued logic."""

    if "all" in node:
        result = Tri.TRUE
        for child in node["all"]:
            result = result & evaluate_condition(child, signals, context)
        return result
    if "any" in node:
        result = Tri.FALSE
        for child in node["any"]:
            result = result | evaluate_condition(child, signals, context)
        return result
    if "not" in node:
        return ~evaluate_condition(node["not"], signals, context)

    operator = node["operator"]

    if "signal" in node:
        presence = signal_presence(signals, node["signal"])
        return _signal_leaf(operator, presence, node["value"])

    return _context_leaf(operator, context, node["context"], node["value"])


def collect_leaf_states(
    node: Mapping[str, Any],
    signals: Mapping[str, Any],
    context: Mapping[str, Any],
) -> tuple[tuple[str, ...], tuple[str, ...]]:
    """Return ``(satisfied_signals, unresolved_signals)`` for one condition tree.

    Used when a rule evaluates to UNKNOWN: it tells the engine that, say,
    SEVERE_HEADACHE is already TRUE while VISUAL_DISTURBANCE is merely unanswered.
    A case like that must never sit in UNKNOWN — it either drives the next question or,
    once the round budget is gone, escalates to a healthcare worker.
    """

    satisfied: list[str] = []
    unresolved: list[str] = []

    def walk(current: Mapping[str, Any]) -> None:
        for key in ("all", "any"):
            if key in current:
                for child in current[key]:
                    walk(child)
                return
        if "not" in current:
            walk(current["not"])
            return
        if "signal" not in current:
            return
        code = current["signal"]
        state = evaluate_condition(current, signals, context)
        if state is Tri.TRUE:
            satisfied.append(code)
        elif state is Tri.UNKNOWN:
            unresolved.append(code)

    walk(node)
    return tuple(dict.fromkeys(satisfied)), tuple(dict.fromkeys(unresolved))


def _signal_leaf(operator: str, presence: Presence, expected: Any) -> Tri:
    if operator == "EXISTS":
        return Tri.TRUE if presence not in UNRESOLVED_PRESENCES else Tri.UNKNOWN
    if operator == "NOT_EXISTS":
        # NOT_EXISTS reports "we never captured a value" — it is NOT proof of absence.
        return Tri.TRUE if presence in UNRESOLVED_PRESENCES else Tri.FALSE
    if presence in UNRESOLVED_PRESENCES:
        # Unanswered, contradictory, or unmeasurable — all three are "no answer".
        return Tri.UNKNOWN

    actual = presence is Presence.PRESENT
    if operator == "EQ":
        return Tri.TRUE if actual is bool(expected) else Tri.FALSE
    if operator == "NEQ":
        return Tri.TRUE if actual is not bool(expected) else Tri.FALSE
    if operator == "IN":
        return Tri.TRUE if any(actual is bool(candidate) for candidate in expected) else Tri.FALSE
    # Numeric comparisons are meaningless against a presence flag.
    return Tri.FALSE


def _context_leaf(
    operator: str, context: Mapping[str, Any], code: str, expected: Any
) -> Tri:
    present = code in context and context[code] is not None
    actual = context.get(code)

    if operator == "NOT_EXISTS":
        return Tri.TRUE if not present else Tri.FALSE
    if operator == "EXISTS":
        return Tri.TRUE if present else Tri.UNKNOWN
    if not present:
        return Tri.UNKNOWN
    # A context enum whose value is literally UNKNOWN is unanswered data, except when
    # the rule explicitly tests for the string "UNKNOWN" (PRE_INFO_001 does exactly that).
    if actual == "UNKNOWN" and expected != "UNKNOWN":
        return Tri.UNKNOWN
    if actual == "CONFLICTED":
        return Tri.UNKNOWN

    if operator == "EQ":
        return _bool(_equals(actual, expected))
    if operator == "NEQ":
        return _bool(not _equals(actual, expected))
    if operator == "IN":
        return _bool(any(_equals(actual, candidate) for candidate in expected))
    return _compare(operator, actual, expected)


def _bool(value: bool) -> Tri:
    return Tri.TRUE if value else Tri.FALSE


def _equals(actual: Any, expected: Any) -> bool:
    # bool is a subclass of int in Python; keep the spaces disjoint so a numeric 1
    # never satisfies a boolean `true` leaf.
    if isinstance(expected, bool) or isinstance(actual, bool):
        return isinstance(actual, bool) and isinstance(expected, bool) and actual is expected
    return actual == expected


def _compare(operator: str, actual: Any, expected: Any) -> Tri:
    if isinstance(actual, bool) or isinstance(expected, bool):
        return Tri.FALSE
    if not isinstance(actual, (int, float)) or not isinstance(expected, (int, float)):
        return Tri.UNKNOWN
    if operator == "GT":
        return _bool(actual > expected)
    if operator == "GTE":
        return _bool(actual >= expected)
    if operator == "LT":
        return _bool(actual < expected)
    if operator == "LTE":
        return _bool(actual <= expected)
    return Tri.FALSE
