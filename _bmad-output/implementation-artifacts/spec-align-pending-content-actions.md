---
title: 'Align first-time moderation actions with pending content'
type: 'bugfix'
created: '2026-07-14'
status: 'done'
route: 'one-shot'
context: []
---

# Align first-time moderation actions with pending content

## Intent

**Problem:** The first-time moderation queue lists only unpublished `PENDING` questions and answers, but exposed Hide and Lock actions. Those actions imply moderation of visible/reported content and make no sense before publication; locking an unpublished question is especially misleading.

**Approach:** Keep first-time review to Approve and Request revision, preserving the existing confirmation/reason flow. Hide and Lock remain available through the established reported/processed-content moderation workflows and are no longer offered in the pending queue.

## Suggested Review Order

- Narrows action configuration to the only non-approval decision valid during first review.
  [PendingContentQueuePage.tsx:15](../../05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx#L15)

- Shows only approve and revision actions for unpublished questions and answers.
  [PendingContentQueuePage.tsx:258](../../05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx#L258)

- Retains the revision confirmation dialog without obsolete action icon branches.
  [PendingContentQueuePage.tsx:302](../../05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx#L302)
