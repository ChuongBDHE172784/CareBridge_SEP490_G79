-- Add reference_links and reference_files columns to ai_moderation_policies table
ALTER TABLE public.ai_moderation_policies ADD COLUMN IF NOT EXISTS reference_links TEXT;
ALTER TABLE public.ai_moderation_policies ADD COLUMN IF NOT EXISTS reference_files TEXT;
