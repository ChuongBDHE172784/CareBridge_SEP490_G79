-- Persist the physical storage backend used by each attachment. Without this
-- column Hibernate reconstructed every row with UploadedFile's in-memory
-- default (cloudinary), including consultation recordings actually stored in
-- Cloudflare R2.

ALTER TABLE public."attachments"
ADD COLUMN IF NOT EXISTS "storage_provider" character varying(20);

-- Canonical Cloudinary keys carry resource/access metadata separated by '|'.
-- Images have always been routed to Cloudinary. Plain document/media keys are
-- the R2 shape used by CareBridge; legacy plain Cloudinary document keys were
-- already non-resolvable because their generated public_id was never stored.
UPDATE public."attachments"
SET "storage_provider" = CASE
    WHEN "storage_key" LIKE '%|%' OR "mime_type" LIKE 'image/%' THEN 'cloudinary'
    ELSE 'r2'
END
WHERE "storage_provider" IS NULL OR btrim("storage_provider") = '';

ALTER TABLE public."attachments"
ALTER COLUMN "storage_provider" SET DEFAULT 'cloudinary',
ALTER COLUMN "storage_provider" SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'attachments_storage_provider_ck'
    ) THEN
        ALTER TABLE public."attachments"
        ADD CONSTRAINT "attachments_storage_provider_ck"
        CHECK ("storage_provider" IN ('cloudinary', 'r2'));
    END IF;
END $$;
