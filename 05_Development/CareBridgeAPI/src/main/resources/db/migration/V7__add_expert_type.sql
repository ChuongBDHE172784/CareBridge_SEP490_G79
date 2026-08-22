-- ============================================================================
-- Migration V7: Phân loại hai nhóm chuyên gia trên users.expert_type
-- ----------------------------------------------------------------------------
-- Tách Chuyên gia Hệ thống (đã ký Thoả thuận hợp tác, cam kết lịch trực) khỏi
-- Chuyên gia Y tế Cộng đồng (tình nguyện). Xem docs/expert-two-tier-flow.md.
--
-- Cột NULLABLE có chủ ý: users dùng chung cho mọi role (MOTHER/FAMILY/ADMIN…),
-- NOT NULL sẽ ép mọi role phải mang giá trị. Ràng buộc CHECK cũng chỉ giới hạn
-- MIỀN GIÁ TRỊ chứ không ràng theo role — role được gán ngay khi người dùng chọn
-- vai trò, trước khi hồ sơ chuyên gia tồn tại, nên ràng theo role sẽ làm vỡ luồng
-- đăng ký.
--
-- Máy trạng thái:
--   NULL ──chọn hình thức + admin duyệt──▶ PENDING_CONTRACT ──admin xếp xuống──▶ COMMUNITY
--                                                │
--                                                └──chuyên gia ký──▶ CONTRACTED ──admin hạ──▶ COMMUNITY
-- ============================================================================

ALTER TABLE public."users"
ADD COLUMN IF NOT EXISTS "expert_type" character varying(20);

-- Backfill: mọi chuyên gia hiện có về nhóm cộng đồng. CONTRACTED chỉ đạt được
-- qua hành vi ký của chính chuyên gia, không suy ra được từ dữ liệu cũ.
UPDATE public."users"
SET "expert_type" = 'COMMUNITY'
WHERE "role" = 'EXPERT'
  AND ("expert_type" IS NULL OR btrim("expert_type") = '');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_expert_type_ck'
    ) THEN
        ALTER TABLE public."users"
        ADD CONSTRAINT "users_expert_type_ck"
        CHECK ("expert_type" IS NULL
               OR "expert_type" IN ('COMMUNITY', 'PENDING_CONTRACT', 'CONTRACTED'));
    END IF;
END $$;

-- Danh bạ chuyên gia sắp xếp nhóm hợp tác lên đầu rồi mới tới rating; chỉ số
-- riêng phần này để ORDER BY không phải quét toàn bảng users.
CREATE INDEX IF NOT EXISTS "users_expert_directory_ix"
ON public."users" USING btree ("expert_type", "rating_avg" DESC)
WHERE "role" = 'EXPERT';
