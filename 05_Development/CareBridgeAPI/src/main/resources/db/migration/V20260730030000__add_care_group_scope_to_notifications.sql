ALTER TABLE notification_records
    ADD COLUMN IF NOT EXISTS care_group_id UUID;

CREATE INDEX IF NOT EXISTS idx_notification_records_care_group_id
    ON notification_records (care_group_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_notification_records_care_group'
    ) THEN
        ALTER TABLE notification_records
            ADD CONSTRAINT fk_notification_records_care_group
            FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id);
    END IF;
END
$$;
