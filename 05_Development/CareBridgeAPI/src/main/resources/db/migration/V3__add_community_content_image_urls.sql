ALTER TABLE public.community_content
    ADD COLUMN IF NOT EXISTS image_urls jsonb NOT NULL DEFAULT '[]'::jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'community_content_image_urls_ck'
          AND conrelid = 'public.community_content'::regclass
    ) THEN
        ALTER TABLE public.community_content
            ADD CONSTRAINT community_content_image_urls_ck
            CHECK (
                jsonb_typeof(image_urls) = 'array'
                AND jsonb_array_length(image_urls) <= 3
            );
    END IF;
END $$;
