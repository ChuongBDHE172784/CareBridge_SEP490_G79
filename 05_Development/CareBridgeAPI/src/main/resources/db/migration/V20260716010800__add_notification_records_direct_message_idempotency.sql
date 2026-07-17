-- CB-EXPCHAT-IMP-001 / ADR-MEDI-004 mục 3 — DB-enforced idempotency for MESSAGE notifications.
-- Independent of the clientMessageId early-return in sendMessage(): protects against the
-- DirectMessageNotificationListener itself running twice for the same (recipient, messageId).
-- Partial unique index — only constrains type=MESSAGE/reference_type=DIRECT_MESSAGE rows, so it
-- cannot collide with any other NotificationType even if reference_id happens to match.
-- Safe additive: no MESSAGE-typed row can exist before this feature (enum value added in the
-- previous migration of this same set), so no pre-existing data can violate it.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_direct_message
    ON public.notification_records (user_id, reference_id)
    WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE';
