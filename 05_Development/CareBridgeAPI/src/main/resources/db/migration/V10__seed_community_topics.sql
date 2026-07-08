-- Seed default community topics so MOTHER users can create questions.
-- Only inserts if no topics exist yet.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM community_topics LIMIT 1) THEN
        INSERT INTO community_topics (id, name, description, icon, sort_order, is_hidden, created_at)
        VALUES
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Dinh dưỡng thai kỳ',     'Chế độ ăn, bổ sung vi chất, thực phẩm an toàn khi mang thai',           'restaurant',      1, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Sức khỏe thai nhi',       'Theo dõi sự phát triển, siêu âm, xét nghiệm thai kỳ',                   'health_and_safety',2, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Chăm sóc sau sinh',       'Hồi phục sau sinh, chăm sóc vết thương, tâm lý sau sinh',               'vaccines',         3, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Nuôi con bằng sữa mẹ',   'Kỹ thuật cho bú, tăng sữa, cai sữa',                                    'child_care',       4, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Giấc ngủ và thể chất',   'Tư thế ngủ, vận động an toàn, giảm đau lưng khi mang thai',             'bedtime',          5, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567806', 'Tâm lý & Cảm xúc',       'Lo âu, trầm cảm thai kỳ, hỗ trợ tinh thần mẹ bầu',                     'psychology',       6, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567807', 'Chăm sóc bé sơ sinh',    'Tắm bé, chăm rốn, lịch tiêm chủng, phát triển trẻ 0–12 tháng',         'pregnant_woman',   7, false, NOW()),
            ('a1b2c3d4-e5f6-7890-abcd-ef1234567808', 'Hỏi đáp chung',           'Các câu hỏi khác về thai kỳ và làm mẹ',                                 'forum',            8, false, NOW());
    END IF;
END $$;
