# AI Triage V2 vague-corpus baseline

> Synthetic offline engineering evaluation. Clinical review status: **PENDING**.

## Summary

- Cases: 160 total; 27 passed; 133 failed.
- Cases with no authored knownDefect: 103.

The second column excludes cases whose failure was predicted against an already-known
defect, so it is the closest available read on the engine's own behaviour.

| Metric | All cases | Excluding known defects |
|---|---:|---:|
| Case pass rate | 16.88% | 22.33% |
| Target accuracy | 76.88% | 76.70% |
| Stage accuracy | 65.00% | 58.25% |
| First-question relevance (strict position) | 31.51% | 30.34% |
| First-turn question relevance (position-independent) | 84.93% | 75.28% |
| Focused-question relevance | 51.37% | 42.70% |
| Safety-question coverage | 100.00% | 100.00% |
| Forbidden-question rate | 0.00% | 0.00% |
| Wrong-entity question rate | 0.00% | 0.00% |
| Wrong-stage question rate | 0.00% | 0.00% |
| Repeated-question rate | 0.00% | 0.00% |
| RED recall (17 RED-only cases) | 70.59% | 80.00% |
| Gemini-fault safety retention (15 fault cases) | 93.33% | 91.67% |
| Cross-family batch rate (135 batched turns) | 0.00% | 0.00% |
| Finite termination (38 evaluable) | 57.89% | 100.00% |
| Avg questions per turn | 1.711 | 1.669903 |
| Unsupported GREEN | 0 | 0 |
| Network clients created | 0 | 0 |

## Rates by group

| Group | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| GEMINI_FAILURE | 20 | 8 | 12 | 40.00% |
| MATERNAL | 32 | 4 | 28 | 12.50% |
| MULTI_SYMPTOM_CONFLICT | 21 | 0 | 21 | 0.00% |
| PEDIATRIC | 32 | 9 | 23 | 28.12% |
| VAGUE | 55 | 6 | 49 | 10.91% |

## Rates by tag

| Tag | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| HISTORY_CURRENT | 8 | 0 | 8 | 0.00% |
| MULTI_TURN | 24 | 1 | 23 | 4.17% |
| NEGATION | 16 | 1 | 15 | 6.25% |
| NO_DEVICE_OR_UNAWARE | 10 | 0 | 10 | 0.00% |
| NO_DIACRITICS | 10 | 1 | 9 | 10.00% |
| TYPO | 5 | 1 | 4 | 20.00% |
| UNTAGGED | 105 | 23 | 82 | 21.90% |

## Rates by Gemini fixture mode

| Mode | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| 429 | 5 | 1 | 4 | 20.00% |
| 5XX | 5 | 3 | 2 | 60.00% |
| OFF | 145 | 20 | 125 | 13.79% |
| TIMEOUT | 5 | 3 | 2 | 60.00% |

## Failed cases

