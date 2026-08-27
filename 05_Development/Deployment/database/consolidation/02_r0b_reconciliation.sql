-- CareBridge database consolidation — R0b reconciliation templates
-- Source: Database_Consolidation_Source_Code_Refactor_Plan.md §6 (R0b, R3b)
--
-- NOT read-only. Every statement here mutates production data and is therefore
-- a template: each block is commented out and requires the operator to fill in
-- the concrete ids and the actor recorded in the change ticket. Do not run this
-- file as a whole.
--
-- Run 01_data_gates.sql first, decide an outcome per row, apply the matching
-- block below, then re-run 01_data_gates.sql and attach both outputs.

\set ON_ERROR_STOP on

-- ===========================================================================
-- R0b-1. Pending account deletion requests  (V3 §3.1.1)
-- ---------------------------------------------------------------------------
-- Exactly one of three outcomes per request, with a written reason:
--   (a) DEACTIVATE — honour the request now as a logical deactivation
--   (b) CANCEL     — the user withdrew, or the request was invalid
--   (c) RETAIN     — keep until the grace period ends; blocks R1b until resolved
-- ===========================================================================

-- Inspect first.
SELECT r.id, r.user_id, r.status, r.reason, r.requested_at, r.scheduled_for,
       u.email, u.enabled, u.account_status, u.deactivated_at
FROM public.account_deletion_requests r
JOIN public.users u ON u.user_id = r.user_id
WHERE r.status = 'PENDING'
ORDER BY r.requested_at;

-- (a) DEACTIVATE. Mirrors AuthServiceImpl.deactivate(): the user row and the
--     request row move together, and sessions/device tokens are revoked in the
--     same transaction so no live credential outlives the deactivation.
-- BEGIN;
--   UPDATE public.users u
--      SET enabled              = false,
--          account_status       = 'DEACTIVATED',
--          deactivated_at       = now(),
--          deactivation_reason  = COALESCE(u.deactivation_reason, r.reason),
--          deactivated_by       = :'actor_user_id'::uuid
--     FROM public.account_deletion_requests r
--    WHERE r.id = :'request_id'::uuid
--      AND u.user_id = r.user_id;
--
--   UPDATE public.auth_sessions
--      SET revoked_at = now()
--    WHERE user_id = (SELECT user_id FROM public.account_deletion_requests
--                      WHERE id = :'request_id'::uuid)
--      AND revoked_at IS NULL;
--
--   UPDATE public.device_tokens
--      SET active = false, updated_at = now()
--    WHERE user_id = (SELECT user_id FROM public.account_deletion_requests
--                      WHERE id = :'request_id'::uuid)
--      AND active;
--
--   UPDATE public.account_deletion_requests
--      SET status = 'COMPLETED', processed_at = now(),
--          processed_by = :'actor_user_id'::uuid,
--          notes = :'ticket_note'
--    WHERE id = :'request_id'::uuid AND status = 'PENDING';
-- COMMIT;

-- (b) CANCEL.
-- BEGIN;
--   UPDATE public.account_deletion_requests
--      SET status = 'CANCELLED', processed_at = now(),
--          processed_by = :'actor_user_id'::uuid,
--          notes = :'ticket_note'
--    WHERE id = :'request_id'::uuid AND status = 'PENDING';
-- COMMIT;

-- (c) RETAIN: no statement. Record the grace-period end date in the change
--     ticket; GATE 1 stays red and R1b stays blocked until it is resolved.

-- ===========================================================================
-- R0b-2. Pending account lock appeals  (V3 §3.1.2)
-- ---------------------------------------------------------------------------
-- After this program the appeal history lives in the CSKH ticket system, so
-- each pending appeal needs a ticket id recorded before the table disappears.
-- The status CHECK requires reviewed_at to be set for every non-PENDING row.
-- ===========================================================================

SELECT a.appeal_id, a.user_id, a.lock_episode_id, a.status, a.submitted_at,
       u.email, u.locked, u.lock_type, u.lock_reason, u.locked_at
FROM public.account_lock_appeals a
JOIN public.users u ON u.user_id = a.user_id
WHERE a.status = 'PENDING'
ORDER BY a.submitted_at;

