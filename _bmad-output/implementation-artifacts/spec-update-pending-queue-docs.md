---
title: 'Document first-time moderation action boundaries'
type: 'chore'
created: '2026-07-14'
status: 'done'
route: 'one-shot'
context: []
---

# Document first-time moderation action boundaries

## Intent

**Problem:** UC-99 documentation preserved a historical note claiming Hide was available in the first-time PENDING queue, which conflicted with the corrected UI behavior and could lead to future regression.

**Approach:** Add append-only corrections to the UC-99 TDS and Test-Spec: first-time review exposes Approve and Request revision only; Hide and Lock remain in reported/processed moderation; the history tab is read-only audit evidence.

## Suggested Review Order

- Preserves the prior decision while recording the authoritative correction and UI boundary.
  [UC99_PendingContentQueue_TDS.md:29](../../04_Implement/UC99_PendingContentQueue/UC99_PendingContentQueue_TDS.md#L29)

- Clarifies that the processed tab is audit history rather than an enforcement surface.
  [UC99_PendingContentQueue_TDS.md:453](../../04_Implement/UC99_PendingContentQueue/UC99_PendingContentQueue_TDS.md#L453)

- Updates manual acceptance evidence to prevent a future reintroduction of Hide or Lock.
  [UC99_PendingContentQueue_Test-Spec.md:24](../../04_Implement/UC99_PendingContentQueue/UC99_PendingContentQueue_Test-Spec.md#L24)
