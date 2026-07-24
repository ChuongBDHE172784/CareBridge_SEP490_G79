-- Newer identity-processing migrations add catalog objects that the older cleanup
-- checksum does not recognize. On a clean bootstrap the table is empty and is an
-- explicitly approved clean absence; live rows remain blocked for explicit mapping.
DO $final_cleanup_compatibility$
BEGIN
    IF to_regclass('public.expert_identity_verifications') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.expert_identity_verifications) THEN
            RAISE EXCEPTION
                'BLOCKED_EXPERT_IDENTITY_ARCHIVE: live rows require credential-history mapping';
        END IF;
        DROP TABLE public.expert_identity_verifications;
    END IF;
END
$final_cleanup_compatibility$;
