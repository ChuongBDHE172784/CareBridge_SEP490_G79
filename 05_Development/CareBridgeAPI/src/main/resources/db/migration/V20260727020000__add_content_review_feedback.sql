ALTER TABLE public.content_items
    ADD COLUMN IF NOT EXISTS revision_reason text,
    ADD COLUMN IF NOT EXISTS revision_requested_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS revision_requested_by uuid,
    ADD COLUMN IF NOT EXISTS revision_requested_version integer,
    ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

ALTER TABLE public.care_item_templates
    ADD COLUMN IF NOT EXISTS author_user_id uuid,
    ADD COLUMN IF NOT EXISTS revision_reason text,
    ADD COLUMN IF NOT EXISTS revision_requested_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS revision_requested_by uuid,
    ADD COLUMN IF NOT EXISTS revision_requested_version integer,
    ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (type IN (
        'REMINDER',
        'COMMUNITY_REPLY',
        'CONSULTATION',
        'EMERGENCY',
        'MESSAGE',
        'GROUP_INVITE',
        'CONTENT_REVIEW'
    ));