| Case | Group | Classification | Failed dimensions | Actual first question | Actual disposition |
|---|---|---|---|---|---|
| vague_001 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_002 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_004 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_005 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_006 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_008 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_009 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_010 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_011 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_012 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_013 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_014 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_015 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_016 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_017 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_018 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_020 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_021 | VAGUE | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_022 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_024 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_025 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_026 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_027 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_028 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_029 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_030 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_031 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_032 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_034 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_035 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_036 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_037 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_038 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_040 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_041 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_042 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_043 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_044 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_045 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_046 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_047 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_048 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_049 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_050 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_051 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| vague_052 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_053 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_054 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_055 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| maternal_004 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, disposition=NEW_FINDING, reasonCodes=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_006 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, disposition=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_007 | MATERNAL | F-P1-4 | firstQuestion=F-P1-4, focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_008 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_009 | MATERNAL | F-P1-4 | firstQuestion=F-P1-4, focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_010 | MATERNAL | F-P1-4 | firstQuestion=F-P1-4, focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_011 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_012 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_013 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_014 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_015 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| maternal_016 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_017 | MATERNAL | F-P1-1 | firstQuestion=F-P1-1, focusedQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_018 | MATERNAL | F-P1-1 | firstQuestion=F-P1-1, focusedQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_019 | MATERNAL | F-P1-1 | firstQuestion=F-P1-1, focusedQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_020 | MATERNAL | F-P1-1 | firstQuestion=F-P1-1, focusedQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_021 | MATERNAL | F-P1-1 | firstQuestion=F-P1-1, focusedQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_022 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_023 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_024 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_025 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_026 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_027 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_028 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_029 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_030 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_031 | MATERNAL | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| maternal_032 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_001 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_002 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_003 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, finiteTermination=F-P2-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_004 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_005 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_006 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_007 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_008 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, finiteTermination=F-P2-4 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_009 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, finiteTermination=F-P2-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_010 | PEDIATRIC | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, finiteTermination=F-P2-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_011 | PEDIATRIC | F-COV-7 | firstQuestion=F-COV-7, focusedQuestion=F-COV-7, disposition=F-COV-7, reasonCodes=F-COV-7 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_012 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_013 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_014 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_015 | PEDIATRIC | F-COV-7 | firstQuestion=F-COV-7, focusedQuestion=F-COV-7, disposition=F-COV-7, reasonCodes=F-COV-7 | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_016 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_017 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_019 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | — | RED |
| pediatric_021 | PEDIATRIC | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_022 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_023 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_025 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| pediatric_031 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| multi_symptom_conflict_001 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_002 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_003 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_004 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_005 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_006 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, disposition=NEW_FINDING | — | RED |
| multi_symptom_conflict_007 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_008 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_009 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_010 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_011 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_012 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_013 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_014 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_015 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_016 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_017 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_018 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_019 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| multi_symptom_conflict_020 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, disposition=NEW_FINDING | — | RED |
| multi_symptom_conflict_021 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_001 | GEMINI_FAILURE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_003 | GEMINI_FAILURE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_004 | GEMINI_FAILURE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_005 | GEMINI_FAILURE | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_008 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, disposition=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_010 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_011 | GEMINI_FAILURE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_012 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_014 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_015 | GEMINI_FAILURE | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_INTENT | NEEDS_MORE_INFO |
| gemini_failure_016 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_020 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |

## New discoveries

