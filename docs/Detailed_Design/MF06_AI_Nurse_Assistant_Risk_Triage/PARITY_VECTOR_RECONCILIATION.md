# Parity Vector Reconciliation

**Date:** 2026-08-05
**Shared artifact:** `05_Development/Contracts/triage/triage_rule_parity_vectors_v2.json`
**Ruleset:** `2.1.0`

Earlier reports quoted "23 shared vectors" and "41 shared vectors" in different places, and separately quoted "87 Python tests" and "54 Java tests" without saying what those numbers counted. This document pins each number to a measured value.

**The "23" figure is retracted.** It was correct when the file held 23 records; the file has since grown to 41 and the report was not updated. Nothing was filtered out — the number was simply stale.

---

## 1. Measured counts

| Metric | Python | Java | Shared artifact | Explanation |
|---|---|---|---|---|
| JSON vector records | — | — | **41** | `len(document["vectors"])` |
| Distinct vector ids | — | — | **41** | no duplicate ids |
| Active vector records | — | — | **41** | the file has no `disabled`/`skip` flag; every record is active |
| Parity invocations | **41** | **41** | 41 | one invocation per record on both sides |
| Non-parity tests in the same class/module | **46** | **13** | — | tri-state, registry, green-lock, renderer, planner (Python); registry, tri-state, green-lock, tie-break (Java) |
| Reported class/module total | **87** | **54** | — | 41 + 46 = 87; 41 + 13 = 54 |

## 2. How each side enumerates

**Python** — `tests/test_rule_registry_parity_v2.py`

```python
@pytest.mark.parametrize("index", range(len(_VECTORS)))
def test_parity_vector(registry, vectors, index):
    vector = vectors[index]
```

Indexes the whole list; the range is derived from the file, so adding a record automatically adds an invocation. A separate guard asserts `len(vectors) == 41`, which fails loudly if the file changes without the count being reviewed.

Measured: `pytest -k parity_vector --collect-only` → **41/87 collected (46 deselected)**.

**Java** — `TriageRuleParityV2Test`

```java
@ParameterizedTest(name = "{0}")
@MethodSource("vectors")
void javaEvaluatorMatchesSharedVector(JsonNode vector) { … }
```

`vectors()` streams every element of `document.get("vectors")`. No predicate, no limit.

Measured: surefire reports 54 tests for the class; the class declares 13 `@Test` methods, so parity invocations = 54 − 13 = **41**.

## 3. Coverage questions answered

| Question | Answer |
|---|---|
| Any vector run by Python but not Java? | **No.** Both enumerate the full list with no filter. |
| Any filtering or predicate? | **No.** |
| Any duplicate expansion (one record → many invocations)? | **No.** One record → exactly one invocation per runtime. |
| Assertions per vector identical? | **No — see §4.** |

## 4. Assertion asymmetry (the one real difference)

Both sides assert the four mandatory fields identically: `outcome`, `decisiveRuleIds` (exact order), `stopConversation`, `actionCode`.

The optional expectation fields are asserted **only when the vector declares them**, and the two runtimes use slightly different containment semantics:

| Optional field | Python | Java |
|---|---|---|
| `reasonCodes` | `assert reason in evaluation.reason_codes` per item | `containsAll(...)` |
| `greenBlockedBy` | per item | `containsAll(...)` |
| `pendingRedRuleIds` | per item | `containsAll(...)` |
| `unresolvedSignals` | per item | `containsAll(...)` |
| `suppressedRuleIds` | per item | `containsAll(...)` |

These are equivalent in effect (both are "contains", not "equals"), but neither side asserts the **absence** of extra entries in those lists. A runtime could therefore emit an additional reason code or blocker without failing parity.

**Limitation recorded, not fixed in this session:** parity currently proves the four mandatory fields match exactly and that the declared optional entries are present. It does not prove the optional lists are identical between runtimes.

## 5. Parity PASS definition

Parity is reported PASS only when **both runtimes execute all 41 active shared vector records** and all mandatory-field assertions hold. That condition is met today: Python 41/41, Java 41/41.
