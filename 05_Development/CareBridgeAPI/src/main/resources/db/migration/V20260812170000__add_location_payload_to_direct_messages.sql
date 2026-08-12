ALTER TABLE direct_messages
    ADD COLUMN IF NOT EXISTS location_latitude double precision,
    ADD COLUMN IF NOT EXISTS location_longitude double precision,
    ADD COLUMN IF NOT EXISTS location_label varchar(200);

ALTER TABLE direct_messages
    DROP CONSTRAINT IF EXISTS direct_messages_payload_check;

ALTER TABLE direct_messages
    ADD CONSTRAINT direct_messages_payload_check
    CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'LOCATION')
      AND ((message_type = 'TEXT'
              AND message_body IS NOT NULL
              AND attachment_id IS NULL
              AND location_latitude IS NULL
              AND location_longitude IS NULL
              AND location_label IS NULL
              AND recalled_at IS NULL)
        OR (message_type IN ('IMAGE', 'FILE')
              AND attachment_id IS NOT NULL
              AND location_latitude IS NULL
              AND location_longitude IS NULL
              AND location_label IS NULL
              AND recalled_at IS NULL)
        OR (message_type = 'LOCATION'
              AND attachment_id IS NULL
              AND message_body IS NULL
              AND location_latitude BETWEEN -90 AND 90
              AND location_longitude BETWEEN -180 AND 180
              AND recalled_at IS NULL)
        OR (recalled_at IS NOT NULL
              AND message_body IS NULL
              AND attachment_id IS NULL
              AND location_latitude IS NULL
              AND location_longitude IS NULL
              AND location_label IS NULL)));