-- (a) APPROVE — unlock the account, then close the appeal. The unlock must be
--     audited with actor, user, lock episode, reason, ticket id and timestamp,
--     because that audit row becomes the only surviving record of the episode.
-- BEGIN;
--   UPDATE public.users
--      SET locked = false, lock_type = NULL, lock_reason = NULL,
--          locked_by = NULL, lock_episode_id = NULL, locked_at = NULL
--    WHERE user_id = :'user_id'::uuid;
--
--   INSERT INTO public.audit_events (actor_user_id, action, target_type,
--                                    target_id, metadata_jsonb)
--   VALUES (:'actor_user_id'::uuid, 'ADMIN_UNLOCK_USER', 'USER',
--           :'user_id'::uuid,
--           jsonb_build_object('lockEpisodeId', :'lock_episode_id',
--                              'reason', :'reason',
--                              'cskhTicketId', :'ticket_id',
--                              'source', 'R0B_APPEAL_RECONCILIATION'));
--
--   UPDATE public.account_lock_appeals
--      SET status = 'APPROVED', reviewed_by = :'actor_user_id'::uuid,
--          reviewed_at = now(), review_note = :'ticket_note'
--    WHERE appeal_id = :'appeal_id'::uuid AND status = 'PENDING';
-- COMMIT;

-- (b) REJECT — the lock stands; the ticket carries the explanation.
-- BEGIN;
--   UPDATE public.account_lock_appeals
--      SET status = 'REJECTED', reviewed_by = :'actor_user_id'::uuid,
--          reviewed_at = now(), review_note = :'ticket_note'
--    WHERE appeal_id = :'appeal_id'::uuid AND status = 'PENDING';
-- COMMIT;

-- ===========================================================================
-- R3b. Partner user remap  (V3 §3.3, plan §4.4 data-first gate)
-- ---------------------------------------------------------------------------
-- This must land in its own release, BEFORE the deploy that removes PARTNER
-- from the Role enum. A JWT or a users row still carrying 'PARTNER' after the
-- enum is gone fails JPA/JWT deserialization at request time.
-- ===========================================================================

SELECT user_id, email, full_name, enabled, account_status, created_at
FROM public.users
WHERE role = 'PARTNER'
ORDER BY created_at;

-- (a) Real user, business decision says convert to another role.
-- BEGIN;
--   UPDATE public.users SET role = :'target_role' WHERE user_id = :'user_id'::uuid;
--   UPDATE public.auth_sessions SET revoked_at = now()
--    WHERE user_id = :'user_id'::uuid AND revoked_at IS NULL;
--   UPDATE public.device_tokens SET active = false, updated_at = now()
--    WHERE user_id = :'user_id'::uuid AND active;
-- COMMIT;

-- (b) Seed/demo account with no real owner — deactivate instead of remapping,
--     so no unowned account keeps a usable role.
-- BEGIN;
--   UPDATE public.users
--      SET enabled = false, account_status = 'DEACTIVATED',
--          deactivated_at = now(),
--          deactivation_reason = 'Partner programme retired (R3b)',
--          deactivated_by = :'actor_user_id'::uuid,
--          role = :'target_role'
--    WHERE user_id = :'user_id'::uuid;
--   UPDATE public.auth_sessions SET revoked_at = now()
--    WHERE user_id = :'user_id'::uuid AND revoked_at IS NULL;
--   UPDATE public.device_tokens SET active = false, updated_at = now()
--    WHERE user_id = :'user_id'::uuid AND active;
-- COMMIT;

-- ===========================================================================
-- R4c. Device provenance snapshot  (V3 §3.2)
-- ---------------------------------------------------------------------------
-- Only needed when GATE 4 linked_observations > 0 and the Product Owner wants
-- the provenance kept. token_reference is deliberately absent from the payload:
-- it is an OAuth credential reference and must never reach an observation row.
-- ===========================================================================

-- BEGIN;
--   UPDATE public.health_observations o
--      SET raw_payload_jsonb = COALESCE(o.raw_payload_jsonb, '{}'::jsonb)
--            || jsonb_build_object(
--                 'deviceProvenance',
--                 jsonb_build_object(
--                   'connectionId', c.device_connection_id,
--                   'providerName', c.provider_name,
--                   'deviceName',   c.device_name,
--                   'capturedAt',   to_char(now() AT TIME ZONE 'UTC',
--                                           'YYYY-MM-DD"T"HH24:MI:SS"Z"')))
--     FROM public.device_connections c
--    WHERE o.device_connection_id = c.device_connection_id
--      AND (o.raw_payload_jsonb -> 'deviceProvenance') IS NULL;
-- COMMIT;

-- Re-run GATE 4 afterwards: observations_missing_provenance and
-- provenance_with_leaked_secret must both be 0.
