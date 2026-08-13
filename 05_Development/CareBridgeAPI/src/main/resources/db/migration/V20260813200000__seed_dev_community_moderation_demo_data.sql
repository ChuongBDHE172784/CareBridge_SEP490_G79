-- Realistic community and moderation demo data for the canonical @carebridge.dev accounts.
--
-- This migration is intentionally data-only and production-safe: it becomes a complete no-op
-- unless all 21 canonical development accounts exist with the expected roles and all six expert
-- accounts are verified. Deterministic UUIDs keep the dataset traceable and conflict-safe without
-- adding any permanent support table.
DO $community_demo$
DECLARE
    v_now timestamptz := clock_timestamp();
    v_content_admin uuid;
    v_moderator uuid;
    v_mothers uuid[];
    v_families uuid[];
    v_experts uuid[];
    v_seed_count integer;
BEGIN
    SELECT count(DISTINCT email)
      INTO v_seed_count
      FROM public.users
     WHERE (email, role) IN (
        ('admin@carebridge.dev', 'SYSTEM_ADMIN'),
        ('moderator@carebridge.dev', 'MODERATOR'),
        ('content@carebridge.dev', 'CONTENT_ADMIN'),
        ('expert@carebridge.dev', 'EXPERT'),
        ('expert2@carebridge.dev', 'EXPERT'),
        ('expert3@carebridge.dev', 'EXPERT'),
        ('expert4@carebridge.dev', 'EXPERT'),
        ('expert5@carebridge.dev', 'EXPERT'),
        ('expert6@carebridge.dev', 'EXPERT'),
        ('mother@carebridge.dev', 'MOTHER'),
        ('mother2@carebridge.dev', 'MOTHER'),
        ('mother3@carebridge.dev', 'MOTHER'),
        ('mother4@carebridge.dev', 'MOTHER'),
        ('mother5@carebridge.dev', 'MOTHER'),
        ('mother6@carebridge.dev', 'MOTHER'),
        ('family@carebridge.dev', 'FAMILY'),
        ('family2@carebridge.dev', 'FAMILY'),
        ('family3@carebridge.dev', 'FAMILY'),
        ('family4@carebridge.dev', 'FAMILY'),
        ('family5@carebridge.dev', 'FAMILY'),
        ('family6@carebridge.dev', 'FAMILY')
     );

    IF v_seed_count <> 21 OR (
        SELECT count(*)
          FROM public.users
         WHERE email IN (
            'expert@carebridge.dev', 'expert2@carebridge.dev', 'expert3@carebridge.dev',
            'expert4@carebridge.dev', 'expert5@carebridge.dev', 'expert6@carebridge.dev')
           AND role = 'EXPERT'
           AND verification_status = 'APPROVED'
    ) <> 6 THEN
        RAISE NOTICE 'Skipping CareBridge community demo seed: canonical dev accounts are unavailable or experts are not verified';
        RETURN;
    END IF;

    SELECT user_id INTO STRICT v_content_admin FROM public.users WHERE email = 'content@carebridge.dev';
    SELECT user_id INTO STRICT v_moderator FROM public.users WHERE email = 'moderator@carebridge.dev';
    v_mothers := ARRAY[
        (SELECT user_id FROM public.users WHERE email = 'mother@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'mother2@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'mother3@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'mother4@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'mother5@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'mother6@carebridge.dev')
    ];
    v_families := ARRAY[
        (SELECT user_id FROM public.users WHERE email = 'family@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'family2@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'family3@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'family4@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'family5@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'family6@carebridge.dev')
    ];
    v_experts := ARRAY[
        (SELECT user_id FROM public.users WHERE email = 'expert@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'expert2@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'expert3@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'expert4@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'expert5@carebridge.dev'),
        (SELECT user_id FROM public.users WHERE email = 'expert6@carebridge.dev')
    ];

    -- Twenty community categories. These are kept distinct from the recommendation TAG catalog.
    WITH category_seed(seq, name, slug, description, icon) AS (VALUES
        (1,  'Kế hoạch mang thai',           'community-ke-hoach-mang-thai',       'Chuẩn bị sức khỏe, dinh dưỡng và tâm lý trước khi mang thai.', 'event_available'),
        (2,  'Ba tháng đầu thai kỳ',         'community-ba-thang-dau',             'Những thay đổi và lưu ý từ tuần 1 đến tuần 13.', 'looks_one'),
        (3,  'Ba tháng giữa thai kỳ',        'community-ba-thang-giua',            'Theo dõi mẹ và bé từ tuần 14 đến tuần 27.', 'looks_two'),
        (4,  'Ba tháng cuối thai kỳ',        'community-ba-thang-cuoi',            'Chuẩn bị an toàn cho giai đoạn từ tuần 28 đến khi sinh.', 'looks_3'),
        (5,  'Khám thai và xét nghiệm',      'community-kham-thai-xet-nghiem',     'Trao đổi về lịch khám, siêu âm và xét nghiệm thai kỳ.', 'medical_information'),
        (6,  'Dinh dưỡng thai kỳ',           'community-dinh-duong-thai-ky',       'Bữa ăn cân bằng, vi chất và an toàn thực phẩm.', 'nutrition'),
        (7,  'Vận động và giấc ngủ',         'community-van-dong-giac-ngu',        'Vận động phù hợp và cải thiện chất lượng giấc ngủ.', 'self_improvement'),
        (8,  'Triệu chứng thường gặp',       'community-trieu-chung-thuong-gap',   'Chia sẻ triệu chứng và cách nhận biết khi cần đi khám.', 'symptoms'),
        (9,  'Thai kỳ nguy cơ cao',          'community-thai-ky-nguy-co-cao',      'Hỗ trợ theo dõi các thai kỳ cần chăm sóc chuyên sâu.', 'health_and_safety'),
        (10, 'Chuẩn bị sinh',                'community-chuan-bi-sinh',            'Dấu hiệu chuyển dạ, kế hoạch sinh và đồ dùng cần thiết.', 'pregnant_woman'),
        (11, 'Phục hồi sau sinh',            'community-phuc-hoi-sau-sinh',        'Chăm sóc thể chất cho mẹ trong những tuần đầu sau sinh.', 'healing'),
        (12, 'Nuôi con bằng sữa mẹ',         'community-nuoi-con-sua-me',          'Khớp ngậm, tư thế bú, hút và bảo quản sữa.', 'breastfeeding'),
        (13, 'Chăm sóc trẻ sơ sinh',         'community-cham-soc-tre-so-sinh',     'Chăm sóc rốn, da, thân nhiệt và vệ sinh cho bé.', 'child_care'),
        (14, 'Giấc ngủ của bé',              'community-giac-ngu-cua-be',           'Nếp ngủ an toàn và cách đồng hành khi bé quấy đêm.', 'bedtime'),
        (15, 'Tiêm chủng và phòng bệnh',     'community-tiem-chung-phong-benh',    'Lịch tiêm và biện pháp phòng bệnh theo mùa.', 'vaccines'),
        (16, 'Phát triển của trẻ',           'community-phat-trien-cua-tre',       'Theo dõi các mốc vận động, giao tiếp và nhận thức.', 'neurology'),
        (17, 'Sức khỏe tinh thần',           'community-suc-khoe-tinh-than',       'Không gian an toàn về lo âu thai kỳ và cảm xúc sau sinh.', 'psychology'),
        (18, 'Vai trò người đồng hành',      'community-nguoi-dong-hanh',          'Kinh nghiệm chăm sóc mẹ và bé dành cho bạn đời.', 'partner_exchange'),
        (19, 'Gia đình và quan hệ',          'community-gia-dinh-quan-he',         'Giao tiếp, phân chia công việc và xây dựng mạng lưới hỗ trợ.', 'diversity_3'),
        (20, 'Dịch vụ y tế và quyền lợi',    'community-dich-vu-y-te',             'Kinh nghiệm chọn cơ sở y tế, bảo hiểm và hồ sơ khám.', 'local_hospital')
    )
    INSERT INTO public.community_topics
        (id, name, description, icon, type, slug, parent_id, is_hidden, sort_order, created_by, created_at, updated_at)
    SELECT ('c1000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           name, description, icon, 'CATEGORY', slug, NULL, false, seq * 10,
           v_content_admin, v_now - interval '90 days', v_now - interval '2 days'
      FROM category_seed
    ON CONFLICT (id) DO NOTHING;

    -- Forty child topics (two per category) give the mobile feed and topic-management page a
    -- practical, navigable taxonomy instead of a flat list.
    WITH topic_seed(seq, category_seq, name, slug, description) AS (VALUES
        (1, 1, 'Chuẩn bị sức khỏe trước thai', 'community-chuan-bi-suc-khoe', 'Khám tiền thai, bệnh nền và những việc nên chuẩn bị trước khi có em bé.'),
        (2, 1, 'Axit folic và vi chất', 'community-axit-folic-vi-chat', 'Trao đổi cách bổ sung vi chất an toàn theo tư vấn chuyên môn.'),
        (3, 2, 'Dấu hiệu thai sớm', 'community-dau-hieu-thai-som', 'Những thay đổi thường gặp trong các tuần đầu thai kỳ.'),
        (4, 2, 'Nghén và ăn uống', 'community-nghen-an-uong', 'Kinh nghiệm ăn uống khi buồn nôn hoặc nhạy mùi.'),
        (5, 3, 'Thai máy lần đầu', 'community-thai-may-lan-dau', 'Chia sẻ thời điểm và cảm nhận thai máy.'),
        (6, 3, 'Đau lưng và chuột rút', 'community-dau-lung-chuot-rut', 'Vận động và tư thế giúp mẹ dễ chịu hơn.'),
        (7, 4, 'Theo dõi cử động thai', 'community-theo-doi-cu-dong-thai', 'Kinh nghiệm theo dõi nhịp sinh hoạt của em bé cuối thai kỳ.'),
        (8, 4, 'Phù và khó thở cuối thai kỳ', 'community-phu-kho-tho-cuoi-thai-ky', 'Phân biệt thay đổi thường gặp với dấu hiệu cần khám sớm.'),
        (9, 5, 'Lịch khám thai', 'community-lich-kham-thai', 'Nhắc lịch và chuẩn bị câu hỏi trước mỗi lần khám.'),
        (10, 5, 'Siêu âm và xét nghiệm', 'community-sieu-am-xet-nghiem', 'Hiểu mục đích của các mốc siêu âm và xét nghiệm phổ biến.'),
        (11, 6, 'Bữa ăn cân bằng', 'community-bua-an-can-bang', 'Gợi ý bữa ăn đa dạng, vừa sức và phù hợp khẩu vị.'),
        (12, 6, 'An toàn thực phẩm', 'community-an-toan-thuc-pham', 'Cách chọn, chế biến và bảo quản thực phẩm trong thai kỳ.'),
        (13, 7, 'Bài tập nhẹ cho mẹ', 'community-bai-tap-nhe', 'Đi bộ, giãn cơ và vận động nhẹ khi không có chống chỉ định.'),
        (14, 7, 'Cải thiện giấc ngủ', 'community-cai-thien-giac-ngu', 'Tư thế và thói quen giúp mẹ ngủ thoải mái hơn.'),
        (15, 8, 'Đau bụng và ra máu', 'community-dau-bung-ra-mau', 'Nhận biết dấu hiệu cần liên hệ cơ sở y tế ngay.'),
        (16, 8, 'Đau đầu và huyết áp', 'community-dau-dau-huyet-ap', 'Theo dõi đau đầu, huyết áp và các triệu chứng đi kèm.'),
        (17, 9, 'Tiểu đường thai kỳ', 'community-tieu-duong-thai-ky', 'Chia sẻ cách theo dõi đường huyết và lịch tái khám.'),
        (18, 9, 'Tiền sản giật', 'community-tien-san-giat', 'Thông tin an toàn và dấu hiệu cảnh báo cần khám khẩn.'),
        (19, 10, 'Dấu hiệu chuyển dạ', 'community-dau-hieu-chuyen-da', 'Phân biệt cơn gò và chuẩn bị thời điểm đến viện.'),
        (20, 10, 'Chuẩn bị đồ đi sinh', 'community-do-di-sinh', 'Danh sách đồ dùng thiết thực cho mẹ, bé và người đồng hành.'),
        (21, 11, 'Chăm sóc vết sinh', 'community-cham-soc-vet-sinh', 'Vệ sinh, nghỉ ngơi và dấu hiệu vết thương cần được kiểm tra.'),
        (22, 11, 'Sức khỏe sàn chậu', 'community-suc-khoe-san-chau', 'Phục hồi sàn chậu và trao đổi với nhân viên y tế sau sinh.'),
        (23, 12, 'Khớp ngậm và tư thế bú', 'community-khop-ngam-tu-the-bu', 'Điều chỉnh tư thế để mẹ và bé thoải mái hơn.'),
        (24, 12, 'Sữa mẹ và hút sữa', 'community-sua-me-hut-sua', 'Nhịp hút, bảo quản và sử dụng sữa mẹ an toàn.'),
        (25, 13, 'Tắm và chăm sóc rốn', 'community-tam-cham-soc-ron', 'Chăm sóc da và giữ cuống rốn sạch, khô.'),
        (26, 13, 'Vàng da và thân nhiệt', 'community-vang-da-than-nhiet', 'Theo dõi màu da, nhiệt độ và thời điểm cần khám.'),
        (27, 14, 'Nếp ngủ an toàn', 'community-nep-ngu-an-toan', 'Không gian ngủ an toàn và thói quen ngủ phù hợp theo tuổi.'),
        (28, 14, 'Bé quấy khóc ban đêm', 'community-be-quay-khoc-dem', 'Chia sẻ cách quan sát nhu cầu và trấn an bé.'),
        (29, 15, 'Lịch tiêm chủng', 'community-lich-tiem-chung', 'Theo dõi mũi tiêm và chuẩn bị trước ngày tiêm.'),
        (30, 15, 'Phòng bệnh theo mùa', 'community-phong-benh-theo-mua', 'Vệ sinh, hạn chế lây nhiễm và chăm sóc khi giao mùa.'),
        (31, 16, 'Mốc vận động', 'community-moc-van-dong', 'Theo dõi khả năng giữ đầu, lẫy, ngồi và bò của bé.'),
        (32, 16, 'Giao tiếp và ngôn ngữ sớm', 'community-ngon-ngu-som', 'Tương tác, đọc sách và trò chuyện cùng bé mỗi ngày.'),
        (33, 17, 'Lo âu trong thai kỳ', 'community-lo-au-thai-ky', 'Chia sẻ cảm xúc và tìm nguồn trợ giúp tin cậy.'),
        (34, 17, 'Trầm cảm sau sinh', 'community-tram-cam-sau-sinh', 'Nhận diện sớm, đồng hành và kết nối hỗ trợ chuyên môn.'),
        (35, 18, 'Chăm sóc mẹ cùng bạn đời', 'community-cham-soc-me-cung-ban-doi', 'Những việc cụ thể người đồng hành có thể chủ động làm.'),
        (36, 18, 'Kỹ năng chăm bé cho bố', 'community-ky-nang-cham-be-cho-bo', 'Tắm, thay tã, bế và dỗ bé một cách an toàn.'),
        (37, 19, 'Giao tiếp trong gia đình', 'community-giao-tiep-gia-dinh', 'Trao đổi nhu cầu và thiết lập ranh giới tôn trọng.'),
        (38, 19, 'Chia sẻ việc nhà', 'community-chia-se-viec-nha', 'Phân công việc chăm mẹ, chăm bé và việc nhà hợp lý.'),
        (39, 20, 'Chọn cơ sở khám chữa bệnh', 'community-chon-co-so-y-te', 'Kinh nghiệm đánh giá khoảng cách, dịch vụ và khả năng cấp cứu.'),
        (40, 20, 'Bảo hiểm và hồ sơ y tế', 'community-bao-hiem-ho-so-y-te', 'Chuẩn bị giấy tờ, quyền lợi và lưu trữ kết quả khám.' )
    )
    INSERT INTO public.community_topics
        (id, name, description, icon, type, slug, parent_id, is_hidden, sort_order, created_by, created_at, updated_at)
    SELECT ('c2000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           name, description, 'forum', 'TOPIC', slug,
           ('c1000000-0000-4000-8000-' || lpad(category_seq::text, 12, '0'))::uuid,
           false, seq * 10, v_content_admin,
           v_now - interval '89 days', v_now - interval '2 days'
      FROM topic_seed
    ON CONFLICT (id) DO NOTHING;

    -- Sixty-four questions: 24 public feed items, 20 first-review items, 12 actively being
    -- scanned by AI, and eight examples hidden/locked after safety review.
    WITH question_seed(
        seq, topic_seq, author_kind, author_idx, title, body, stage, urgency,
        pregnancy_week, baby_age_months, anonymous, status
    ) AS (VALUES
        (1, 1, 'M', 1, 'Nên khám những gì trước khi bắt đầu mang thai?', 'Vợ chồng mình dự định có em bé vào cuối năm. Mình đã tiêm phòng từ trước nhưng chưa biết lần khám tiền thai nên mang theo hồ sơ gì và cần trao đổi những bệnh sử nào với bác sĩ.', 'PRE_PREGNANCY', 'NORMAL', NULL, NULL, false, 'APPROVED'),
        (2, 2, 'F', 1, 'Uống axit folic trước thai kỳ bao lâu là hợp lý?', 'Hai vợ chồng đang chuẩn bị có em bé. Mình muốn nhắc vợ bổ sung axit folic nhưng không muốn tự mua liều cao. Mọi người thường hỏi bác sĩ và chuẩn bị từ thời điểm nào?', 'PRE_PREGNANCY', 'LOW', NULL, NULL, false, 'APPROVED'),
        (3, 3, 'M', 2, 'Thai 6 tuần chỉ căng ngực và mệt có bình thường không?', 'Mình thử que hai vạch, tính theo kỳ kinh thì khoảng 6 tuần. Mấy hôm nay chỉ thấy căng ngực, buồn ngủ, không nghén nhiều nên hơi lo. Lịch hẹn siêu âm của mình còn năm ngày nữa.', 'PREGNANCY', 'NORMAL', 6, NULL, false, 'APPROVED'),
        (4, 4, 'M', 3, 'Mẹo ăn từng bữa nhỏ khi nghén buổi chiều', 'Mình nghén nhiều từ khoảng 3 giờ chiều, ngửi mùi cơm nóng là buồn nôn nhưng vẫn uống được nước. Có món nhẹ nào các mẹ thấy dễ ăn mà vẫn đủ năng lượng không?', 'PREGNANCY', 'NORMAL', 9, NULL, false, 'APPROVED'),
        (5, 5, 'M', 4, 'Thai máy tuần 19 lúc có lúc không', 'Mình mang thai lần đầu, tuần 19 mới cảm giác như cá quẫy nhẹ. Có hôm thấy vài lần, có hôm gần như không để ý thấy. Ở giai đoạn này mọi người theo dõi thai máy thế nào?', 'PREGNANCY', 'NORMAL', 19, NULL, false, 'APPROVED'),
        (6, 6, 'F', 2, 'Giúp vợ giảm chuột rút bắp chân ban đêm', 'Vợ mình tuần 23 hay bị chuột rút lúc gần sáng, ban ngày vẫn đi lại bình thường. Mình đang giúp kê gối và xoa nhẹ, muốn hỏi thêm cách hỗ trợ an toàn và khi nào nên báo bác sĩ.', 'PREGNANCY', 'NORMAL', 23, NULL, false, 'APPROVED'),
        (7, 7, 'M', 5, 'Ghi nhận cử động thai sao cho đỡ áp lực?', 'Mình tuần 31, bé thường hoạt động rõ sau bữa tối. Mỗi lần bận việc không ghi đủ lại thấy lo. Có cách tạo thói quen theo dõi cử động thai mà không khiến bản thân căng thẳng quá không?', 'PREGNANCY', 'NORMAL', 31, NULL, false, 'APPROVED'),
        (8, 8, 'M', 6, 'Phù chân nhẹ cuối ngày ở tuần 34', 'Buổi sáng chân mình bình thường nhưng cuối ngày hai mắt cá hơi phù, nghỉ và kê cao thì giảm. Huyết áp lần khám gần nhất bình thường. Mình nên tiếp tục quan sát những dấu hiệu nào?', 'PREGNANCY', 'NORMAL', 34, NULL, false, 'APPROVED'),
        (9, 9, 'F', 3, 'Chuẩn bị câu hỏi cho lịch khám thai tuần 28', 'Tuần tới mình đưa vợ đi khám mốc 28 tuần. Hai vợ chồng thường quên câu hỏi khi vào phòng khám, mọi người có mẫu ghi chú ngắn nào để theo dõi thuốc, xét nghiệm và dấu hiệu cần lưu ý không?', 'PREGNANCY', 'LOW', 27, NULL, false, 'APPROVED'),
        (10, 10, 'M', 1, 'Đọc kết quả siêu âm mà không tự làm mình lo lắng', 'Mình vừa nhận phiếu siêu âm hình thái với nhiều chỉ số viết tắt. Bác sĩ hẹn giải thích trong buổi khám chiều mai. Trong lúc chờ mình nên ghi lại câu hỏi nào thay vì tự so từng con số trên mạng?', 'PREGNANCY', 'NORMAL', 22, NULL, false, 'APPROVED'),
        (11, 11, 'M', 2, 'Gợi ý bữa sáng nhanh cho mẹ bầu đi làm sớm', 'Mình phải ra khỏi nhà lúc 6 giờ 30, nếu ăn quá no sẽ khó chịu nhưng bỏ bữa thì mệt. Mọi người có bữa sáng nào chuẩn bị từ tối, dễ mang theo và không quá ngọt không?', 'PREGNANCY', 'LOW', 17, NULL, false, 'APPROVED'),
        (12, 12, 'F', 4, 'Rửa và bảo quản rau sống cho vợ mang thai', 'Nhà mình hay ăn rau sống nhưng từ khi vợ có thai thì cả hai khá dè dặt. Mình muốn chuyển sang món chín và bảo quản thực phẩm tốt hơn. Có nguyên tắc bếp sạch nào dễ áp dụng mỗi ngày?', 'PREGNANCY', 'NORMAL', 16, NULL, false, 'APPROVED'),
        (13, 13, 'M', 3, 'Đi bộ 20 phút ở tam cá nguyệt hai', 'Trước thai kỳ mình ít vận động. Hiện tuần 21, bác sĩ nói thai kỳ ổn và có thể vận động nhẹ. Mình định bắt đầu đi bộ 15 đến 20 phút, nên tăng dần thế nào để không bị quá sức?', 'PREGNANCY', 'LOW', 21, NULL, false, 'APPROVED'),
        (14, 14, 'M', 4, 'Tư thế ngủ khi bụng bắt đầu lớn', 'Từ tuần 25 mình trở mình nhiều và hay tỉnh giấc vì mỏi hông. Mình có gối ôm dài nhưng chưa biết kê ở chân hay dưới bụng sẽ dễ chịu hơn. Các mẹ chia sẻ cách sắp gối được không?', 'PREGNANCY', 'NORMAL', 25, NULL, false, 'APPROVED'),
        (15, 15, 'M', 5, 'Đau bụng lâm râm: nên ghi lại thông tin gì trước khi gọi viện?', 'Mình tuần 12, thỉnh thoảng đau lâm râm vài phút rồi hết, hiện không ra máu. Mình sẽ gọi nơi đang theo dõi thai để hỏi. Trước khi gọi nên ghi thời điểm, mức đau và triệu chứng nào để mô tả rõ hơn?', 'PREGNANCY', 'URGENT', 12, NULL, true, 'APPROVED'),
        (16, 16, 'F', 5, 'Máy đo huyết áp tại nhà nên ghi kết quả ra sao?', 'Vợ mình được dặn theo dõi huyết áp một tuần vì gần đây hay đau đầu. Hai vợ chồng đã có máy đo bắp tay. Mình muốn lập bảng giờ đo, chỉ số và triệu chứng để mang đi tái khám.', 'PREGNANCY', 'URGENT', 30, NULL, false, 'APPROVED'),
        (17, 17, 'M', 6, 'Chuẩn bị bữa phụ khi theo dõi tiểu đường thai kỳ', 'Mình đang theo dõi đường huyết theo lịch của bác sĩ. Giữa hai bữa chính đôi lúc đói nhưng chưa biết chọn bữa phụ thế nào để dễ ghi nhận phản ứng của cơ thể.', 'PREGNANCY', 'NORMAL', 29, NULL, false, 'APPROVED'),
        (18, 18, 'F', 6, 'Người nhà cần nhớ dấu hiệu cảnh báo tiền sản giật nào?', 'Chị gái mình đang mang thai tuần 32 và được theo dõi sát huyết áp. Gia đình muốn thống nhất các dấu hiệu phải đưa đi viện ngay để không lúng túng khi chỉ có một người ở nhà.', 'PREGNANCY', 'URGENT', 32, NULL, false, 'APPROVED'),
        (19, 19, 'M', 1, 'Cơn gò luyện tập khác chuyển dạ thật như thế nào?', 'Mình tuần 37, bụng thỉnh thoảng cứng nhưng nghỉ một lúc thì dịu. Túi đi sinh đã sẵn sàng. Mình muốn biết nên quan sát nhịp cơn gò và dấu hiệu đi kèm ra sao trước khi gọi viện.', 'PREGNANCY', 'NORMAL', 37, NULL, false, 'APPROVED'),
        (20, 20, 'F', 2, 'Danh sách đồ đi sinh gọn cho người đồng hành', 'Bệnh viện đã cung cấp một phần đồ cho mẹ và bé. Mình muốn chuẩn bị một túi nhỏ dễ tìm, không mang dư quá nhiều. Những món nào người đồng hành thường cần nhất trong 24 giờ đầu?', 'PREGNANCY', 'LOW', 36, NULL, false, 'APPROVED'),
        (21, 21, 'M', 2, 'Vết mổ sau sinh ngày thứ sáu hơi căng khi đứng lên', 'Mình sinh mổ được sáu ngày, vết mổ khô, không sốt nhưng hơi căng khi đổi tư thế. Mình vẫn có lịch kiểm tra. Các mẹ thường đỡ bụng và đứng lên thế nào để bớt khó chịu?', 'POSTPARTUM', 'NORMAL', NULL, 0, false, 'APPROVED'),
        (22, 22, 'M', 3, 'Bắt đầu bài tập sàn chậu sau sinh khi nào?', 'Mình sinh thường được gần bốn tuần, sản dịch đã giảm và không có biến chứng. Mình muốn hỏi nhân viên y tế về phục hồi sàn chậu, trước buổi khám nên ghi nhận những triệu chứng nào?', 'POSTPARTUM', 'LOW', NULL, 1, false, 'APPROVED'),
        (23, 23, 'M', 4, 'Bé hay tuột khớp ngậm vào cuối cữ bú', 'Bé nhà mình ba tuần tuổi, đầu cữ bú khá tốt nhưng cuối cữ hay tuột khớp ngậm khiến đầu ti đau. Mình đã đặt lịch tư vấn sữa mẹ, muốn xin thêm kinh nghiệm chỉnh lại tư thế.', 'POSTPARTUM', 'NORMAL', NULL, 1, false, 'APPROVED'),
        (24, 24, 'F', 3, 'Chia ca rửa và tiệt trùng dụng cụ hút sữa', 'Vợ mình hút sữa vài cữ mỗi ngày. Mình phụ trách rửa dụng cụ nhưng ban đêm khá rối. Gia đình nào có cách chia bộ dụng cụ, ghi giờ sữa và phân ca để cả hai được nghỉ hơn không?', 'POSTPARTUM', 'LOW', NULL, 2, false, 'APPROVED'),

        (25, 25, 'M', 5, 'Chăm cuống rốn khô trong những ngày đầu', 'Bé được tám ngày, cuống rốn khô và chưa rụng. Mình lau tay sạch trước khi chăm bé và giữ vùng rốn thoáng. Xin hỏi cần quan sát thêm mùi, dịch hay vùng da xung quanh thế nào?', 'POSTPARTUM', 'NORMAL', NULL, 0, false, 'PENDING'),
        (26, 26, 'F', 4, 'Theo dõi vàng da ở nhà trước lịch tái khám', 'Bé nhà mình được bác sĩ hẹn tái khám vàng da vào sáng mai. Trong tối nay gia đình nên theo dõi bú, đánh thức và màu da dưới ánh sáng tự nhiên như thế nào để báo lại đầy đủ?', 'POSTPARTUM', 'URGENT', NULL, 0, false, 'PENDING'),
        (27, 27, 'M', 6, 'Sắp xếp chỗ ngủ an toàn trong phòng nhỏ', 'Phòng ngủ nhà mình khá nhỏ, bé có cũi riêng cạnh giường. Mình muốn bỏ bớt gối và chăn dư, nhờ mọi người góp ý cách sắp xếp đồ dùng cần thiết nhưng vẫn giữ bề mặt ngủ thoáng.', 'POSTPARTUM', 'NORMAL', NULL, 2, false, 'PENDING'),
        (28, 28, 'F', 5, 'Bố thay ca dỗ bé quấy từ 1 đến 3 giờ sáng', 'Bé gần hai tháng thường có một khung giờ quấy ban đêm. Mình muốn thay ca để vợ ngủ liền một mạch hơn. Ngoài kiểm tra tã, nhiệt độ và dấu hiệu đói, mình nên ghi lại điều gì?', 'POSTPARTUM', 'NORMAL', NULL, 2, false, 'PENDING'),
        (29, 29, 'M', 1, 'Chuẩn bị cho mũi tiêm đầu tiên của bé', 'Tuần sau bé có lịch tiêm. Mình đã giữ sổ tiêm và muốn chuẩn bị quần áo, giờ bú cùng những câu hỏi về phản ứng sau tiêm. Các mẹ thường mang theo gì?', 'POSTPARTUM', 'LOW', NULL, 2, false, 'PENDING'),
        (30, 30, 'F', 6, 'Hạn chế lây cảm cúm khi trong nhà có trẻ nhỏ', 'Mình hơi sổ mũi và đang chủ động đeo khẩu trang, rửa tay, không hôn bé. Gia đình muốn thống nhất thêm cách vệ sinh bề mặt và theo dõi triệu chứng cho cả mẹ lẫn bé.', 'POSTPARTUM', 'NORMAL', NULL, 3, false, 'PENDING'),
        (31, 31, 'M', 2, 'Bé ba tháng tập giữ đầu được bao lâu?', 'Khi nằm sấp có giám sát, bé nhà mình giữ đầu được một lúc rồi mệt. Mình không muốn so sánh máy móc với các bé khác, chỉ muốn biết cách ghi nhận tiến bộ để hỏi bác sĩ.', 'POSTPARTUM', 'LOW', NULL, 3, false, 'PENDING'),
        (32, 32, 'F', 1, 'Bố đọc sách cho bé hai tháng có sớm không?', 'Mình thường đọc vài phút bằng giọng chậm khi bé tỉnh táo. Bé nhìn mặt và đôi lúc ê a. Hoạt động ngắn như vậy nên duy trì vào khung giờ nào để không làm bé quá kích thích?', 'POSTPARTUM', 'LOW', NULL, 2, false, 'PENDING'),
        (33, 33, 'M', 3, 'Lo trước mỗi lần khám thai dù kết quả vẫn ổn', 'Mình cứ gần ngày khám là khó ngủ và nghĩ đến nhiều tình huống xấu, dù các lần khám trước bác sĩ nói thai phát triển phù hợp. Mình muốn tìm cách điều hòa lo âu và biết khi nào nên gặp chuyên gia.', 'PREGNANCY', 'NORMAL', 24, NULL, true, 'PENDING'),
        (34, 34, 'F', 2, 'Làm sao hỏi han mẹ sau sinh mà không gây áp lực?', 'Vợ mình ba tuần sau sinh hay im lặng và dễ khóc. Mình đã chủ động làm việc nhà nhưng không biết mở lời ra sao. Mình muốn lắng nghe đúng cách và cùng vợ tìm hỗ trợ nếu cần.', 'POSTPARTUM', 'URGENT', NULL, 1, false, 'PENDING'),
        (35, 35, 'F', 3, 'Lập lịch chăm mẹ trong hai tuần đầu sau sinh', 'Mình muốn chia lịch nấu ăn, trông bé và đưa mẹ đi khám để vợ không phải nhắc từng việc. Có gia đình nào dùng bảng ca đơn giản mà hiệu quả không?', 'POSTPARTUM', 'LOW', NULL, 0, false, 'PENDING'),
        (36, 36, 'F', 4, 'Tập bế và thay tã trước ngày đón bé', 'Mình chưa từng chăm trẻ sơ sinh và khá vụng. Hai vợ chồng định học lớp tiền sản. Ngoài bế đỡ đầu cổ và thay tã, bố nên tập thêm kỹ năng nào?', 'PREGNANCY', 'LOW', 33, NULL, false, 'PENDING'),
        (37, 37, 'M', 4, 'Nói chuyện với ông bà về cách chăm bé mới', 'Ông bà rất thương cháu nhưng một vài kinh nghiệm cũ khác với hướng dẫn của bệnh viện. Mình muốn trao đổi tôn trọng, có tài liệu rõ ràng và không biến cuộc nói chuyện thành tranh cãi.', 'POSTPARTUM', 'NORMAL', NULL, 1, true, 'PENDING'),
        (38, 38, 'F', 5, 'Chia việc nhà khi cả hai đều thiếu ngủ', 'Từ ngày có bé, việc nhỏ cũng dễ bị quên. Hai vợ chồng muốn chia việc theo mức ưu tiên thay vì ai thấy thì làm. Mọi người có cách kiểm tra nhanh mỗi sáng không?', 'POSTPARTUM', 'LOW', NULL, 2, false, 'PENDING'),
        (39, 39, 'M', 5, 'Tiêu chí chọn nơi khám thai gần nhà', 'Mình mới chuyển chỗ ở, đang cân nhắc phòng khám gần nhà và bệnh viện xa hơn. Ngoài chuyên môn, giờ hoạt động và khả năng cấp cứu, mình nên hỏi thêm tiêu chí nào?', 'PREGNANCY', 'NORMAL', 15, NULL, false, 'PENDING'),
        (40, 40, 'F', 6, 'Sắp xếp hồ sơ khám thai để đi viện nhanh', 'Nhà mình có nhiều phiếu siêu âm và xét nghiệm theo từng mốc. Mình định chia túi theo tháng và lưu bản chụp có mật khẩu. Có cách nào gọn hơn mà vẫn dễ đưa bác sĩ xem?', 'PREGNANCY', 'LOW', 35, NULL, false, 'PENDING'),
        (41, 11, 'M', 1, 'Bữa trưa văn phòng ít dầu mỡ cho tuần 20', 'Căng tin công ty thường nhiều món chiên. Mình đang thử mang cơm ba ngày mỗi tuần, ưu tiên rau chín và đạm dễ ăn. Xin thêm gợi ý đổi món để không ngán.', 'PREGNANCY', 'LOW', 20, NULL, false, 'PENDING'),
        (42, 14, 'M', 2, 'Thức giấc nhiều lần vì phải đi vệ sinh', 'Mình tuần 30, ban đêm thức dậy vài lần nên sáng khá mệt. Mình vẫn uống đủ nước ban ngày và không có đau buốt. Có thói quen buổi tối nào giúp ngủ lại dễ hơn?', 'PREGNANCY', 'NORMAL', 30, NULL, false, 'PENDING'),
        (43, 20, 'F', 1, 'Người đồng hành có cần chuẩn bị giấy tờ riêng?', 'Bệnh viện đã dặn giấy tờ của mẹ và hồ sơ thai. Mình là người đi cùng, muốn chuẩn bị giấy tờ, số điện thoại và phương án thanh toán để lúc nhập viện không mất thời gian.', 'PREGNANCY', 'LOW', 38, NULL, false, 'PENDING'),
        (44, 23, 'M', 6, 'Đầu ti đau khi bé mới bắt đầu bú', 'Bé được mười ngày. Mình đau nhiều nhất lúc bé mới ngậm rồi giảm dần, da chưa chảy máu. Mình muốn được kiểm tra khớp ngậm và xin kinh nghiệm chuẩn bị cho buổi tư vấn.', 'POSTPARTUM', 'NORMAL', NULL, 0, true, 'PENDING'),

        (45, 4, 'M', 1, 'Nghén nhưng vẫn uống được nước, cần theo dõi gì?', 'Mình đang tuần 8, buồn nôn nhiều vào buổi sáng nhưng vẫn uống từng ngụm và đi tiểu bình thường. Mình vừa đăng câu hỏi và đang chờ hệ thống kiểm tra nội dung.', 'PREGNANCY', 'NORMAL', 8, NULL, false, 'AI_PENDING'),
        (46, 7, 'F', 2, 'Cùng vợ ghi giờ thai máy vào buổi tối', 'Vợ mình tuần 29 thấy bé hoạt động rõ sau bữa tối. Hai vợ chồng muốn ghi ngắn gọn trong ứng dụng để mang theo khi khám, nội dung đang được kiểm tra tự động.', 'PREGNANCY', 'LOW', 29, NULL, false, 'AI_PENDING'),
        (47, 9, 'M', 3, 'Lần khám tới nên hỏi lại lịch xét nghiệm nào?', 'Mình tuần 25 và có nhiều mốc ghi chú khác nhau. Mình muốn gom câu hỏi để xác nhận trực tiếp với bác sĩ, hiện bài đăng đang được hệ thống rà soát.', 'PREGNANCY', 'LOW', 25, NULL, false, 'AI_PENDING'),
        (48, 13, 'M', 4, 'Khởi động trước khi đi bộ nhẹ trong thai kỳ', 'Bác sĩ cho phép mình vận động nhẹ. Mình muốn hỏi cách khởi động và nhận biết khi cần dừng, câu hỏi hiện đang chờ kiểm tra an toàn.', 'PREGNANCY', 'NORMAL', 18, NULL, false, 'AI_PENDING'),
        (49, 24, 'F', 3, 'Ghi nhãn sữa mẹ theo cữ hút ban đêm', 'Mình phụ trách ghi giờ và xếp sữa vào ngăn mát. Muốn xin mẫu nhãn đơn giản để tránh nhầm thứ tự sử dụng, bài đang được AI xử lý.', 'POSTPARTUM', 'LOW', NULL, 2, false, 'AI_PENDING'),
        (50, 27, 'M', 5, 'Kiểm tra lại không gian ngủ của bé', 'Mình đã dọn chăn, gối và đồ chơi khỏi cũi, đặt cũi gần giường. Nhờ cộng đồng góp ý thêm sau khi nội dung hoàn tất kiểm tra.', 'POSTPARTUM', 'NORMAL', NULL, 1, false, 'AI_PENDING'),
        (51, 33, 'M', 6, 'Tìm nhóm hỗ trợ cảm xúc cho mẹ bầu', 'Gần đây mình dễ căng thẳng và muốn tìm nhóm chia sẻ có kiểm duyệt. Mình không có ý nghĩ làm hại bản thân, chỉ muốn chủ động được hỗ trợ sớm.', 'PREGNANCY', 'NORMAL', 28, NULL, true, 'AI_PENDING'),
        (52, 36, 'F', 4, 'Bố luyện thay tã bằng búp bê trước ngày sinh', 'Mình đang tập thao tác chậm, luôn đỡ đầu cổ và chuẩn bị sẵn đồ. Muốn hỏi các bố mẹ điều gì từng khiến mình bất ngờ trong tuần đầu.', 'PREGNANCY', 'LOW', 35, NULL, false, 'AI_PENDING'),

        (53, 2, 'M', 2, 'Tự tăng liều vitamin để có thai nhanh hơn?', 'Một tài khoản khẳng định cứ tăng gấp nhiều lần liều vitamin là sẽ dễ có thai và bảo mọi người không cần hỏi bác sĩ. Nội dung này đã bị ẩn vì lời khuyên y tế không an toàn.', 'PRE_PREGNANCY', 'URGENT', NULL, NULL, false, 'HIDDEN'),
        (54, 10, 'F', 5, 'Nhận đọc kết quả siêu âm trả phí qua tin nhắn', 'Bài đăng mời chuyển khoản để được chẩn đoán từ ảnh siêu âm, không cung cấp danh tính hay chứng chỉ chuyên môn. Nội dung đã bị ẩn để kiểm tra dấu hiệu mạo danh.', 'PREGNANCY', 'NORMAL', 22, NULL, false, 'HIDDEN'),
        (55, 17, 'M', 3, 'Bỏ theo dõi đường huyết vì thấy người vẫn khỏe?', 'Bài viết kêu gọi mẹ bầu tự ngừng theo dõi và bỏ lịch tái khám tiểu đường thai kỳ. Nội dung đã bị khóa do có thể khiến người đọc trì hoãn chăm sóc cần thiết.', 'PREGNANCY', 'URGENT', 30, NULL, true, 'LOCKED'),
        (56, 18, 'F', 6, 'Công kích mẹ bầu có chỉ số huyết áp cao', 'Bài đăng dùng lời lẽ miệt thị, đổ lỗi cho người đang được theo dõi nguy cơ tiền sản giật. Nội dung đã bị ẩn vì quấy rối và gây tổn thương.', 'PREGNANCY', 'NORMAL', 33, NULL, false, 'HIDDEN'),
        (57, 23, 'M', 1, 'Cam kết chữa tắc tia sữa chỉ sau một lần mua thuốc', 'Bài đăng quảng cáo sản phẩm không rõ nguồn gốc, hứa chữa khỏi ngay và yêu cầu cung cấp số điện thoại cá nhân. Nội dung đã bị khóa để ngăn quảng cáo trá hình.', 'POSTPARTUM', 'NORMAL', NULL, 1, false, 'LOCKED'),
        (58, 26, 'F', 2, 'Không cần khám khi trẻ sơ sinh vàng da?', 'Bài viết khuyên mọi gia đình bỏ lịch tái khám vàng da và áp dụng một mẹo truyền miệng thay thế. Nội dung đã bị khóa vì thông tin y tế nguy hiểm.', 'POSTPARTUM', 'URGENT', NULL, 0, false, 'LOCKED'),
        (59, 34, 'M', 4, 'Chế giễu người mẹ đang tìm hỗ trợ sau sinh', 'Bài đăng phủ nhận khó khăn tâm lý sau sinh và công kích người đang tìm kiếm hỗ trợ. Nội dung đã bị ẩn theo chính sách chống quấy rối.', 'POSTPARTUM', 'NORMAL', NULL, 2, true, 'HIDDEN'),
        (60, 29, 'F', 3, 'Bán suất tiêm chủng giá rẻ không cần hồ sơ', 'Bài đăng yêu cầu chuyển khoản giữ chỗ cho một gói tiêm không có cơ sở cung cấp rõ ràng và xin ảnh giấy tờ của trẻ. Nội dung đã bị khóa vì nguy cơ lừa đảo và lộ dữ liệu.', 'POSTPARTUM', 'URGENT', NULL, 3, false, 'LOCKED'),
        (61, 31, 'M', 2, 'Tập nằm sấp có giám sát cho bé ba tháng', 'Bé nhà mình tỉnh táo vào buổi sáng nên mình muốn chia thành vài lần tập ngắn, luôn có người lớn quan sát. Câu hỏi đang được hệ thống kiểm tra trước khi hiển thị.', 'POSTPARTUM', 'LOW', NULL, 3, false, 'AI_PENDING'),
        (62, 32, 'F', 1, 'Trò chuyện với bé trong lúc thay tã', 'Mình thường gọi tên đồ vật và đáp lại tiếng ê a của bé. Muốn hỏi cách giữ tương tác vui nhưng không kéo dài khi bé đã mệt, nội dung đang được rà soát.', 'POSTPARTUM', 'LOW', NULL, 2, false, 'AI_PENDING'),
        (63, 35, 'F', 3, 'Chuẩn bị nước và bữa nhẹ cho mẹ trong cữ bú', 'Mình đặt một giỏ nhỏ cạnh chỗ ngồi gồm nước, khăn sạch và món nhẹ đã chuẩn bị an toàn. Xin thêm kinh nghiệm của người đồng hành sau khi câu hỏi được duyệt.', 'POSTPARTUM', 'LOW', NULL, 1, false, 'AI_PENDING'),
        (64, 39, 'M', 4, 'Ghi lại số điện thoại cần thiết trước ngày dự sinh', 'Mình đang gom số phòng sinh, xe hỗ trợ và người thân vào một ghi chú ngoại tuyến để dùng khi mất mạng. Bài đăng đang chờ AI kiểm tra.', 'PREGNANCY', 'NORMAL', 36, NULL, false, 'AI_PENDING')
    )
    INSERT INTO public.community_content
        (content_id, topic_id, parent_content_id, author_user_id, content_type, title, body,
         stage, urgency, is_anonymous, moderation_status, pregnancy_week, baby_age_months,
         like_count, answer_count, is_expert_labeled, is_personal_experience, image_urls,
         experience_tag, created_at, updated_at)
    SELECT ('c3000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           ('c2000000-0000-4000-8000-' || lpad(topic_seq::text, 12, '0'))::uuid,
           NULL,
           CASE author_kind WHEN 'M' THEN v_mothers[author_idx] ELSE v_families[author_idx] END,
           'QUESTION', title, body, stage, urgency, anonymous, status,
           pregnancy_week::smallint, baby_age_months::smallint,
           CASE WHEN status = 'APPROVED' THEN 2 ELSE 0 END,
           CASE WHEN status = 'APPROVED' THEN 2 ELSE 0 END,
           false, false, '[]'::jsonb, NULL,
           v_now - make_interval(days => (65 - seq)),
           v_now - make_interval(days => (65 - seq)) + interval '15 minutes'
      FROM question_seed
    ON CONFLICT (content_id) DO NOTHING;

    -- Two tailored public replies for each approved question: one verified-expert response and
    -- one lived-experience response from a mother/family member.
    WITH approved_answer_seed(seq, question_seq, author_kind, author_idx, body, expert_labeled, personal, experience_tag) AS (VALUES
        (1, 1, 'E', 1, 'Một lần khám tiền thai thường bắt đầu bằng bệnh sử của cả hai vợ chồng, thuốc và thực phẩm bổ sung đang dùng, tiền sử tiêm chủng và bệnh mạn tính. Bạn nên mang kết quả khám gần đây, danh sách thuốc và ghi sẵn các câu hỏi; cơ sở khám sẽ chỉ định xét nghiệm phù hợp thay vì làm đồng loạt.', true, false, NULL),
        (2, 1, 'M', 3, 'Mình từng gom ảnh sổ tiêm, đơn thuốc cũ và ghi bệnh sử gia đình vào một trang giấy. Buổi khám nhờ vậy đỡ sót, bác sĩ cũng dễ đánh dấu phần cần bổ sung.', false, true, 'KINH_NGHIEM_CHUAN_BI'),
        (3, 2, 'E', 2, 'Axit folic thường được khuyến nghị bắt đầu trước khi mang thai, nhưng liều và thời gian cụ thể cần dựa vào tiền sử sức khỏe, thuốc đang dùng và nguy cơ cá nhân. Hai bạn nên hỏi bác sĩ hoặc dược sĩ lâm sàng, không tự tăng liều cao.', true, false, NULL),
        (4, 2, 'F', 4, 'Nhà mình đặt lịch nhắc uống cùng giờ và mang đúng hộp sản phẩm đến buổi khám tiền thai. Bác sĩ xem thành phần rồi mới hướng dẫn nên dùng tiếp loại nào.', false, true, 'NGUOI_DONG_HANH'),
        (5, 3, 'E', 3, 'Mức độ triệu chứng ở đầu thai kỳ rất khác nhau và ít nghén không tự nó cho biết thai phát triển ra sao. Bạn cứ giữ lịch hẹn; nếu đau bụng tăng, ra máu, choáng hoặc có dấu hiệu khiến bạn lo, hãy liên hệ cơ sở theo dõi sớm.', true, false, NULL),
        (6, 3, 'M', 5, 'Mình cũng gần như chỉ buồn ngủ ở tuần 6. Mình ghi triệu chứng, tránh tự so với người khác và chờ buổi siêu âm đúng hẹn nên tâm lý dễ chịu hơn.', false, true, 'KINH_NGHIEM_THAI_SOM'),
        (7, 4, 'E', 4, 'Có thể thử khẩu phần nhỏ, món nguội hoặc ít mùi, ăn chậm và uống từng ngụm giữa các bữa. Nếu không giữ được nước, tiểu ít, chóng mặt, sụt cân hoặc nôn kéo dài, bạn nên liên hệ cơ sở y tế để được đánh giá.', true, false, NULL),
        (8, 4, 'M', 1, 'Mình chuẩn bị bánh mì nhạt, sữa chua và trái cây đã rửa sạch thành phần nhỏ. Để nguội thức ăn một chút cũng giúp mình bớt nhạy mùi hơn.', false, true, 'KINH_NGHIEM_NGHEN'),
        (9, 5, 'E', 5, 'Ở thai kỳ đầu tiên, cảm nhận cử động có thể còn không đều trong giai đoạn này. Bạn nên trao đổi ở lần khám kế tiếp về mốc theo dõi phù hợp; nếu sau khi đã có nhịp quen thuộc mà cử động giảm rõ, hãy liên hệ nơi theo dõi thai.', true, false, NULL),
        (10, 5, 'M', 2, 'Tuần 19 mình chỉ nhận ra khi nằm yên sau bữa tối. Mình ghi chú nhẹ nhàng chứ chưa đặt mục tiêu đếm, đến các tuần sau cảm giác mới rõ và đều hơn.', false, true, 'KINH_NGHIEM_THAI_MAY'),
        (11, 6, 'E', 6, 'Kéo giãn bắp chân nhẹ, vận động đều và uống đủ nước có thể hữu ích nếu thai kỳ không có chống chỉ định. Không nên xoa bóp mạnh một chân đang sưng, nóng hoặc đau; những dấu hiệu đó cần được đánh giá y tế sớm.', true, false, NULL),
        (12, 6, 'F', 1, 'Mình để sẵn nước cạnh giường, nhắc vợ giãn bắp chân trước ngủ và bật đèn nhỏ để hỗ trợ khi chuột rút. Ghi lại ngày xảy ra giúp lần khám sau kể rõ hơn.', false, true, 'NGUOI_DONG_HANH'),
        (13, 7, 'E', 1, 'Hãy chọn một khoảng thời gian bé thường hoạt động, ngồi hoặc nằm thoải mái và làm theo hướng dẫn đếm cử động của cơ sở đang theo dõi. Điều quan trọng là nhận ra thay đổi rõ so với nhịp quen thuộc, không phải so với thai kỳ của người khác.', true, false, NULL),
        (14, 7, 'M', 4, 'Mình đặt một nhắc lịch sau bữa tối nhưng nếu bận thì chuyển sang lúc bé hay máy tiếp theo. Việc ghi theo xu hướng vài ngày giúp mình bớt áp lực hơn từng con số riêng lẻ.', false, true, 'KINH_NGHIEM_THEO_DOI'),
        (15, 8, 'E', 2, 'Phù nhẹ hai bên chân cuối ngày có thể gặp trong thai kỳ, nhưng phù tăng nhanh, đau đầu dữ dội, nhìn mờ, đau vùng thượng vị, khó thở tăng hoặc huyết áp cao cần được liên hệ khám ngay. Bạn nên tiếp tục theo dõi và giữ lịch khám.', true, false, NULL),
        (16, 8, 'M', 6, 'Mình mang giày rộng, đổi tư thế thường xuyên và kê chân khi nghỉ. Mỗi sáng mình quan sát mặt, tay, chân và ghi huyết áp theo đúng hướng dẫn của bác sĩ.', false, true, 'KINH_NGHIEM_CUOI_THAI_KY'),
        (17, 9, 'E', 3, 'Một ghi chú ngắn có thể gồm thuốc và vi chất đang dùng, triệu chứng mới, cử động thai, kết quả đo tại nhà và các xét nghiệm đã làm. Đánh dấu ba câu quan trọng nhất để hỏi trước, sau đó ghi lại hướng dẫn ngay trong buổi khám.', true, false, NULL),
        (18, 9, 'F', 3, 'Mình dùng ghi chú chung trên điện thoại để cả hai cùng thêm câu hỏi. Trước khi vào khám mình đọc lại theo thứ tự ưu tiên và chụp ảnh lịch hẹn mới.', false, true, 'NGUOI_DONG_HANH'),
        (19, 10, 'E', 4, 'Các chỉ số siêu âm cần được hiểu theo tuổi thai, diễn tiến qua nhiều lần đo và bối cảnh lâm sàng. Bạn nên ghi lại thuật ngữ chưa rõ, hỏi bác sĩ kết luận chính, điều gì cần theo dõi và mốc tái khám thay vì tự diễn giải một con số riêng lẻ.', true, false, NULL),
        (20, 10, 'M', 5, 'Mình từng tìm từng chỉ số rồi càng lo. Sau đó mình chỉ ghi ba câu: kết luận là gì, có cần theo dõi thêm không, và khi nào quay lại; buổi khám hiệu quả hơn hẳn.', false, true, 'KINH_NGHIEM_KHAM_THAI'),
        (21, 11, 'E', 5, 'Bữa sáng nên có nguồn tinh bột phù hợp, đạm và thực phẩm tươi đã chế biến an toàn. Khẩu phần cần tùy tình trạng dinh dưỡng và bệnh lý; nếu đang theo dõi đường huyết hoặc thiếu máu, hãy xin tư vấn cá nhân hóa.', true, false, NULL),
        (22, 11, 'M', 1, 'Mình luân phiên yến mạch nấu chín với trứng, bánh mì kèm đạm và sữa chua không quá ngọt. Chia hộp từ tối giúp sáng không bị bỏ bữa.', false, true, 'KINH_NGHIEM_BUA_SANG'),
        (23, 12, 'E', 6, 'Ưu tiên thực phẩm nấu chín kỹ, tách dụng cụ sống và chín, rửa tay và bề mặt chế biến, bảo quản lạnh đúng thời gian. Rau quả dùng trực tiếp cần rửa dưới vòi nước sạch; không dùng hóa chất tẩy rửa không dành cho thực phẩm.', true, false, NULL),
        (24, 12, 'F', 2, 'Nhà mình chia riêng thớt sống, thớt chín và ghi ngày lên hộp thức ăn. Những ngày bận thì chọn rau nấu chín để cả hai đỡ phải lo khâu sơ chế.', false, true, 'NGUOI_DONG_HANH'),
        (25, 13, 'E', 1, 'Nếu đã được xác nhận không có chống chỉ định, bạn có thể bắt đầu chậm ở mức vẫn nói chuyện được, khởi động và hạ nhịp nhẹ. Dừng vận động và liên hệ y tế khi đau ngực, khó thở bất thường, choáng, ra máu, rỉ ối hoặc đau tăng.', true, false, NULL),
        (26, 13, 'M', 3, 'Mình bắt đầu 10 phút quanh khu nhà, mang nước và đi cùng người thân. Sau một tuần thấy ổn mới tăng thêm vài phút, không cố đạt số bước vào ngày mệt.', false, true, 'KINH_NGHIEM_VAN_DONG'),
        (27, 14, 'E', 2, 'Bạn có thể thử nằm nghiêng ở tư thế dễ chịu, kê gối giữa hai đầu gối và đỡ dưới bụng hoặc sau lưng. Tránh tự ép mình giữ một tư thế cả đêm; hãy trao đổi nếu đau kéo dài hoặc kèm triệu chứng khác.', true, false, NULL),
        (28, 14, 'M', 2, 'Mình kê một phần gối dưới bụng và một phần giữa hai chân, thêm gối nhỏ sau lưng. Quan trọng nhất là thử từng cách vài đêm chứ không mua quá nhiều gối cùng lúc.', false, true, 'KINH_NGHIEM_GIAC_NGU'),
        (29, 15, 'E', 3, 'Khi gọi cơ sở y tế, hãy mô tả tuổi thai, vị trí và mức đau, thời điểm bắt đầu, kéo dài bao lâu, có lặp lại không, kèm ra máu, rỉ dịch, sốt, choáng hay thay đổi cử động thai. Nếu đau tăng hoặc có dấu hiệu cảnh báo, đừng chờ phản hồi trên cộng đồng.', true, false, NULL),
        (30, 15, 'M', 4, 'Mình lưu sẵn số khoa cấp cứu và ghi nhanh giờ bắt đầu, mức đau từ 0 đến 10. Nhờ vậy khi gọi mình nói rõ hơn và không quên chi tiết vì lo.', false, true, 'KINH_NGHIEM_LIEN_HE_Y_TE'),
        (31, 16, 'E', 4, 'Đo sau khi ngồi nghỉ, lưng và tay được đỡ, băng quấn đúng cỡ, ghi cả hai chỉ số, nhịp tim, giờ đo và triệu chứng. Hãy làm theo ngưỡng liên hệ mà bác sĩ đã dặn; đau đầu dữ dội, nhìn mờ hoặc đau thượng vị cần được đánh giá khẩn.', true, false, NULL),
        (32, 16, 'F', 5, 'Mình tạo bảng gồm giờ, huyết áp lần một, lần hai và triệu chứng. Hai vợ chồng không tự đổi thuốc theo con số mà gửi bảng cho bác sĩ đúng lịch.', false, true, 'NGUOI_DONG_HANH'),
        (33, 17, 'E', 5, 'Bữa phụ cần phù hợp kế hoạch dinh dưỡng và mục tiêu đường huyết cá nhân. Bạn có thể ghi món, lượng ăn, giờ ăn và kết quả theo lịch được hướng dẫn để chuyên viên dinh dưỡng hoặc bác sĩ điều chỉnh dựa trên dữ liệu thật.', true, false, NULL),
        (34, 17, 'M', 6, 'Mình chuẩn bị phần nhỏ, giữ giờ tương đối ổn định và ghi đúng món chứ không chỉ ghi “ăn nhẹ”. Mang nhật ký đi tái khám giúp mình nhận được góp ý cụ thể hơn.', false, true, 'KINH_NGHIEM_DUONG_HUYET'),
        (35, 18, 'E', 6, 'Gia đình nên nhớ các dấu hiệu như đau đầu dữ dội không giảm, nhìn mờ, đau vùng thượng vị, khó thở, co giật, phù tăng nhanh hoặc huyết áp chạm ngưỡng bác sĩ đã dặn. Khi xuất hiện, cần liên hệ cấp cứu hoặc đến cơ sở y tế ngay.', true, false, NULL),
        (36, 18, 'F', 6, 'Nhà mình dán số bệnh viện, tuyến đường và danh sách dấu hiệu ngay gần cửa. Mỗi người đều biết túi hồ sơ ở đâu nên khi cần không phải gọi hỏi nhau.', false, true, 'NGUOI_DONG_HANH'),
        (37, 19, 'E', 1, 'Cơn gò chuyển dạ thường có xu hướng đều hơn, mạnh hơn và không hết chỉ nhờ nghỉ hoặc đổi tư thế, nhưng mỗi người có thể khác. Rỉ ối, ra máu, cử động thai giảm hoặc bất kỳ dấu hiệu bất thường nào đều cần gọi nơi dự sinh ngay.', true, false, NULL),
        (38, 19, 'M', 1, 'Mình dùng đồng hồ ghi giờ bắt đầu và kết thúc, đồng thời để ý dịch và thai máy. Khi chưa chắc, mình gọi thẳng phòng sinh thay vì chờ hỏi trên mạng.', false, true, 'KINH_NGHIEM_CHUYEN_DA'),
        (39, 20, 'E', 2, 'Hãy ưu tiên giấy tờ, hồ sơ thai, thuốc đang dùng, đồ vệ sinh cá nhân và một bộ đồ phù hợp hướng dẫn bệnh viện. Gọi trước để biết nơi sinh đã cung cấp gì sẽ giúp tránh mang quá nhiều.', true, false, NULL),
        (40, 20, 'F', 2, 'Mình chia túi thành ba ngăn có nhãn: giấy tờ, đồ mẹ và đồ bé; sạc điện thoại để túi ngoài. Danh sách của bệnh viện là chuẩn chính, cộng đồng chỉ giúp mình sắp xếp.', false, true, 'NGUOI_DONG_HANH'),
        (41, 21, 'E', 3, 'Khi đổi tư thế, bạn có thể nghiêng người, chống tay và đứng dậy chậm, tránh gắng sức. Vết mổ đỏ lan, sưng nóng, chảy dịch, đau tăng, sốt hoặc cảm giác bất thường cần được cơ sở đã phẫu thuật kiểm tra sớm.', true, false, NULL),
        (42, 21, 'M', 5, 'Mình ôm gối nhỏ trước bụng khi ho hoặc đứng lên, xoay nghiêng rồi chống tay thay vì ngồi bật dậy. Mình vẫn kiểm tra vết mổ mỗi ngày và giữ đúng lịch hẹn.', false, true, 'KINH_NGHIEM_SAU_SINH'),
        (43, 22, 'E', 4, 'Thời điểm bắt đầu phụ thuộc quá trình sinh, triệu chứng và đánh giá sau sinh. Bạn nên ghi nhận đau, cảm giác nặng vùng chậu, són tiểu, khó đại tiện hoặc khó chịu khi vận động để được hướng dẫn bài tập phù hợp.', true, false, NULL),
        (44, 22, 'M', 3, 'Trước buổi khám mình ghi lúc nào bị són, hoạt động nào gây khó chịu và mức đau. Chuyên viên hướng dẫn rất nhẹ từ nhịp thở, khác hẳn việc tự tập theo video.', false, true, 'KINH_NGHIEM_PHUC_HOI'),
        (45, 23, 'E', 5, 'Đau kéo dài hoặc tổn thương đầu ti thường gợi ý cần quan sát trực tiếp tư thế và khớp ngậm. Trong lúc chờ, bạn có thể đổi tư thế, đưa bé sát người, hỗ trợ đầu cổ và ngắt khớp ngậm nhẹ nhàng nếu đau tăng.', true, false, NULL),
        (46, 23, 'M', 4, 'Mình quay một đoạn ngắn tư thế bú chỉ để xem cùng tư vấn viên, không đăng công khai. Sau khi chỉnh gối và đưa bé sát hơn, cuối cữ bé ít tuột hơn.', false, true, 'KINH_NGHIEM_CHO_BU'),
        (47, 24, 'E', 6, 'Dụng cụ cần được làm sạch theo hướng dẫn của nhà sản xuất và khuyến nghị của cơ sở y tế, để khô trên bề mặt sạch. Sữa nên được ghi thời điểm hút và bảo quản đúng nhiệt độ; nếu có trẻ sinh non hoặc bệnh lý, hãy xin hướng dẫn riêng.', true, false, NULL),
        (48, 24, 'F', 3, 'Nhà mình có hai khay: đồ sạch đã khô và đồ chờ rửa. Mình ghi giờ lên nhãn ngay sau cữ hút, xếp theo thứ tự cũ ở trước nên ban đêm ít nhầm hơn.', false, true, 'NGUOI_DONG_HANH')
    )
    INSERT INTO public.community_content
        (content_id, topic_id, parent_content_id, author_user_id, content_type, title, body,
         stage, urgency, is_anonymous, moderation_status, pregnancy_week, baby_age_months,
         like_count, answer_count, is_expert_labeled, is_personal_experience, image_urls,
         experience_tag, created_at, updated_at)
    SELECT ('c4000000-0000-4000-8000-' || lpad(seed.seq::text, 12, '0'))::uuid,
           q.topic_id,
           ('c3000000-0000-4000-8000-' || lpad(seed.question_seq::text, 12, '0'))::uuid,
           CASE seed.author_kind WHEN 'E' THEN v_experts[seed.author_idx]
                                 WHEN 'M' THEN v_mothers[seed.author_idx]
                                 ELSE v_families[seed.author_idx] END,
           'ANSWER', NULL, seed.body, q.stage, NULL, false, 'APPROVED',
           q.pregnancy_week, q.baby_age_months, 1, 0,
           seed.expert_labeled, seed.personal, '[]'::jsonb, seed.experience_tag,
           q.created_at + CASE WHEN seed.expert_labeled THEN interval '5 hours' ELSE interval '1 day' END,
           q.created_at + CASE WHEN seed.expert_labeled THEN interval '5 hours 10 minutes' ELSE interval '1 day 10 minutes' END
      FROM approved_answer_seed seed
      JOIN public.community_content q
        ON q.content_id = ('c3000000-0000-4000-8000-' || lpad(seed.question_seq::text, 12, '0'))::uuid
    ON CONFLICT (content_id) DO NOTHING;

    -- Twenty pending answers ensure both tabs of /moderator/pending-content have a full page.
    WITH pending_answer_seed(seq, question_seq, author_kind, author_idx, body, experience_tag) AS (VALUES
        (49, 1, 'M', 2, 'Mình đang gom hồ sơ khám và muốn hỏi thêm có cần mang kết quả xét nghiệm cách đây một năm không.', 'CHO_DUYET_KINH_NGHIEM'),
        (50, 2, 'F', 2, 'Mình đã đặt lịch tư vấn để kiểm tra đúng hàm lượng thay vì chọn theo quảng cáo trên mạng.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (51, 3, 'M', 3, 'Mình từng ghi từng triệu chứng theo ngày rồi mang đến lịch siêu âm, thấy dễ trao đổi hơn.', 'CHO_DUYET_KINH_NGHIEM'),
        (52, 4, 'M', 4, 'Khoai hấp để nguội bớt và bánh mì nhạt là hai món mình dễ ăn nhất khi nghén.', 'CHO_DUYET_KINH_NGHIEM'),
        (53, 5, 'F', 3, 'Vợ mình thường cảm nhận rõ hơn sau khi nghỉ tối, mình chỉ nhắc nhẹ chứ không tạo áp lực.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (54, 6, 'M', 5, 'Mình giãn chân nhẹ trước ngủ và báo bác sĩ khi chuột rút xuất hiện thường xuyên hơn.', 'CHO_DUYET_KINH_NGHIEM'),
        (55, 7, 'M', 6, 'Mình dùng một dấu chấm mỗi lần để ghi nhanh, cuối ngày mới thêm ghi chú nếu có gì khác thường.', 'CHO_DUYET_KINH_NGHIEM'),
        (56, 8, 'F', 4, 'Mình nhắc vợ kê chân và cùng theo dõi xem phù có giảm sau nghỉ hay không.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (57, 9, 'M', 1, 'Mình ghi ba câu quan trọng ở đầu trang để vào khám không bị quên.', 'CHO_DUYET_KINH_NGHIEM'),
        (58, 10, 'M', 2, 'Chờ bác sĩ kết luận giúp mình bình tĩnh hơn nhiều so với tự tìm từng chữ viết tắt.', 'CHO_DUYET_KINH_NGHIEM'),
        (59, 11, 'F', 5, 'Nhà mình chuẩn bị hộp bữa sáng từ tối và luôn có một món dự phòng dễ ăn.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (60, 12, 'M', 3, 'Từ khi tách thớt và hộp sống chín, mình thấy việc chuẩn bị đồ ăn đỡ lo hơn.', 'CHO_DUYET_KINH_NGHIEM'),
        (61, 13, 'M', 4, 'Mình bắt đầu rất chậm, ngày mệt thì nghỉ và không cố bù vào hôm sau.', 'CHO_DUYET_KINH_NGHIEM'),
        (62, 14, 'F', 1, 'Mình giúp sắp gối trước khi ngủ để vợ không phải kéo chỉnh nhiều lần trong đêm.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (63, 15, 'M', 6, 'Lưu số bệnh viện trong mục yêu thích là việc nhỏ nhưng giúp mình yên tâm hơn.', 'CHO_DUYET_KINH_NGHIEM'),
        (64, 16, 'F', 6, 'Mình luôn để vợ ngồi nghỉ đủ rồi mới đo và ghi cả triệu chứng chứ không chỉ ghi con số.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (65, 17, 'M', 2, 'Ghi chính xác lượng ăn giúp buổi tư vấn dinh dưỡng của mình cụ thể hơn.', 'CHO_DUYET_KINH_NGHIEM'),
        (66, 18, 'F', 2, 'Cả nhà mình đều lưu tuyến đường đến viện và số trực của khoa sản.', 'CHO_DUYET_NGUOI_DONG_HANH'),
        (67, 19, 'M', 5, 'Khi phân vân mình gọi thẳng nơi dự sinh, không chờ cơn gò phải giống mô tả của người khác.', 'CHO_DUYET_KINH_NGHIEM'),
        (68, 20, 'F', 4, 'Túi hồ sơ để riêng gần cửa giúp mình không phải mở tung vali lúc cần.', 'CHO_DUYET_NGUOI_DONG_HANH')
    )
    INSERT INTO public.community_content
        (content_id, topic_id, parent_content_id, author_user_id, content_type, title, body,
         stage, urgency, is_anonymous, moderation_status, pregnancy_week, baby_age_months,
         like_count, answer_count, is_expert_labeled, is_personal_experience, image_urls,
         experience_tag, created_at, updated_at)
    SELECT ('c4000000-0000-4000-8000-' || lpad(seed.seq::text, 12, '0'))::uuid,
           q.topic_id, q.content_id,
           CASE seed.author_kind WHEN 'M' THEN v_mothers[seed.author_idx] ELSE v_families[seed.author_idx] END,
           'ANSWER', NULL, seed.body, q.stage, NULL, false, 'PENDING',
           q.pregnancy_week, q.baby_age_months, 0, 0, false, true, '[]'::jsonb,
           seed.experience_tag, v_now - make_interval(days => (70 - seed.seq)),
           v_now - make_interval(days => (70 - seed.seq)) + interval '5 minutes'
      FROM pending_answer_seed seed
      JOIN public.community_content q
        ON q.content_id = ('c3000000-0000-4000-8000-' || lpad(seed.question_seq::text, 12, '0'))::uuid
    ON CONFLICT (content_id) DO NOTHING;

    -- Eight hidden unsafe/spam replies plus eight AI_PENDING replies exercise moderation and
    -- private "processing" states without exposing them in the public answer list.
    WITH nonpublic_answer_seed(seq, question_seq, author_kind, author_idx, body, status) AS (VALUES
        (69, 1, 'F', 5, 'Cứ mua gói thuốc của tôi là không cần khám tiền thai, nhắn số điện thoại để nhận giá ưu đãi.', 'HIDDEN'),
        (70, 2, 'M', 6, 'Tăng liều càng cao càng tốt, không cần đọc nhãn hay hỏi nhân viên y tế.', 'HIDDEN'),
        (71, 3, 'F', 6, 'Tôi có thể nhìn ảnh siêu âm và chẩn đoán chắc chắn nếu bạn chuyển khoản trước.', 'HIDDEN'),
        (72, 4, 'M', 5, 'Ai nghén mà than mệt là quá yếu đuối, đừng đăng bài làm phiền người khác.', 'HIDDEN'),
        (73, 5, 'F', 4, 'Bấm vào liên kết lạ này và gửi căn cước để nhận máy nghe tim thai miễn phí.', 'HIDDEN'),
        (74, 6, 'M', 4, 'Ngừng toàn bộ thuốc bác sĩ kê rồi dùng sản phẩm không nhãn này sẽ hết chuột rút.', 'HIDDEN'),
        (75, 7, 'F', 3, 'Không cần theo dõi thai máy vì ứng dụng của tôi cam kết biết chính xác tình trạng em bé.', 'HIDDEN'),
        (76, 8, 'M', 3, 'Phù chân thì tự dùng thuốc lợi tiểu của người nhà, không cần đo huyết áp.', 'HIDDEN'),
        (77, 9, 'M', 4, 'Mình có mẫu ghi chú lịch khám một trang, đang chờ hệ thống kiểm tra trước khi chia sẻ.', 'AI_PENDING'),
        (78, 10, 'F', 1, 'Mình thường cùng vợ khoanh phần bác sĩ kết luận trên phiếu, phản hồi đang được kiểm tra.', 'AI_PENDING'),
        (79, 11, 'M', 5, 'Mình gói bữa sáng thành phần nhỏ để mang theo, bài trả lời đang được AI rà soát.', 'AI_PENDING'),
        (80, 12, 'F', 2, 'Nhà mình dán nhãn thớt sống chín bằng màu khác nhau, đang chờ duyệt nội dung.', 'AI_PENDING'),
        (81, 13, 'M', 6, 'Đi cùng một người trong tuần đầu giúp mình tự tin hơn, phản hồi đang xử lý.', 'AI_PENDING'),
        (82, 14, 'F', 3, 'Mình giúp kê gối dưới chân và sau lưng rồi hỏi vợ tư thế nào dễ chịu nhất.', 'AI_PENDING'),
        (83, 15, 'M', 1, 'Ghi sẵn số viện và triệu chứng giúp mình mô tả rõ khi gọi, đang chờ kiểm tra.', 'AI_PENDING'),
        (84, 16, 'F', 5, 'Mình tạo bảng huyết áp dùng chung để hai vợ chồng cùng cập nhật, nội dung đang xử lý.', 'AI_PENDING')
    )
    INSERT INTO public.community_content
        (content_id, topic_id, parent_content_id, author_user_id, content_type, title, body,
         stage, urgency, is_anonymous, moderation_status, pregnancy_week, baby_age_months,
         like_count, answer_count, is_expert_labeled, is_personal_experience, image_urls,
         experience_tag, created_at, updated_at)
    SELECT ('c4000000-0000-4000-8000-' || lpad(seed.seq::text, 12, '0'))::uuid,
           q.topic_id, q.content_id,
           CASE seed.author_kind WHEN 'M' THEN v_mothers[seed.author_idx] ELSE v_families[seed.author_idx] END,
           'ANSWER', NULL, seed.body, q.stage, NULL, false, seed.status,
           q.pregnancy_week, q.baby_age_months, 0, 0, false,
           seed.status = 'AI_PENDING', '[]'::jsonb,
           CASE WHEN seed.status = 'AI_PENDING' THEN 'DANG_KIEM_TRA' ELSE NULL END,
           v_now - make_interval(days => greatest(1, 90 - seed.seq)),
           v_now - make_interval(days => greatest(1, 90 - seed.seq)) + interval '5 minutes'
      FROM nonpublic_answer_seed seed
      JOIN public.community_content q
        ON q.content_id = ('c3000000-0000-4000-8000-' || lpad(seed.question_seq::text, 12, '0'))::uuid
    ON CONFLICT (content_id) DO NOTHING;

    -- Saved questions: four bookmarks per mother/family pair, 24 deterministic rows total.
    INSERT INTO public.community_interactions
        (interaction_id, actor_user_id, interaction_type, content_id, topic_id, target_content_type, created_at)
    SELECT ('c5000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           CASE WHEN seq <= 12
                THEN v_mothers[((seq - 1) % 6) + 1]
                ELSE v_families[((seq - 13) % 6) + 1] END,
           'BOOKMARK',
           ('c3000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           NULL, 'QUESTION', v_now - make_interval(days => (25 - seq))
      FROM generate_series(1, 24) AS seed(seq)
    ON CONFLICT DO NOTHING;

    -- Two question reactions per public question.
    INSERT INTO public.community_interactions
        (interaction_id, actor_user_id, interaction_type, content_id, topic_id, target_content_type, created_at)
    SELECT ('c5000000-0000-4000-8000-' || lpad((100 + ((q.seq - 1) * 2) + r.reaction_no)::text, 12, '0'))::uuid,
           CASE r.reaction_no
                WHEN 1 THEN v_mothers[(q.seq % 6) + 1]
                ELSE v_families[((q.seq + 2) % 6) + 1] END,
           'REACTION',
           ('c3000000-0000-4000-8000-' || lpad(q.seq::text, 12, '0'))::uuid,
           NULL, 'QUESTION', v_now - make_interval(days => (25 - q.seq)) + make_interval(hours => r.reaction_no)
      FROM generate_series(1, 24) AS q(seq)
      CROSS JOIN generate_series(1, 2) AS r(reaction_no)
    ON CONFLICT DO NOTHING;

    -- One reaction for every public answer.
    INSERT INTO public.community_interactions
        (interaction_id, actor_user_id, interaction_type, content_id, topic_id, target_content_type, created_at)
    SELECT ('c5000000-0000-4000-8000-' || lpad((200 + seq)::text, 12, '0'))::uuid,
           CASE WHEN seq % 2 = 1 THEN v_families[(seq % 6) + 1] ELSE v_mothers[(seq % 6) + 1] END,
           'REACTION',
           ('c4000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           NULL, 'ANSWER', v_now - make_interval(days => greatest(1, 50 - seq))
      FROM generate_series(1, 48) AS seed(seq)
    ON CONFLICT DO NOTHING;

    -- Twenty-four topic follows spread across mother and family accounts.
    INSERT INTO public.community_interactions
        (interaction_id, actor_user_id, interaction_type, content_id, topic_id, target_content_type, created_at)
    SELECT ('c5000000-0000-4000-8000-' || lpad((300 + seq)::text, 12, '0'))::uuid,
           CASE WHEN seq % 2 = 1
                THEN v_mothers[((seq + 1) % 6) + 1]
                ELSE v_families[((seq + 1) % 6) + 1] END,
           'FOLLOW', NULL,
           ('c2000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           NULL, v_now - make_interval(days => (30 - seq))
      FROM generate_series(1, 24) AS seed(seq)
    ON CONFLICT DO NOTHING;

    -- Twenty-four moderation cases cover user reports, AI-origin reports, all queue states,
    -- question/answer/account targets and all supported priorities.
    WITH case_seed(
        seq, target_type, target_seq, target_user_kind, target_user_idx,
        reporter_kind, reporter_idx, reason_code, description, status, source, priority
    ) AS (VALUES
        (1, 'QUESTION', 53, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI phát hiện lời khuyên tự tăng liều vitamin và bỏ qua tư vấn chuyên môn.', 'PENDING', 'AUTOMATED', 'URGENT'),
        (2, 'QUESTION', 54, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI cảnh báo lời mời chẩn đoán trả phí không có thông tin chuyên môn xác thực.', 'PENDING', 'AUTOMATED', 'HIGH'),
        (3, 'QUESTION', 55, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI phát hiện nội dung khuyến khích ngừng theo dõi tiểu đường thai kỳ.', 'PENDING', 'AUTOMATED', 'URGENT'),
        (4, 'QUESTION', 56, NULL, NULL, NULL, NULL, 'HARASSMENT', 'AI phát hiện ngôn ngữ công kích người đang có thai kỳ nguy cơ cao.', 'PENDING', 'AUTOMATED', 'HIGH'),
        (5, 'QUESTION', 57, NULL, NULL, NULL, NULL, 'SPAM', 'AI nhận diện quảng cáo sản phẩm không rõ nguồn gốc và thu thập số điện thoại.', 'PENDING', 'AUTOMATED', 'NORMAL'),
        (6, 'QUESTION', 58, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI cảnh báo nội dung có thể làm trì hoãn khám vàng da sơ sinh.', 'PENDING', 'AUTOMATED', 'URGENT'),
        (7, 'QUESTION', 59, NULL, NULL, NULL, NULL, 'HARASSMENT', 'AI chuyển kiểm duyệt viên xem xét nội dung chế giễu sức khỏe tâm thần sau sinh.', 'IN_REVIEW', 'AUTOMATED', 'HIGH'),
        (8, 'QUESTION', 60, NULL, NULL, NULL, NULL, 'DISGUISED_ADVERTISING', 'AI phát hiện dấu hiệu lừa đảo, xin giấy tờ trẻ và yêu cầu chuyển khoản.', 'IN_REVIEW', 'AUTOMATED', 'URGENT'),
        (9, 'ANSWER', 69, NULL, NULL, NULL, NULL, 'SPAM', 'AI phát hiện trả lời bán thuốc và kéo người dùng ra khỏi nền tảng.', 'IN_REVIEW', 'AUTOMATED', 'NORMAL'),
        (10, 'ANSWER', 70, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI phát hiện khuyến nghị tăng liều vi chất không an toàn.', 'IN_REVIEW', 'AUTOMATED', 'URGENT'),
        (11, 'ANSWER', 71, NULL, NULL, NULL, NULL, 'UNSAFE_ADVICE', 'AI đánh dấu hành vi tự nhận chẩn đoán qua ảnh siêu âm.', 'RESOLVED', 'AUTOMATED', 'HIGH'),
        (12, 'ANSWER', 72, NULL, NULL, NULL, NULL, 'HARASSMENT', 'AI phát hiện lời lẽ hạ thấp người đang bị nghén.', 'RESOLVED', 'AUTOMATED', 'HIGH'),
        (13, 'ANSWER', 73, NULL, NULL, 'M', 1, 'SPAM', 'Người dùng báo cáo liên kết đáng ngờ xin ảnh giấy tờ cá nhân.', 'RESOLVED', 'USER', 'HIGH'),
        (14, 'ANSWER', 74, NULL, NULL, 'F', 1, 'UNSAFE_ADVICE', 'Người dùng báo cáo lời khuyên bỏ thuốc được bác sĩ kê.', 'RESOLVED', 'USER', 'URGENT'),
        (15, 'ANSWER', 75, NULL, NULL, 'M', 2, 'INACCURATE_INFORMATION', 'Người dùng báo cáo quảng cáo ứng dụng cam kết thay thế theo dõi thai.', 'RESOLVED', 'USER', 'NORMAL'),
        (16, 'ANSWER', 76, NULL, NULL, 'F', 2, 'UNSAFE_ADVICE', 'Người dùng báo cáo lời khuyên tự dùng thuốc của người khác.', 'RESOLVED', 'USER', 'URGENT'),
        (17, 'USER', NULL, 'M', 5, 'M', 3, 'HARASSMENT', 'Báo cáo tài khoản nhiều lần gửi tin nhắn công kích thành viên khác.', 'DISMISSED', 'USER', 'NORMAL'),
        (18, 'USER', NULL, 'F', 5, 'F', 3, 'SPAM', 'Báo cáo tài khoản đăng lặp lại liên kết bán hàng trong bình luận.', 'DISMISSED', 'USER', 'NORMAL'),
        (19, 'USER', NULL, 'M', 6, 'M', 4, 'OTHER', 'Báo cáo hành vi liên hệ ngoài nền tảng; kiểm tra không thấy đủ bằng chứng.', 'DISMISSED', 'USER', 'NORMAL'),
        (20, 'USER', NULL, 'F', 6, 'F', 4, 'HARASSMENT', 'Báo cáo lời nói thiếu tôn trọng; hai bên đã cung cấp bối cảnh khác nhau.', 'DISMISSED', 'USER', 'NORMAL'),
        (21, 'QUESTION', 5, NULL, NULL, 'M', 5, 'INACCURATE_INFORMATION', 'Người dùng muốn kiểm tra lại cách diễn đạt về thời điểm cảm nhận thai máy.', 'PENDING', 'USER', 'NORMAL'),
        (22, 'QUESTION', 8, NULL, NULL, 'F', 5, 'UNSAFE_ADVICE', 'Người dùng lo phần trao đổi về phù chân có thể bị hiểu là thay thế khám.', 'PENDING', 'USER', 'HIGH'),
        (23, 'ANSWER', 3, NULL, NULL, 'M', 6, 'INACCURATE_INFORMATION', 'Người dùng đề nghị chuyên gia làm rõ khuyến nghị axit folic mang tính cá nhân hóa.', 'PENDING', 'USER', 'NORMAL'),
        (24, 'EXPERT', NULL, 'E', 6, 'F', 6, 'OTHER', 'Báo cáo yêu cầu xác minh lại thông tin nơi công tác của chuyên gia.', 'PENDING', 'USER', 'NORMAL')
    )
    INSERT INTO public.moderation_cases
        (moderation_case_id, reporter_user_id, assigned_moderator_id, target_type, target_id,
         reason_code, description, status, opened_at, resolved_at, updated_at, report_source,
         priority, claimed_at, ai_feedback_decision, ai_feedback_reason, ai_feedback_by,
         ai_feedback_at, ai_feedback_assessment_id)
    SELECT ('c6000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           CASE reporter_kind WHEN 'M' THEN v_mothers[reporter_idx]
                              WHEN 'F' THEN v_families[reporter_idx]
                              ELSE NULL END,
           CASE WHEN status = 'IN_REVIEW' THEN v_moderator ELSE NULL END,
           target_type,
           CASE
             WHEN target_type = 'QUESTION' THEN ('c3000000-0000-4000-8000-' || lpad(target_seq::text, 12, '0'))::uuid
             WHEN target_type = 'ANSWER' THEN ('c4000000-0000-4000-8000-' || lpad(target_seq::text, 12, '0'))::uuid
             WHEN target_user_kind = 'M' THEN v_mothers[target_user_idx]
             WHEN target_user_kind = 'F' THEN v_families[target_user_idx]
             ELSE v_experts[target_user_idx]
           END,
           reason_code, description, status,
           v_now - make_interval(days => (25 - seq)),
           CASE WHEN status IN ('RESOLVED', 'DISMISSED')
                THEN v_now - make_interval(days => (24 - seq)) ELSE NULL END,
           v_now - make_interval(days => greatest(0, 24 - seq)),
           source, priority,
           CASE WHEN status = 'IN_REVIEW' THEN v_now - make_interval(days => (24 - seq)) ELSE NULL END,
           NULL, NULL, NULL, NULL, NULL
      FROM case_seed
    ON CONFLICT (moderation_case_id) DO NOTHING;

    -- Forty-four durable AI jobs: 20 completed examples, 20 queued/processing items backing
    -- every AI_PENDING question/answer above, and four observable failures.
    WITH job_seed(seq, target_type, target_seq, status) AS (
        SELECT seq, 'QUESTION', seq, 'COMPLETED' FROM generate_series(1, 8) seed(seq)
        UNION ALL
        SELECT 8 + seq,
               CASE WHEN seq <= 8 THEN 'QUESTION' ELSE 'ANSWER' END,
               CASE WHEN seq <= 8 THEN 52 + seq ELSE 60 + seq END,
               'COMPLETED'
          FROM generate_series(1, 12) seed(seq)
        UNION ALL
        SELECT 20 + seq, 'QUESTION',
               CASE WHEN seq <= 8 THEN 44 + seq ELSE 52 + seq END,
               CASE WHEN seq <= 6 THEN 'QUEUED' ELSE 'PROCESSING' END
          FROM generate_series(1, 12) seed(seq)
        UNION ALL
        SELECT 32 + seq, 'ANSWER', 76 + seq,
               CASE WHEN seq <= 4 THEN 'QUEUED' ELSE 'PROCESSING' END
          FROM generate_series(1, 8) seed(seq)
        UNION ALL
        SELECT 40 + seq, 'QUESTION', 24 + seq, 'FAILED'
          FROM generate_series(1, 4) seed(seq)
    )
    INSERT INTO public.ai_content_scan_jobs
        (job_id, target_type, target_id, content_hash, status, attempt_count, next_attempt_at,
         locked_by, locked_at, last_error_code, force_rescan, created_at, updated_at, completed_at)
    SELECT ('c7000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           target_type,
           content.content_id,
           encode(sha256(convert_to(
               CASE
                 WHEN target_type = 'QUESTION' AND content.title IS NOT NULL AND btrim(content.title) <> ''
                   THEN btrim(content.title || E'\n' || content.body)
                 ELSE btrim(content.body)
               END,
               'UTF8')), 'hex'),
           status,
           CASE status WHEN 'QUEUED' THEN 0 WHEN 'PROCESSING' THEN 1 ELSE 1 END,
           CASE WHEN status IN ('QUEUED', 'PROCESSING') THEN v_now - interval '1 minute' ELSE v_now END,
           CASE WHEN status = 'PROCESSING' THEN 'carebridge-ai-worker-demo' ELSE NULL END,
           CASE WHEN status = 'PROCESSING' THEN v_now - interval '2 minutes' ELSE NULL END,
           CASE WHEN status = 'FAILED' THEN 'AI_PROVIDER_TIMEOUT' ELSE NULL END,
           false,
           v_now - make_interval(hours => greatest(1, 45 - seq)),
           v_now - make_interval(hours => greatest(0, 44 - seq)),
           CASE WHEN status IN ('COMPLETED', 'FAILED')
                THEN v_now - make_interval(hours => greatest(0, 44 - seq)) ELSE NULL END
      FROM job_seed
      JOIN public.community_content content
        ON content.content_id = (
            CASE target_type
              WHEN 'QUESTION' THEN 'c3000000-0000-4000-8000-'
              ELSE 'c4000000-0000-4000-8000-'
            END || lpad(target_seq::text, 12, '0'))::uuid
    ON CONFLICT (job_id) DO NOTHING;

    -- Twenty completed assessments (safe + violation/uncertain) and four failed assessments.
    -- The non-safe rows snapshot canonical policies exactly as AiVerdictParser persists them.
    WITH policy_set AS (
        SELECT target.target_type,
               encode(sha256(convert_to(string_agg(
                   p.policy_code || ':' || p.version::text || ':' || p.severity || ':'
                   || p.violation_category || ':'
                   || CASE
                        WHEN p.confidence_threshold = trunc(p.confidence_threshold)
                        THEN trunc(p.confidence_threshold)::text
                        ELSE trim(trailing '0' FROM p.confidence_threshold::text)
                      END || ':'
                   || p.applicable_target_types,
                   '|' ORDER BY p.policy_code), 'UTF8')), 'hex') AS policy_set_hash
          FROM (VALUES ('QUESTION'), ('ANSWER')) AS target(target_type)
          JOIN public.ai_moderation_policies p
            ON p.active
           AND target.target_type = ANY (regexp_split_to_array(p.applicable_target_types, '\s*,\s*'))
         GROUP BY target.target_type
    ),
    assessment_seed(
        seq, job_seq, case_seq, classification, severity, confidence, action, explanation,
        policy_code, evidence
    ) AS (
        SELECT seq, seq, NULL::integer, 'SAFE', 'LOW', 0.960::numeric, 'NO_ACTION',
               'Nội dung là câu hỏi sức khỏe mang tính chia sẻ, có ngữ cảnh rõ và không thay thế chẩn đoán.'
               , NULL::text, NULL::text
          FROM generate_series(1, 8) seed(seq)
        UNION ALL
        SELECT 8 + seq, 8 + seq, seq,
               CASE WHEN seq <= 10 THEN 'VIOLATION' ELSE 'UNCERTAIN' END,
               CASE WHEN seq IN (1, 3, 6, 8, 10) THEN 'CRITICAL'
                    WHEN seq IN (2, 4, 7, 11, 12) THEN 'HIGH' ELSE 'MEDIUM' END,
               CASE WHEN seq <= 10 THEN (0.985 - seq * 0.006)::numeric ELSE 0.710::numeric END,
               CASE WHEN seq IN (1, 3, 6, 8, 10) THEN 'ESCALATE'
                    WHEN seq <= 10 THEN 'PRIORITY_REVIEW' ELSE 'REVIEW' END,
               CASE seq
                 WHEN 1 THEN 'Phát hiện lời khuyên tăng liều vi chất không có đánh giá chuyên môn.'
                 WHEN 2 THEN 'Có dấu hiệu mạo danh chuyên gia và chẩn đoán đổi lấy chuyển khoản.'
                 WHEN 3 THEN 'Khuyến khích bỏ theo dõi bệnh lý thai kỳ, nguy cơ gây trì hoãn chăm sóc.'
                 WHEN 4 THEN 'Ngôn ngữ hạ thấp và công kích thành viên thuộc nhóm dễ tổn thương.'
                 WHEN 5 THEN 'Quảng cáo trá hình sản phẩm y tế không rõ nguồn gốc.'
                 WHEN 6 THEN 'Thông tin có thể khiến gia đình trì hoãn khám vàng da sơ sinh.'
                 WHEN 7 THEN 'Phát hiện lời lẽ chế giễu người đang tìm hỗ trợ tâm lý.'
                 WHEN 8 THEN 'Dấu hiệu lừa đảo và yêu cầu cung cấp dữ liệu nhận dạng của trẻ.'
                 WHEN 9 THEN 'Bình luận kéo người dùng ra ngoài nền tảng để bán thuốc.'
                 WHEN 10 THEN 'Khuyến nghị tăng liều vi chất tuyệt đối, không có cảnh báo an toàn.'
                 WHEN 11 THEN 'Nội dung tự nhận khả năng đọc ảnh siêu âm; cần người kiểm duyệt xác minh ngữ cảnh.'
                 ELSE 'Bình luận có ngôn ngữ thiếu tôn trọng; cần người kiểm duyệt đánh giá bối cảnh.'
               END,
               CASE seq
                 WHEN 1 THEN 'DANGEROUS_MEDICAL_ADVICE'
                 WHEN 2 THEN 'EXPERT_IMPERSONATION'
                 WHEN 3 THEN 'DANGEROUS_MEDICAL_ADVICE'
                 WHEN 4 THEN 'HARASSMENT_BULLYING'
                 WHEN 5 THEN 'SPAM_ADVERTISING'
                 WHEN 6 THEN 'DANGEROUS_MEDICAL_ADVICE'
                 WHEN 7 THEN 'HARASSMENT_BULLYING'
                 WHEN 8 THEN 'SCAM_FRAUD'
                 WHEN 9 THEN 'SPAM_ADVERTISING'
                 WHEN 10 THEN 'DANGEROUS_MEDICAL_ADVICE'
                 WHEN 11 THEN 'EXPERT_IMPERSONATION'
                 ELSE 'HARASSMENT_BULLYING'
               END,
               CASE seq
                 WHEN 1 THEN 'tăng gấp nhiều lần liều vitamin'
                 WHEN 2 THEN 'chẩn đoán từ ảnh siêu âm'
                 WHEN 3 THEN 'tự ngừng theo dõi và bỏ lịch tái khám'
                 WHEN 4 THEN 'dùng lời lẽ miệt thị, đổ lỗi'
                 WHEN 5 THEN 'quảng cáo sản phẩm không rõ nguồn gốc'
                 WHEN 6 THEN 'bỏ lịch tái khám vàng da'
                 WHEN 7 THEN 'công kích người đang tìm kiếm hỗ trợ'
                 WHEN 8 THEN 'yêu cầu chuyển khoản giữ chỗ'
                 WHEN 9 THEN 'mua gói thuốc của tôi'
                 WHEN 10 THEN 'Tăng liều càng cao càng tốt'
                 WHEN 11 THEN 'chẩn đoán chắc chắn nếu bạn chuyển khoản trước'
                 ELSE 'quá yếu đuối, đừng đăng bài làm phiền'
               END
          FROM generate_series(1, 12) seed(seq)
        UNION ALL
        SELECT 20 + seq, 40 + seq, NULL::integer, NULL, NULL, NULL, NULL,
               'Không nhận được kết quả từ nhà cung cấp AI sau số lần thử cho phép.'
               , NULL::text, NULL::text
          FROM generate_series(1, 4) seed(seq)
    )
    INSERT INTO public.ai_content_assessments
        (assessment_id, job_id, target_type, target_id, content_hash, policy_set_hash,
         provider, model, status, classification, overall_severity, confidence,
         recommended_action, explanation, error_code, attempt_count, latency_ms,
         prompt_tokens, output_tokens, matches_jsonb, moderation_case_id, created_at, completed_at)
    SELECT ('c8000000-0000-4000-8000-' || lpad(a.seq::text, 12, '0'))::uuid,
           j.job_id, j.target_type, j.target_id, j.content_hash, ps.policy_set_hash,
           'GEMINI', 'gemini-2.5-flash',
           CASE WHEN a.seq <= 20 THEN 'COMPLETED' ELSE 'FAILED' END,
           a.classification, COALESCE(p.severity, a.severity), a.confidence,
           CASE
             WHEN a.classification IS NULL THEN NULL
             WHEN a.classification = 'SAFE' THEN 'NO_ACTION'
             WHEN a.classification = 'UNCERTAIN' THEN 'REVIEW'
             WHEN p.severity = 'CRITICAL' THEN 'ESCALATE'
             WHEN p.severity = 'HIGH' THEN 'PRIORITY_REVIEW'
             ELSE 'REVIEW'
           END,
           a.explanation,
           CASE WHEN a.seq > 20 THEN 'AI_PROVIDER_TIMEOUT' ELSE NULL END,
           1, CASE WHEN a.seq <= 20 THEN 420 + a.seq * 17 ELSE 30000 END,
           CASE WHEN a.seq <= 20 THEN 640 + a.seq * 3 ELSE NULL END,
           CASE WHEN a.seq <= 20 THEN 82 + a.seq ELSE NULL END,
           CASE WHEN a.classification IN ('VIOLATION', 'UNCERTAIN') THEN
              jsonb_build_array(jsonb_build_object(
                  'policyId', p.policy_id,
                  'policyCode', p.policy_code,
                  'policyVersion', p.version,
                  'category', p.violation_category,
                  'severity', p.severity,
                  'confidence', a.confidence,
                  'evidence', jsonb_build_array(a.evidence),
                  'explanation', a.explanation))
             ELSE '[]'::jsonb END,
           CASE WHEN a.case_seq IS NOT NULL
                THEN ('c6000000-0000-4000-8000-' || lpad(a.case_seq::text, 12, '0'))::uuid
                ELSE NULL END,
           j.created_at + interval '1 minute',
           CASE WHEN a.seq <= 20 THEN j.completed_at ELSE j.updated_at END
      FROM assessment_seed a
      JOIN public.ai_content_scan_jobs j
        ON j.job_id = ('c7000000-0000-4000-8000-' || lpad(a.job_seq::text, 12, '0'))::uuid
      JOIN policy_set ps
        ON ps.target_type = j.target_type
       AND ps.policy_set_hash IS NOT NULL
      LEFT JOIN public.ai_moderation_policies p
        ON p.policy_code = a.policy_code
     WHERE a.policy_code IS NULL OR p.policy_id IS NOT NULL
    ON CONFLICT (assessment_id) DO NOTHING;

    -- Align automated cases with the authoritative policy category/priority matrix.
    UPDATE public.moderation_cases c
       SET reason_code = p.report_category,
           priority = CASE p.severity
               WHEN 'CRITICAL' THEN 'URGENT'
               WHEN 'HIGH' THEN 'HIGH'
               ELSE 'NORMAL'
           END,
           updated_at = clock_timestamp()
      FROM public.ai_content_assessments a
      CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
      JOIN public.ai_moderation_policies p
        ON p.policy_id = (match.value ->> 'policyId')::uuid
     WHERE c.moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%'
       AND c.report_source = 'AUTOMATED'
       AND a.moderation_case_id = c.moderation_case_id;

    -- Link moderator feedback only after assessments exist to respect the intentional circular FK.
    UPDATE public.moderation_cases c
       SET ai_feedback_decision = CASE WHEN seed.seq IN (8, 12) THEN 'DISAGREE' ELSE 'AGREE' END,
           ai_feedback_reason = CASE WHEN seed.seq IN (8, 12)
               THEN 'Kiểm duyệt viên đồng ý cần xử lý nhưng điều chỉnh lại nhóm chính sách theo ngữ cảnh.'
               ELSE 'Kiểm duyệt viên xác nhận cảnh báo AI phù hợp với nội dung và mức độ rủi ro.' END,
           ai_feedback_by = v_moderator,
           ai_feedback_at = v_now - make_interval(days => greatest(0, 13 - seed.seq)),
           ai_feedback_assessment_id = ('c8000000-0000-4000-8000-' || lpad((8 + seed.seq)::text, 12, '0'))::uuid
      FROM generate_series(7, 12) AS seed(seq)
     WHERE c.moderation_case_id = ('c6000000-0000-4000-8000-' || lpad(seed.seq::text, 12, '0'))::uuid
       AND c.ai_feedback_assessment_id IS NULL;

    -- Twenty account-level enforcement history items populate /moderator/violations without
    -- suspending or restricting the canonical login accounts (all timed actions are expired).
    WITH account_action_seed(seq, target_kind, target_idx, action_type, reason, expires_days_ago) AS (VALUES
        (1, 'M', 1, 'WARN', 'Nhắc nhở không đăng lặp cùng một bình luận ở nhiều chủ đề.', NULL),
        (2, 'F', 1, 'WARN', 'Nhắc nhở giữ ngôn ngữ tôn trọng khi trao đổi kinh nghiệm chăm bé.', NULL),
        (3, 'M', 2, 'RESTRICT', 'Tạm hạn chế đăng bài do chia sẻ liên kết bán hàng nhiều lần.', 40),
        (4, 'F', 2, 'SUSPEND', 'Tạm đình chỉ sau nhiều lần gửi tin nhắn quấy rối thành viên.', 35),
        (5, 'M', 3, 'WARN', 'Nhắc nhở không công khai số điện thoại và hồ sơ y tế của người khác.', NULL),
        (6, 'F', 3, 'ESCALATE', 'Chuyển quản trị viên xem xét chuỗi hành vi mạo danh chuyên gia.', NULL),
        (7, 'M', 4, 'RESTRICT', 'Tạm hạn chế đăng nội dung y tế thiếu nguồn và khẳng định tuyệt đối.', 30),
        (8, 'F', 4, 'WARN', 'Nhắc nhở không kéo thành viên sang nhóm bán hàng ngoài nền tảng.', NULL),
        (9, 'M', 5, 'SUSPEND', 'Tạm đình chỉ do tiếp tục công kích cá nhân sau cảnh báo.', 28),
        (10, 'F', 5, 'WARN', 'Nhắc nhở phân biệt trải nghiệm cá nhân với tư vấn chuyên môn.', NULL),
        (11, 'M', 6, 'RESTRICT', 'Tạm hạn chế bình luận do spam nội dung không liên quan.', 24),
        (12, 'F', 6, 'ESCALATE', 'Đề nghị quản trị viên rà soát dấu hiệu lừa đảo qua chuyển khoản.', NULL),
        (13, 'M', 1, 'WARN', 'Cảnh báo lần hai về việc đăng ảnh có dữ liệu cá nhân chưa che.', NULL),
        (14, 'F', 1, 'RESTRICT', 'Tạm hạn chế sau khi quảng bá sản phẩm trong câu trả lời sức khỏe.', 18),
        (15, 'M', 2, 'WARN', 'Nhắc nhở kiểm tra nguồn trước khi chia sẻ thông tin tiêm chủng.', NULL),
        (16, 'F', 2, 'ESCALATE', 'Chuyển quản trị viên đánh giá lịch sử nhiều báo cáo liên quan.', NULL),
        (17, 'M', 3, 'SUSPEND', 'Tạm đình chỉ do đăng lại nội dung đã bị ẩn.', 12),
        (18, 'F', 3, 'WARN', 'Nhắc nhở không dùng lời lẽ gây áp lực với mẹ sau sinh.', NULL),
        (19, 'M', 4, 'RESTRICT', 'Tạm hạn chế do gửi liên kết không rõ nguồn trong nhiều chủ đề.', 8),
        (20, 'F', 4, 'WARN', 'Nhắc nhở tôn trọng quyết định khám và điều trị của thành viên.', NULL)
    )
    INSERT INTO public.audit_events
        (audit_event_id, actor_user_id, event_category, subject_user_id, subject_reference_id,
         resource_type, resource_id, purpose, decision, occurred_at, created_at, note_text,
         event_origin, payload, severity, status)
    SELECT ('c9000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           v_moderator, 'MODERATION_' || action_type,
           CASE target_kind WHEN 'M' THEN v_mothers[target_idx] ELSE v_families[target_idx] END,
           NULL, 'ACCOUNT',
           CASE target_kind WHEN 'M' THEN v_mothers[target_idx] ELSE v_families[target_idx] END,
           'COMMUNITY_SAFETY', action_type,
           v_now - make_interval(days => (45 - seq)),
           v_now - make_interval(days => (45 - seq)), reason, 'AUDIT_LOG',
           jsonb_strip_nulls(jsonb_build_object(
               'reason', reason,
               'expiresAt', CASE WHEN expires_days_ago IS NOT NULL
                   THEN to_char(
                       (v_now - make_interval(days => expires_days_ago)) AT TIME ZONE 'UTC',
                       'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
                   ELSE NULL END)),
           CASE WHEN action_type IN ('SUSPEND', 'ESCALATE') THEN 'HIGH' ELSE 'MEDIUM' END,
           'CLOSED'
      FROM account_action_seed
    ON CONFLICT (audit_event_id) DO NOTHING;

    -- Thirty content actions include 20 explicit approvals plus hide/lock decisions, proving
    -- both moderator-approved and enforcement workflows while keeping every action traceable.
    WITH content_action_seed(seq, target_type, target_seq, action_type, reason) AS (VALUES
        (101, 'QUESTION', 1, 'APPROVE', 'Câu hỏi rõ ràng, phù hợp chủ đề và có ngữ cảnh sức khỏe an toàn.'),
        (102, 'QUESTION', 2, 'APPROVE', 'Nội dung chuẩn bị thai kỳ, không đưa ra liều dùng tuyệt đối.'),
        (103, 'QUESTION', 3, 'APPROVE', 'Câu hỏi trải nghiệm cá nhân, có kế hoạch khám cụ thể.'),
        (104, 'QUESTION', 4, 'APPROVE', 'Câu hỏi dinh dưỡng thực tế, không thay thế tư vấn y tế.'),
        (105, 'QUESTION', 5, 'APPROVE', 'Nội dung theo dõi thai máy phù hợp để cộng đồng trao đổi.'),
        (106, 'QUESTION', 6, 'APPROVE', 'Người đồng hành hỏi cách hỗ trợ và nêu rõ khi cần báo bác sĩ.'),
        (107, 'QUESTION', 7, 'APPROVE', 'Câu hỏi tập trung xây dựng thói quen theo dõi không gây áp lực.'),
        (108, 'QUESTION', 8, 'APPROVE', 'Câu hỏi mô tả triệu chứng và thông tin khám gần nhất đầy đủ.'),
        (109, 'QUESTION', 9, 'APPROVE', 'Nội dung chuẩn bị lịch khám có giá trị thực hành cho cộng đồng.'),
        (110, 'QUESTION', 10, 'APPROVE', 'Câu hỏi khuyến khích trao đổi trực tiếp với bác sĩ.'),
        (121, 'QUESTION', 11, 'APPROVE', 'Gợi ý bữa sáng thực tế, không khẳng định một khẩu phần phù hợp cho mọi người.'),
        (122, 'QUESTION', 12, 'APPROVE', 'Câu hỏi tập trung vào an toàn thực phẩm và vệ sinh bếp.'),
        (123, 'QUESTION', 13, 'APPROVE', 'Nội dung vận động nêu rõ đã được bác sĩ xác nhận và hỏi cách bắt đầu an toàn.'),
        (124, 'QUESTION', 14, 'APPROVE', 'Câu hỏi về tư thế ngủ phù hợp trao đổi kinh nghiệm cộng đồng.'),
        (125, 'QUESTION', 15, 'APPROVE', 'Câu hỏi khẩn có kế hoạch liên hệ cơ sở y tế và cần hướng dẫn mô tả triệu chứng.'),
        (126, 'QUESTION', 16, 'APPROVE', 'Người đồng hành hỏi cách ghi huyết áp đúng để cung cấp cho bác sĩ.'),
        (127, 'QUESTION', 17, 'APPROVE', 'Nội dung tuân thủ kế hoạch theo dõi đường huyết của bác sĩ.'),
        (128, 'QUESTION', 18, 'APPROVE', 'Câu hỏi giúp gia đình ghi nhớ dấu hiệu cần đi viện ngay.'),
        (129, 'QUESTION', 19, 'APPROVE', 'Nội dung chuẩn bị chuyển dạ có kế hoạch gọi nơi dự sinh khi bất thường.'),
        (130, 'QUESTION', 20, 'APPROVE', 'Danh sách đồ đi sinh mang tính tổ chức và ưu tiên hướng dẫn bệnh viện.'),
        (111, 'QUESTION', 53, 'HIDE', 'Ẩn lời khuyên tự tăng liều vi chất không an toàn.'),
        (112, 'QUESTION', 54, 'HIDE', 'Ẩn nội dung mời chẩn đoán trả phí chưa xác minh danh tính.'),
        (113, 'QUESTION', 55, 'LOCK', 'Khóa nội dung khuyến khích bỏ theo dõi bệnh lý thai kỳ.'),
        (114, 'QUESTION', 56, 'HIDE', 'Ẩn nội dung quấy rối và đổ lỗi cho mẹ bầu.'),
        (115, 'QUESTION', 57, 'LOCK', 'Khóa quảng cáo sản phẩm y tế không rõ nguồn gốc.'),
        (116, 'QUESTION', 58, 'LOCK', 'Khóa thông tin nguy hiểm có thể làm trì hoãn khám trẻ sơ sinh.'),
        (117, 'ANSWER', 69, 'HIDE', 'Ẩn trả lời spam bán thuốc và thu thập số điện thoại.'),
        (118, 'ANSWER', 70, 'HIDE', 'Ẩn trả lời khuyến nghị liều dùng không an toàn.'),
        (119, 'ANSWER', 71, 'HIDE', 'Ẩn hành vi mạo danh chẩn đoán qua ảnh siêu âm.'),
        (120, 'ANSWER', 72, 'HIDE', 'Ẩn bình luận công kích thành viên đang bị nghén.')
    )
    INSERT INTO public.audit_events
        (audit_event_id, actor_user_id, event_category, subject_reference_id,
         resource_type, resource_id, purpose, decision, occurred_at, created_at,
         note_text, event_origin, payload, severity, status)
    SELECT ('c9000000-0000-4000-8000-' || lpad(seq::text, 12, '0'))::uuid,
           v_moderator, 'MODERATION_' || action_type, NULL,
           target_type,
           (CASE target_type WHEN 'QUESTION' THEN 'c3000000-0000-4000-8000-'
                             ELSE 'c4000000-0000-4000-8000-' END || lpad(target_seq::text, 12, '0'))::uuid,
           'COMMUNITY_CONTENT_REVIEW', action_type,
           v_now - make_interval(days => greatest(1, 130 - seq)),
           v_now - make_interval(days => greatest(1, 130 - seq)),
           reason, 'AUDIT_LOG', jsonb_build_object('reason', reason),
           CASE WHEN action_type = 'LOCK' THEN 'HIGH' ELSE 'MEDIUM' END, 'CLOSED'
      FROM content_action_seed
    ON CONFLICT (audit_event_id) DO NOTHING;

    -- Fail the migration if a future schema or seed edit silently drops required demo coverage.
    IF (SELECT count(*) FROM public.community_topics WHERE id::text LIKE 'c1000000-0000-4000-8000-%') <> 20
       OR (SELECT count(*) FROM public.community_topics WHERE id::text LIKE 'c2000000-0000-4000-8000-%') <> 40
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c3000000-0000-4000-8000-%') <> 64
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c4000000-0000-4000-8000-%') <> 84
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c3000000-0000-4000-8000-%' AND moderation_status = 'APPROVED') <> 24
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c3000000-0000-4000-8000-%' AND moderation_status = 'PENDING') <> 20
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c4000000-0000-4000-8000-%' AND moderation_status = 'APPROVED') <> 48
       OR (SELECT count(*) FROM public.community_content WHERE content_id::text LIKE 'c4000000-0000-4000-8000-%' AND moderation_status = 'PENDING') <> 20
       OR (SELECT count(*) FROM public.community_interactions WHERE interaction_id::text LIKE 'c5000000-0000-4000-8000-%' AND interaction_type = 'BOOKMARK') <> 24
       OR (SELECT count(*) FROM public.moderation_cases WHERE moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%') <> 24
       OR (SELECT count(*) FROM public.ai_content_scan_jobs WHERE job_id::text LIKE 'c7000000-0000-4000-8000-%') <> 44
       OR (SELECT count(*) FROM public.ai_content_assessments WHERE assessment_id::text LIKE 'c8000000-0000-4000-8000-%') <> 24
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_scan_jobs j
              JOIN public.community_content content
                ON content.content_id = j.target_id
             WHERE j.job_id::text LIKE 'c7000000-0000-4000-8000-%'
               AND j.content_hash <> encode(sha256(convert_to(
                   CASE
                     WHEN j.target_type = 'QUESTION' AND content.title IS NOT NULL AND btrim(content.title) <> ''
                       THEN btrim(content.title || E'\n' || content.body)
                     ELSE btrim(content.body)
                   END,
                   'UTF8')), 'hex')
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
              JOIN public.ai_content_scan_jobs j ON j.job_id = a.job_id
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND a.content_hash <> j.content_hash
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              LEFT JOIN public.ai_moderation_policies p
                ON p.policy_id = (match.value ->> 'policyId')::uuid
               AND p.policy_code = match.value ->> 'policyCode'
               AND p.version = (match.value ->> 'policyVersion')::integer
               AND p.violation_category = match.value ->> 'category'
               AND p.severity = match.value ->> 'severity'
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND p.policy_id IS NULL
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
              JOIN public.community_content content
                ON content.content_id = a.target_id
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              CROSS JOIN LATERAL jsonb_array_elements_text(match.value -> 'evidence') AS evidence(excerpt)
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND strpos(content.body, evidence.excerpt) = 0
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND a.policy_set_hash <> (
                    SELECT encode(sha256(convert_to(string_agg(
                               p.policy_code || ':' || p.version::text || ':' || p.severity || ':'
                               || p.violation_category || ':'
                               || CASE
                                    WHEN p.confidence_threshold = trunc(p.confidence_threshold)
                                    THEN trunc(p.confidence_threshold)::text
                                    ELSE trim(trailing '0' FROM p.confidence_threshold::text)
                                  END || ':' || p.applicable_target_types,
                               '|' ORDER BY p.policy_code), 'UTF8')), 'hex')
                      FROM public.ai_moderation_policies p
                     WHERE p.active
                       AND a.target_type = ANY (regexp_split_to_array(p.applicable_target_types, '\s*,\s*'))
               )
       )
       OR EXISTS (
            SELECT 1
              FROM public.moderation_cases c
              JOIN public.ai_content_assessments a
                ON a.moderation_case_id = c.moderation_case_id
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              JOIN public.ai_moderation_policies p
                ON p.policy_id = (match.value ->> 'policyId')::uuid
             WHERE c.moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%'
               AND c.report_source = 'AUTOMATED'
               AND (
                    c.reason_code <> p.report_category
                    OR c.priority <> CASE p.severity
                        WHEN 'CRITICAL' THEN 'URGENT'
                        WHEN 'HIGH' THEN 'HIGH'
                        ELSE 'NORMAL'
                      END
               )
       )
       OR (SELECT count(*) FROM public.audit_events WHERE audit_event_id::text LIKE 'c9000000-0000-4000-8000-%' AND resource_type = 'ACCOUNT') <> 20
       OR (SELECT count(*) FROM public.audit_events WHERE audit_event_id::text LIKE 'c9000000-0000-4000-8000-%' AND resource_type IN ('QUESTION', 'ANSWER')) <> 30
    THEN
        RAISE EXCEPTION 'CAREBRIDGE_COMMUNITY_DEMO_SEED_INCOMPLETE';
    END IF;
END
$community_demo$;
