-- Recompute community_questions.answer_count from the actual APPROVED answer set.
-- Fixes counts left at 0 (or otherwise stale) by moderation approve/hide actions that
-- previously did not keep answer_count in sync with the visible answer set.
UPDATE community_questions q
SET answer_count = COALESCE((
    SELECT COUNT(*)
    FROM community_answers a
    WHERE a.question_id = q.id
      AND a.status = 'APPROVED'
), 0);
