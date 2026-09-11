-- ============================================================================
-- Migration V11: restore the runtime sequence grant for objects created after V1
-- ============================================================================
--
-- V1 ends with
--     GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public
--         TO carebridge_application;
-- but GRANT ... ON ALL SEQUENCES only touches the sequences that exist when it
-- runs. V3 then created public.maternal_knowledge_chunks with a SERIAL primary
-- key, so its sequence was born after the grant and carebridge_application was
-- left without USAGE on it. ProductionBaselineBootstrapTest asserts that no
-- sequence in public is missing that privilege, which is what caught this.
--
-- Re-issuing the same grant is enough: it is idempotent, and it covers every
-- sequence created between V1 and now without widening the privilege set --
-- these are exactly the privileges V1 already grants on sequences.
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public
    TO carebridge_application;

-- Deliberately no ALTER DEFAULT PRIVILEGES. The runtime role's table grants are
-- intentionally narrow and asymmetric -- audit_events is SELECT+INSERT with no
-- UPDATE or DELETE, reminder_occurrence_aliases is SELECT only, and
-- flyway_schema_history is revoked entirely -- so a blanket default that fires
-- on every future table would quietly hand out privileges the security contract
-- in ProductionBaselineBootstrapTest exists to forbid. New tables keep granting
-- explicitly.
