-- ============================================================================
-- Migration V4: Allow open-ended checklist instance windows
-- ----------------------------------------------------------------------------
-- Lifecycle substages có thể khai báo end_inclusive = 2147483647 (mở vô hạn,
-- ví dụ "Uống Axit Folic hàng ngày từ tuần 21"). ChecklistLifecycleEligibility
-- Service khi đó trả windowEnd = NULL trong khi windowStart vẫn có giá trị.
-- Ràng buộc cũ bắt buộc hai cột phải cùng NULL hoặc cùng NOT NULL nên toàn bộ
-- ứng viên open-ended bị chặn với MATERIALIZATION_FAILED (SQLSTATE 23514).
--
-- Ràng buộc mới vẫn cấm window_end tồn tại khi thiếu window_start và vẫn cấm
-- cửa sổ đảo ngược (window_end < window_start).
-- ============================================================================

ALTER TABLE public."checklist_instances"
DROP CONSTRAINT IF EXISTS "checklist_instances_window_ck";

ALTER TABLE public."checklist_instances"
ADD CONSTRAINT "checklist_instances_window_ck" CHECK (
    (window_start IS NULL AND window_end IS NULL)
    OR (
        window_start IS NOT NULL
        AND (window_end IS NULL OR window_end >= window_start)
    )
);
