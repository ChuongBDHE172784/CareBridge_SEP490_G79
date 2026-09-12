-- ============================================================================
-- Migration V12: restore the direct_messages body CHECK lost in consolidation
-- ============================================================================
--
-- BR-DCC-005 requires a TEXT direct message to carry a body of 1..2000 characters
-- after trimming. That was enforced by
--     direct_messages_body_ck
--         CHECK (length(btrim(message_body)) >= 1
--            AND length(btrim(message_body)) <= 2000)
-- until dad0e345c consolidated the migration chain: the V1 baseline kept only
-- direct_messages_payload_check, which asserts message_body IS NOT NULL and so
-- accepts a whitespace-only body. DirectChatIntegrationTest expects the blank
-- insert to be rejected, and it was the thing that noticed the constraint had
-- gone missing.
--
-- Any row already violating the rule is normalised first: a blank body becomes a
-- single '.', an over-long one is truncated to 2000 characters. Both are
-- no-ops on a schema that never accepted such rows, and they keep the ALTER from
-- failing on an environment that collected some while the check was absent.
UPDATE public.direct_messages
   SET message_body = '.'
 WHERE message_body IS NOT NULL
   AND length(btrim(message_body)) < 1;

UPDATE public.direct_messages
   SET message_body = left(message_body, 2000)
 WHERE message_body IS NOT NULL
   AND length(btrim(message_body)) > 2000;

ALTER TABLE public.direct_messages
    DROP CONSTRAINT IF EXISTS direct_messages_body_ck;

ALTER TABLE public.direct_messages
    ADD CONSTRAINT direct_messages_body_ck
    CHECK (message_body IS NULL
           OR (length(btrim(message_body)) >= 1
               AND length(btrim(message_body)) <= 2000));