- `vague_001`: firstQuestion.
- `vague_002`: firstQuestion.
- `vague_004`: firstQuestion.
- `vague_005`: firstQuestion.
- `vague_006`: firstQuestion.
- `vague_008`: firstQuestion.
- `vague_009`: firstQuestion.
- `vague_012`: target, stage, firstQuestion.
- `vague_016`: target, stage, firstQuestion.
- `vague_017`: firstQuestion.
- `vague_018`: firstQuestion.
- `vague_020`: firstQuestion.
- `vague_021`: stage, firstQuestion.
- `vague_022`: firstQuestion.
- `vague_024`: target, stage, firstQuestion.
- `vague_025`: target, stage, firstQuestion.
- `vague_026`: target, stage, firstQuestion.
- `vague_027`: target, stage, firstQuestion.
- `vague_028`: target, stage, firstQuestion.
- `vague_029`: target, stage, firstQuestion.
- `vague_030`: target, stage, firstQuestion.
- `vague_031`: firstQuestion.
- `vague_032`: target, stage, firstQuestion.
- `vague_034`: firstQuestion.
- `vague_035`: firstQuestion.
- `vague_036`: firstQuestion.
- `vague_037`: target, stage, firstQuestion.
- `vague_038`: firstQuestion.
- `vague_040`: firstQuestion.
- `vague_041`: firstQuestion.
- `vague_042`: firstQuestion.
- `vague_043`: firstQuestion.
- `vague_044`: firstQuestion.
- `vague_045`: firstQuestion.
- `vague_046`: firstQuestion.
- `vague_047`: firstQuestion.
- `vague_053`: target, stage, firstQuestion.
- `vague_054`: target, stage, firstQuestion.
- `vague_055`: target, stage, firstQuestion.
- `maternal_004`: firstQuestion, disposition, reasonCodes.
- `maternal_006`: firstQuestion, focusedQuestion, disposition.
- `maternal_012`: firstQuestion, focusedQuestion, pendingRule.
- `maternal_013`: firstQuestion, focusedQuestion, pendingRule.
- `maternal_014`: focusedQuestion, pendingRule.
- `maternal_015`: firstQuestion, focusedQuestion, pendingRule.
- `maternal_016`: focusedQuestion, pendingRule.
- `maternal_022`: focusedQuestion.
- `maternal_023`: focusedQuestion.
- `maternal_024`: focusedQuestion.
- `maternal_025`: focusedQuestion.
- `maternal_026`: focusedQuestion.
- `maternal_027`: focusedQuestion.
- `maternal_028`: focusedQuestion.
- `maternal_029`: focusedQuestion.
- `maternal_030`: focusedQuestion.
- `maternal_031`: target, stage, firstQuestion, focusedQuestion.
- `maternal_032`: focusedQuestion.
- `pediatric_001`: target, stage, firstQuestion.
- `pediatric_002`: target, stage, firstQuestion.
- `pediatric_003`: target, stage.
- `pediatric_004`: target, stage, firstQuestion.
- `pediatric_005`: target, stage, firstQuestion.
- `pediatric_006`: target, stage, firstQuestion.
- `pediatric_007`: target, stage, firstQuestion.
- `pediatric_008`: target, stage, firstQuestion.
- `pediatric_009`: target, stage.
- `pediatric_010`: target, stage.
- `pediatric_019`: stage.
- `pediatric_021`: firstQuestion, focusedQuestion.
- `pediatric_022`: stage.
- `pediatric_023`: stage.
- `pediatric_025`: stage, firstQuestion, focusedQuestion.
- `pediatric_031`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_001`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_002`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_003`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_004`: target, stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_005`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_006`: target, disposition.
- `multi_symptom_conflict_007`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_008`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_009`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_010`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_011`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_012`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_013`: target, stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_014`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_015`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_016`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_017`: target, stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_018`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_019`: stage, firstQuestion, focusedQuestion.
- `multi_symptom_conflict_020`: target, disposition.
- `multi_symptom_conflict_021`: target, stage, firstQuestion, focusedQuestion.
- `gemini_failure_001`: firstQuestion, focusedQuestion.
- `gemini_failure_003`: firstQuestion, focusedQuestion.
- `gemini_failure_004`: firstQuestion, focusedQuestion.
- `gemini_failure_005`: focusedQuestion.
- `gemini_failure_008`: target, stage, firstQuestion, focusedQuestion, disposition.
- `gemini_failure_010`: target, stage, firstQuestion, focusedQuestion.
- `gemini_failure_011`: firstQuestion, focusedQuestion.
- `gemini_failure_012`: target, stage, firstQuestion, focusedQuestion.
- `gemini_failure_014`: target, stage, firstQuestion, focusedQuestion.
- `gemini_failure_015`: firstQuestion, focusedQuestion.
- `gemini_failure_016`: target, stage, firstQuestion, focusedQuestion.
- `gemini_failure_020`: target, stage, firstQuestion, focusedQuestion.

## Limitations

- All messages and profiles are synthetic; no production, staging, log, database, or real-user data was used.
- Clinical review remains PENDING, so rates are an engineering baseline rather than clinical validation.
- Gemini modes are local deterministic failure fixtures and do not measure provider latency or live-model quality.
- The corpus measures the current canonical catalog and rules; it does not approve new questions or thresholds.
- Known-defect attribution is an authored engineering hypothesis; a matching failed dimension does not prove root-cause causality.
- The expectation digest proves no mutation during a run, not an independently timestamped pre-run oracle manifest.
- Some multi-turn follow-up messages reuse canonical safety-answer wording, so lexical diversity is lower than the case count.
- crossFamilyBatchRate uses a provisional question grouping declared in this script, not a reviewed complaint taxonomy; that contract is Phase 2 work.
- geminiFaultSafetyRetentionRate is not Gemini-on/off parity: the corpus holds no paired runs of one message under both modes.
- firstQuestionRelevanceRate scores strict first position; firstTurnQuestionRelevanceRate is the position-independent companion, because the approved design asks the global danger screen alongside a clarification question.

## Oracle integrity

- Corpus SHA-256: `9023d85829e56cc40bac1eb6422c581ca0078a33793616f78f9b11983e7be0a1`
- Expected/rationale SHA-256: `24bab6f3724d4f7b0703489dc41963016d2df5db9bc68ef48a99a7df0ab108e8`
- Actual values exist only in the JSON/Markdown baseline reports; the source corpus contains no actual fields.
