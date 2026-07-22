-- Manual sample-data script for the community CATEGORY -> TOPIC hierarchy (Amendment 2, ADR-COM-020).
-- NOT a Flyway migration: lives outside db/migration, so it never auto-applies. Run it by hand
-- whenever you want extra topics/tags to test the Content Admin "Quản lý Chủ đề & Danh mục" screen.
--
-- Precondition: V20260722054603__invert_community_topic_hierarchy.sql must already be applied
-- (the 5 CATEGORY rows this script attaches TOPIC rows to must exist).
--
-- Safe to re-run any number of times: every row is upserted by fixed id (ON CONFLICT DO UPDATE),
-- so re-running just resets these sample rows back to the values below. It never deletes rows,
-- so it is safe even if you created questions/follows against a sample topic during testing.
--
-- Usage:
--   psql "$SUPABASE_DB_URL" -f community_topics_sample_data.sql
--   (or run it from pgAdmin4's Query Tool against your local carebridge_local database)

BEGIN;

-- 15 sample TOPIC rows nested under the 5 approved CATEGORY rows.
INSERT INTO community_topics
    (id, name, description, icon, type, slug, parent_id, is_hidden, sort_order, created_by, created_at, updated_at)
VALUES
    -- Chuẩn bị mang thai (b1b2c3d4-e5f6-7890-abcd-ef1234567801)
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567801', 'Xét nghiệm tiền hôn nhân',
     'Các xét nghiệm nên làm trước khi kết hôn và mang thai', NULL, 'TOPIC',
     'xet-nghiem-tien-hon-nhan', 'b1b2c3d4-e5f6-7890-abcd-ef1234567801', false, 101, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567802', 'Tiêm phòng trước khi mang thai',
     'Lịch tiêm chủng khuyến nghị cho phụ nữ chuẩn bị mang thai', NULL, 'TOPIC',
     'tiem-phong-truoc-khi-mang-thai', 'b1b2c3d4-e5f6-7890-abcd-ef1234567801', false, 102, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567803', 'Chế độ dinh dưỡng trước thụ thai',
     'Dinh dưỡng và bổ sung vi chất trước khi mang thai', NULL, 'TOPIC',
     'che-do-dinh-duong-truoc-thu-thai', 'b1b2c3d4-e5f6-7890-abcd-ef1234567801', false, 103, NULL, NOW(), NOW()),

    -- Mang thai (b1b2c3d4-e5f6-7890-abcd-ef1234567802)
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567804', 'Khám thai định kỳ',
     'Lịch khám và các mốc siêu âm quan trọng trong thai kỳ', NULL, 'TOPIC',
     'kham-thai-dinh-ky', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', false, 104, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567805', 'Dấu hiệu nguy hiểm khi mang thai',
     'Các dấu hiệu cần đến cơ sở y tế ngay', NULL, 'TOPIC',
     'dau-hieu-nguy-hiem-khi-mang-thai', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', false, 105, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567806', 'Vận động và yoga bầu',
     'Bài tập nhẹ nhàng an toàn cho mẹ bầu', NULL, 'TOPIC',
     'van-dong-va-yoga-bau', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802', false, 106, NULL, NOW(), NOW()),

    -- Sau sinh (b1b2c3d4-e5f6-7890-abcd-ef1234567803)
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567807', 'Trầm cảm sau sinh',
     'Nhận biết và hỗ trợ trầm cảm, lo âu sau sinh', NULL, 'TOPIC',
     'tram-cam-sau-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', false, 107, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567808', 'Phục hồi vóc dáng sau sinh',
     'Dinh dưỡng và vận động phục hồi sau sinh', NULL, 'TOPIC',
     'phuc-hoi-voc-dang-sau-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', false, 108, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567809', 'Chăm sóc vết mổ vết khâu',
     'Hướng dẫn chăm sóc vết thương sau sinh mổ hoặc sinh thường', NULL, 'TOPIC',
     'cham-soc-vet-mo-vet-khau', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803', false, 109, NULL, NOW(), NOW()),

    -- Chăm bé (b1b2c3d4-e5f6-7890-abcd-ef1234567804)
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567810', 'Dinh dưỡng ăn dặm',
     'Xây dựng thực đơn ăn dặm khoa học cho bé', NULL, 'TOPIC',
     'dinh-duong-an-dam', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', false, 110, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567811', 'Tiêm chủng cho bé',
     'Lịch tiêm chủng mở rộng và dịch vụ cho trẻ nhỏ', NULL, 'TOPIC',
     'tiem-chung-cho-be', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', false, 111, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567812', 'Giấc ngủ của trẻ sơ sinh',
     'Thiết lập nếp sinh hoạt và giấc ngủ cho trẻ sơ sinh', NULL, 'TOPIC',
     'giac-ngu-cua-tre-so-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', false, 112, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567813', 'Phát triển vận động của bé',
     'Các mốc phát triển vận động theo tháng tuổi', NULL, 'TOPIC',
     'phat-trien-van-dong-cua-be', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804', false, 113, NULL, NOW(), NOW()),

    -- Khác (b1b2c3d4-e5f6-7890-abcd-ef1234567805)
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567814', 'Bảo hiểm y tế cho mẹ và bé',
     'Quyền lợi bảo hiểm y tế liên quan thai sản', NULL, 'TOPIC',
     'bao-hiem-y-te-cho-me-va-be', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805', false, 114, NULL, NOW(), NOW()),
    ('d1d2d3d4-e5f6-7890-abcd-ef1234567815', 'Kinh nghiệm chọn bệnh viện',
     'Chia sẻ kinh nghiệm chọn nơi khám và sinh', NULL, 'TOPIC',
     'kinh-nghiem-chon-benh-vien', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805', false, 115, NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    type = EXCLUDED.type,
    slug = EXCLUDED.slug,
    parent_id = EXCLUDED.parent_id,
    is_hidden = EXCLUDED.is_hidden,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

-- 8 sample TAG rows (flat, never nested under a CATEGORY).
INSERT INTO community_topics
    (id, name, description, icon, type, slug, parent_id, is_hidden, sort_order, created_by, created_at, updated_at)
VALUES
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567801', 'Song thai', 'Mang đa thai', NULL, 'TAG',
     'song-thai', NULL, false, 1, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567802', 'Sinh mổ', 'Sinh mổ lấy thai', NULL, 'TAG',
     'sinh-mo', NULL, false, 2, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567803', 'Sinh thường', 'Sinh thường qua đường âm đạo', NULL, 'TAG',
     'sinh-thuong', NULL, false, 3, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567804', 'Mang thai lần đầu', 'Dành cho mẹ mang thai con so', NULL, 'TAG',
     'mang-thai-lan-dau', NULL, false, 4, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567805', 'Mẹ đơn thân', 'Nội dung dành cho mẹ đơn thân', NULL, 'TAG',
     'me-don-than', NULL, false, 5, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567806', 'Thai kỳ nguy cơ cao', 'Thai kỳ cần theo dõi y tế sát sao', NULL, 'TAG',
     'thai-ky-nguy-co-cao', NULL, false, 6, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567807', 'Sinh non', 'Sinh trước 37 tuần thai kỳ', NULL, 'TAG',
     'sinh-non', NULL, false, 7, NULL, NOW(), NOW()),
    ('e1e2e3e4-e5f6-7890-abcd-ef1234567808', 'Nuôi con bằng sữa công thức', 'Nuôi con bằng sữa công thức', NULL, 'TAG',
     'nuoi-con-bang-sua-cong-thuc', NULL, false, 8, NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    type = EXCLUDED.type,
    slug = EXCLUDED.slug,
    parent_id = EXCLUDED.parent_id,
    is_hidden = EXCLUDED.is_hidden,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

COMMIT;
