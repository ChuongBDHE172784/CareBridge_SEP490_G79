-- ============================================================================
-- Migration V5: Add call recording and consent fields to conversation_calls
-- ----------------------------------------------------------------------------
-- Hỗ trợ lưu trữ bản ghi âm/ghi hình cuộc gọi tư vấn 1-1 giữa Mẹ và Chuyên gia
-- (Private R2 Attachment) và trạng thái đồng thuận quyền riêng tư PDPA.
-- ============================================================================

ALTER TABLE public."conversation_calls"
ADD COLUMN IF NOT EXISTS "recording_file_id" uuid,
ADD COLUMN IF NOT EXISTS "consent_attested" boolean NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS "recording_status" character varying(30) NOT NULL DEFAULT 'NONE',
ADD COLUMN IF NOT EXISTS "recorded_duration_seconds" integer;

-- Ràng buộc khoá ngoại tham chiếu attachments
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'conversation_calls_recording_file_id_fkey'
    ) THEN
        ALTER TABLE public."conversation_calls"
        ADD CONSTRAINT "conversation_calls_recording_file_id_fkey"
        FOREIGN KEY ("recording_file_id") REFERENCES public."attachments"("attachment_id") ON DELETE SET NULL;
    END IF;
END $$;

-- Ràng buộc trạng thái recording_status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'conversation_calls_recording_status_ck'
    ) THEN
        ALTER TABLE public."conversation_calls"
        ADD CONSTRAINT "conversation_calls_recording_status_ck"
        CHECK ("recording_status" IN ('NONE', 'RECORDING', 'UPLOADED', 'FAILED'));
    END IF;
END $$;

-- Index phục vụ truy vấn tra cứu bản ghi của Admin Portal
CREATE INDEX IF NOT EXISTS "conversation_calls_admin_search_ix"
ON public."conversation_calls" ("initiated_at" DESC, "call_status", "recording_status");
