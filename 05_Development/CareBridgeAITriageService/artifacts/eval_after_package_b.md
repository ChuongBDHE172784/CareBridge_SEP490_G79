# AI Triage V2 vague-corpus baseline

> Synthetic offline engineering evaluation. Clinical review status: **PENDING**.

## Summary

- Cases: 160 total; 72 passed; 88 failed.
- Cases with no authored knownDefect: 103.

The second column excludes cases whose failure was predicted against an already-known
defect, so it is the closest available read on the engine's own behaviour.

| Metric | All cases | Excluding known defects |
|---|---:|---:|
| Case pass rate | 45.00% | 49.51% |
| Target accuracy | 92.50% | 89.32% |
| Stage accuracy | 81.25% | 71.84% |
| First-question relevance (strict position) | 74.66% | 71.91% |
| First-turn question relevance (position-independent) | 84.93% | 75.28% |
| Focused-question relevance | 58.90% | 52.81% |
| Safety-question coverage | 100.00% | 100.00% |
| Forbidden-question rate | 0.62% | 0.97% |
| Wrong-entity question rate | 3.75% | 0.97% |
| Wrong-stage question rate | 1.25% | 1.94% |
| Repeated-question rate | 0.00% | 0.00% |
| RED recall (17 RED-only cases) | 70.59% | 80.00% |
| Gemini-fault safety retention (15 fault cases) | 93.33% | 91.67% |
| Cross-family batch rate (103 batched turns) | 0.00% | 0.00% |
| Finite termination (38 evaluable) | 97.37% | 100.00% |
| Avg questions per turn | 1.622 | 1.747573 |
| Unsupported GREEN | 0 | 0 |
| Network clients created | 0 | 0 |

## Rates by group

| Group | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| GEMINI_FAILURE | 20 | 14 | 6 | 70.00% |
| MATERNAL | 32 | 4 | 28 | 12.50% |
| MULTI_SYMPTOM_CONFLICT | 21 | 0 | 21 | 0.00% |
| PEDIATRIC | 32 | 20 | 12 | 62.50% |
| VAGUE | 55 | 34 | 21 | 61.82% |

## Rates by tag

| Tag | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| HISTORY_CURRENT | 8 | 0 | 8 | 0.00% |
| MULTI_TURN | 24 | 18 | 6 | 75.00% |
| NEGATION | 16 | 9 | 7 | 56.25% |
| NO_DEVICE_OR_UNAWARE | 10 | 10 | 0 | 100.00% |
| NO_DIACRITICS | 10 | 4 | 6 | 40.00% |
| TYPO | 5 | 2 | 3 | 40.00% |
| UNTAGGED | 105 | 39 | 66 | 37.14% |

## Rates by Gemini fixture mode

| Mode | Total | Passed | Failed | Pass rate |
|---|---:|---:|---:|---:|
| 429 | 5 | 4 | 1 | 80.00% |
| 5XX | 5 | 5 | 0 | 100.00% |
| OFF | 145 | 60 | 85 | 41.38% |
| TIMEOUT | 5 | 3 | 2 | 60.00% |

## Failed cases

| Case | Group | Classification | Failed dimensions | Actual first question | Actual disposition |
|---|---|---|---|---|---|
| vague_009 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_010 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_011 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_012 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_013 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_014 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_015 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_016 | VAGUE | F-PROD-1 | focusedQuestion=F-PROD-1, pendingRule=F-PROD-1 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_021 | VAGUE | NEW_FINDING | stage=NEW_FINDING, wrongStageQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_032 | VAGUE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_034 | VAGUE | NEW_FINDING | wrongEntityQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_036 | VAGUE | NEW_FINDING | firstQuestion=NEW_FINDING, finiteTermination=F-P1-2 | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| vague_038 | VAGUE | NEW_FINDING | wrongEntityQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_040 | VAGUE | NEW_FINDING | wrongEntityQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_044 | VAGUE | NEW_FINDING | wrongEntityQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_046 | VAGUE | NEW_FINDING | wrongEntityQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| vague_048 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_049 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_050 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_051 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| vague_052 | VAGUE | F-P1-1 | firstQuestion=F-P1-1 | Q_CLARIFY_STAGE | NEEDS_MORE_INFO |
| maternal_004 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, disposition=NEW_FINDING, reasonCodes=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_006 | MATERNAL | NEW_FINDING | firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, disposition=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_007 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_008 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_009 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_010 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_011 | MATERNAL | F-P1-4 | focusedQuestion=F-P1-4, pendingRule=F-P1-4 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_012 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_013 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_014 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| maternal_015 | MATERNAL | NEW_FINDING | focusedQuestion=NEW_FINDING, pendingRule=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
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
| pediatric_011 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_012 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_013 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_014 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_015 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_016 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_017 | PEDIATRIC | F-COV-7 | disposition=F-COV-7, reasonCodes=F-COV-7 | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_019 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | — | RED |
| pediatric_022 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_023 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_025 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| pediatric_031 | PEDIATRIC | NEW_FINDING | stage=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
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
| multi_symptom_conflict_021 | MULTI_SYMPTOM_CONFLICT | NEW_FINDING | target=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_001 | GEMINI_FAILURE | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_003 | GEMINI_FAILURE | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_005 | GEMINI_FAILURE | NEW_FINDING | focusedQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_008 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING, disposition=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |
| gemini_failure_010 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, focusedQuestion=NEW_FINDING, forbiddenQuestions=NEW_FINDING, wrongEntityQuestion=NEW_FINDING, wrongStageQuestion=NEW_FINDING | Q_GLOBAL_DANGER | NEEDS_MORE_INFO |
| gemini_failure_014 | GEMINI_FAILURE | NEW_FINDING | target=NEW_FINDING, stage=NEW_FINDING, firstQuestion=NEW_FINDING, focusedQuestion=NEW_FINDING | Q_CLARIFY_TARGET_FIRST | NEEDS_MORE_INFO |

## New discoveries

- `vague_012`: target, stage, firstQuestion.
- `vague_021`: stage, wrongStageQuestion.
- `vague_032`: target, stage, firstQuestion.
- `vague_034`: wrongEntityQuestion.
- `vague_036`: firstQuestion.
- `vague_038`: wrongEntityQuestion.
- `vague_040`: wrongEntityQuestion.
- `vague_044`: wrongEntityQuestion.
- `vague_046`: wrongEntityQuestion.
- `maternal_004`: firstQuestion, disposition, reasonCodes.
- `maternal_006`: firstQuestion, focusedQuestion, disposition.
- `maternal_012`: focusedQuestion, pendingRule.
- `maternal_013`: focusedQuestion, pendingRule.
- `maternal_014`: focusedQuestion, pendingRule.
- `maternal_015`: focusedQuestion, pendingRule.
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
- `pediatric_019`: stage.
- `pediatric_022`: stage.
- `pediatric_023`: stage.
- `pediatric_025`: stage.
- `pediatric_031`: stage.
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
- `multi_symptom_conflict_021`: target, firstQuestion, focusedQuestion.
- `gemini_failure_001`: focusedQuestion.
- `gemini_failure_003`: focusedQuestion.
- `gemini_failure_005`: focusedQuestion.
- `gemini_failure_008`: target, stage, firstQuestion, focusedQuestion, disposition.
- `gemini_failure_010`: target, stage, focusedQuestion, forbiddenQuestions, wrongEntityQuestion, wrongStageQuestion.
- `gemini_failure_014`: target, stage, firstQuestion, focusedQuestion.

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
