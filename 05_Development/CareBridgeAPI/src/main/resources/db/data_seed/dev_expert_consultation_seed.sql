-- ============================================================================
-- CareBridge Expert Consultation Seed Data
-- Bổ sung ca tư vấn, lịch rảnh (availability), yêu cầu tư vấn (consultation requests),
-- lịch hẹn (bookings), hội thoại & tin nhắn tư vấn cho các tài khoản EXPERT.
-- ============================================================================

DO $carebridge_expert_consultation_seed$
DECLARE
    -- Expert IDs
    v_exp1 uuid; -- BS Đỗ Hải Long (Sản khoa - TP.HCM)
    v_exp2 uuid; -- BS Trần Thị Thu Nga (Sản khoa - TP.HCM)
    v_exp3 uuid; -- BS Trần Văn Hoàng (Nhi khoa - TP.HCM)
    v_exp4 uuid; -- BS Nguyễn Văn Minh (Dinh dưỡng - Hà Nội)
    v_exp5 uuid; -- BS Nguyễn Thị Lan (Tâm lý - Đà Nẵng)
    v_exp6 uuid; -- BS Lê Văn Bình (Nhi khoa - Cần Thơ)
    v_dr_an uuid;
    v_dr_binh uuid;

    -- Mother IDs
    v_m1 uuid; -- Mẹ Bầu 1
    v_m2 uuid; -- Mẹ Bầu 2
    v_m3 uuid; -- Mẹ bầu 3
    v_m4 uuid; -- Mẹ bầu 4
    v_m5 uuid; -- Mẹ bầu 5
    v_m6 uuid; -- Mẹ bầu 6

    -- Base timestamp (current day truncated to hour for clean slots)
    v_today timestamp with time zone;
    v_tomorrow timestamp with time zone;
    v_day3 timestamp with time zone;
    v_day4 timestamp with time zone;
    v_day5 timestamp with time zone;
    v_yesterday timestamp with time zone;

    -- Working variables for IDs
    v_conv_id uuid;
    v_avail_id uuid;
    v_req_id uuid;
    v_book_id uuid;
BEGIN
    -- 1. Resolve User IDs
    SELECT user_id INTO v_exp1 FROM public.users WHERE email = 'expert@carebridge.dev';
    SELECT user_id INTO v_exp2 FROM public.users WHERE email = 'expert2@carebridge.dev';
    SELECT user_id INTO v_exp3 FROM public.users WHERE email = 'expert3@carebridge.dev';
    SELECT user_id INTO v_exp4 FROM public.users WHERE email = 'expert4@carebridge.dev';
    SELECT user_id INTO v_exp5 FROM public.users WHERE email = 'expert5@carebridge.dev';
    SELECT user_id INTO v_exp6 FROM public.users WHERE email = 'expert6@carebridge.dev';
    SELECT user_id INTO v_dr_an FROM public.users WHERE email = 'dr.an@carebridge.vn';
    SELECT user_id INTO v_dr_binh FROM public.users WHERE email = 'dr.binh@carebridge.vn';

    SELECT user_id INTO v_m1 FROM public.users WHERE email = 'mother@carebridge.dev';
    SELECT user_id INTO v_m2 FROM public.users WHERE email = 'mother2@carebridge.dev';
    SELECT user_id INTO v_m3 FROM public.users WHERE email = 'mother3@carebridge.dev';
    SELECT user_id INTO v_m4 FROM public.users WHERE email = 'mother4@carebridge.dev';
    SELECT user_id INTO v_m5 FROM public.users WHERE email = 'mother5@carebridge.dev';
    SELECT user_id INTO v_m6 FROM public.users WHERE email = 'mother6@carebridge.dev';

    -- Fallbacks if mothers not found
    IF v_m1 IS NULL THEN
        SELECT user_id INTO v_m1 FROM public.users WHERE role = 'MOTHER' LIMIT 1;
    END IF;
    IF v_m2 IS NULL THEN v_m2 := v_m1; END IF;
    IF v_m3 IS NULL THEN v_m3 := v_m1; END IF;
    IF v_m4 IS NULL THEN v_m4 := v_m2; END IF;
    IF v_m5 IS NULL THEN v_m5 := v_m1; END IF;
    IF v_m6 IS NULL THEN v_m6 := v_m2; END IF;

    -- Time references (midnight today in local time)
    v_today := date_trunc('day', now());
    v_tomorrow := v_today + interval '1 day';
    v_day3 := v_today + interval '2 days';
    v_day4 := v_today + interval '3 days';
    v_day5 := v_today + interval '4 days';
    v_yesterday := v_today - interval '1 day';

    -- ========================================================================
    -- 2. SEED EXPERT AVAILABILITY SLOTS (Ca tư vấn rảnh của Chuyên gia)
    -- ========================================================================
    
    -- Helper macro/block for each expert
    -- EXPERT 1: BS Đỗ Hải Long (Sản khoa - Từ Dũ)
    IF v_exp1 IS NOT NULL THEN
        -- Location share
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000001', v_exp1, v_exp1, 10.76810000, 106.68340000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        -- Slots Today
        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000001001', v_exp1, v_exp1, v_today + interval '8 hours', v_today + interval '9 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001002', v_exp1, v_exp1, v_today + interval '9 hours 30 minutes', v_today + interval '10 hours 30 minutes', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001003', v_exp1, v_exp1, v_today + interval '14 hours', v_today + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001004', v_exp1, v_exp1, v_today + interval '15 hours 30 minutes', v_today + interval '16 hours 30 minutes', 'VOICE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001005', v_exp1, v_exp1, v_today + interval '19 hours', v_today + interval '20 hours', 'VIDEO', 'BOOKED', now(), now()),
        -- Slots Tomorrow
        ('90000000-0000-4000-8000-000000001006', v_exp1, v_exp1, v_tomorrow + interval '8 hours 30 minutes', v_tomorrow + interval '9 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001007', v_exp1, v_exp1, v_tomorrow + interval '10 hours', v_tomorrow + interval '11 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001008', v_exp1, v_exp1, v_tomorrow + interval '14 hours', v_tomorrow + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001009', v_exp1, v_exp1, v_tomorrow + interval '19 hours 30 minutes', v_tomorrow + interval '20 hours 30 minutes', 'VIDEO', 'AVAILABLE', now(), now()),
        -- Slots Day 3 & 4
        ('90000000-0000-4000-8000-000000001010', v_exp1, v_exp1, v_day3 + interval '9 hours', v_day3 + interval '10 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001011', v_exp1, v_exp1, v_day3 + interval '15 hours', v_day3 + interval '16 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001012', v_exp1, v_exp1, v_day4 + interval '8 hours', v_day4 + interval '9 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000001013', v_exp1, v_exp1, v_day4 + interval '14 hours', v_day4 + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- EXPERT 2: BS Trần Thị Thu Nga (Sản khoa - Từ Dũ)
    IF v_exp2 IS NOT NULL THEN
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000002', v_exp2, v_exp2, 10.76810000, 106.68340000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000002001', v_exp2, v_exp2, v_today + interval '8 hours 30 minutes', v_today + interval '9 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000002002', v_exp2, v_exp2, v_today + interval '10 hours', v_today + interval '11 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000002003', v_exp2, v_exp2, v_today + interval '14 hours 30 minutes', v_today + interval '15 hours 30 minutes', 'VOICE', 'BOOKED', now(), now()),
        ('90000000-0000-4000-8000-000000002004', v_exp2, v_exp2, v_tomorrow + interval '9 hours', v_tomorrow + interval '10 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000002005', v_exp2, v_exp2, v_tomorrow + interval '15 hours', v_tomorrow + interval '16 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000002006', v_exp2, v_exp2, v_day3 + interval '10 hours', v_day3 + interval '11 hours', 'ONLINE', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- EXPERT 3: BS Trần Văn Hoàng (Nhi khoa - Nhi Đồng 1)
    IF v_exp3 IS NOT NULL THEN
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000003', v_exp3, v_exp3, 10.77120000, 106.66980000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000003001', v_exp3, v_exp3, v_today + interval '9 hours', v_today + interval '10 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003002', v_exp3, v_exp3, v_today + interval '10 hours 30 minutes', v_today + interval '11 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003003', v_exp3, v_exp3, v_today + interval '15 hours', v_today + interval '16 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003004', v_exp3, v_exp3, v_today + interval '19 hours', v_today + interval '20 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003005', v_exp3, v_exp3, v_tomorrow + interval '8 hours 30 minutes', v_tomorrow + interval '9 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003006', v_exp3, v_exp3, v_tomorrow + interval '14 hours', v_tomorrow + interval '15 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000003007', v_exp3, v_exp3, v_day3 + interval '14 hours', v_day3 + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- EXPERT 4: BS Nguyễn Văn Minh (Dinh dưỡng - Bạch Mai)
    IF v_exp4 IS NOT NULL THEN
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000004', v_exp4, v_exp4, 21.00280000, 105.84120000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000004001', v_exp4, v_exp4, v_today + interval '8 hours', v_today + interval '9 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000004002', v_exp4, v_exp4, v_today + interval '14 hours', v_today + interval '15 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000004003', v_exp4, v_exp4, v_today + interval '19 hours 30 minutes', v_today + interval '20 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000004004', v_exp4, v_exp4, v_tomorrow + interval '9 hours', v_tomorrow + interval '10 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000004005', v_exp4, v_exp4, v_tomorrow + interval '15 hours', v_tomorrow + interval '16 hours', 'VIDEO', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- EXPERT 5: BS Nguyễn Thị Lan (Tâm lý - Đà Nẵng)
    IF v_exp5 IS NOT NULL THEN
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000005', v_exp5, v_exp5, 16.03540000, 108.23210000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000005001', v_exp5, v_exp5, v_today + interval '9 hours', v_today + interval '10 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000005002', v_exp5, v_exp5, v_today + interval '15 hours', v_today + interval '16 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000005003', v_exp5, v_exp5, v_today + interval '20 hours', v_today + interval '21 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000005004', v_exp5, v_exp5, v_tomorrow + interval '14 hours', v_tomorrow + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000005005', v_exp5, v_exp5, v_day3 + interval '19 hours', v_day3 + interval '20 hours', 'VIDEO', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- EXPERT 6: BS Lê Văn Bình (Nhi khoa - Cần Thơ)
    IF v_exp6 IS NOT NULL THEN
        INSERT INTO public.expert_location_shares (location_share_id, user_id, professional_profile_id, latitude, longitude, accuracy_meters, availability_status, created_at, updated_at)
        VALUES ('95000000-0000-4000-8000-000000000006', v_exp6, v_exp6, 10.03450000, 105.78900000, 10.0, 'AVAILABLE', now(), now())
        ON CONFLICT (location_share_id) DO NOTHING;

        INSERT INTO public.expert_availability (availability_id, user_id, professional_profile_id, start_at, end_at, channel_type, status, created_at, updated_at) VALUES
        ('90000000-0000-4000-8000-000000006001', v_exp6, v_exp6, v_today + interval '8 hours 30 minutes', v_today + interval '9 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000006002', v_exp6, v_exp6, v_today + interval '10 hours', v_today + interval '11 hours', 'VIDEO', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000006003', v_exp6, v_exp6, v_today + interval '14 hours', v_today + interval '15 hours', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000006004', v_exp6, v_exp6, v_tomorrow + interval '8 hours 30 minutes', v_tomorrow + interval '9 hours 30 minutes', 'ONLINE', 'AVAILABLE', now(), now()),
        ('90000000-0000-4000-8000-000000006005', v_exp6, v_exp6, v_tomorrow + interval '15 hours', v_tomorrow + interval '16 hours', 'VIDEO', 'AVAILABLE', now(), now())
        ON CONFLICT (availability_id) DO NOTHING;
    END IF;

    -- ========================================================================
    -- 3. SEED DIRECT CONVERSATIONS (Hội thoại tư vấn giữa Mother và Expert)
    -- ========================================================================
    IF v_exp1 IS NOT NULL AND v_m1 IS NOT NULL THEN
        INSERT INTO public.direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at)
        VALUES ('93000000-0000-4000-8000-000000000001', v_m1, v_exp1, 'ACTIVE', now() - interval '2 days', now() - interval '3 hours')
        ON CONFLICT (conversation_id) DO NOTHING;

        -- Messages
        INSERT INTO public.direct_messages (message_id, conversation_id, sender_user_id, client_message_id, message_body, message_type, created_at) VALUES
        ('96000000-0000-4000-8000-000000000001', '93000000-0000-4000-8000-000000000001', v_m1, '96100000-0000-4000-8000-000000000001', 'Chào bác sĩ Long, em vừa có kết quả siêu âm tuần 22, em gửi bác sĩ xem giúp em với ạ.', 'TEXT', now() - interval '2 days'),
        ('96000000-0000-4000-8000-000000000002', '93000000-0000-4000-8000-000000000001', v_exp1, '96100000-0000-4000-8000-000000000002', 'Chào bạn Mai, bạn gửi ảnh chụp rõ nét phiếu siêu âm và các chỉ số hình thái thai nhi qua đây nhé.', 'TEXT', now() - interval '2 days' + interval '15 minutes'),
        ('96000000-0000-4000-8000-000000000003', '93000000-0000-4000-8000-000000000001', v_exp1, '96100000-0000-4000-8000-000000000003', 'Kết quả siêu âm 4D của em rất tốt: độ dài xương đùi, chu vi đầu, và tim thai 142 lần/phút đều hoàn toàn bình thường. Mẹ yên tâm tiếp tục uống bổ sung Sắt, Canxi và theo dõi cử động thai đều đặn nhé.', 'TEXT', now() - interval '1 day' + interval '2 hours')
        ON CONFLICT (message_id) DO NOTHING;
    END IF;

    IF v_exp3 IS NOT NULL AND v_m2 IS NOT NULL THEN
        INSERT INTO public.direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at)
        VALUES ('93000000-0000-4000-8000-000000000002', v_m2, v_exp3, 'ACTIVE', now() - interval '1 day', now() - interval '1 hour')
        ON CONFLICT (conversation_id) DO NOTHING;

        INSERT INTO public.direct_messages (message_id, conversation_id, sender_user_id, client_message_id, message_body, message_type, created_at) VALUES
        ('96000000-0000-4000-8000-000000000004', '93000000-0000-4000-8000-000000000002', v_m2, '96100000-0000-4000-8000-000000000004', 'Bác sĩ Hoàng cho em hỏi bé nhà em 3 tháng tuổi sau tiêm 6in1 bị sốt 38.2 độ thì chăm sóc thế nào ạ?', 'TEXT', now() - interval '1 day'),
        ('96000000-0000-4000-8000-000000000005', '93000000-0000-4000-8000-000000000002', v_exp3, '96100000-0000-4000-8000-000000000005', 'Chào mẹ, sốt nhẹ 38.2 sau tiêm là phản ứng miễn dịch bình thường. Mẹ lau mát bằng nước ấm ở nách, bẹn, cho bé bú nhiều cữ hơn và theo dõi nhiệt độ mỗi 2-3 tiếng nhé. Nếu sốt trên 38.5 độ hãy cho uống hạ sốt Paracetamol theo cân nặng.', 'TEXT', now() - interval '1 day' + interval '20 minutes')
        ON CONFLICT (message_id) DO NOTHING;
    END IF;

    IF v_exp4 IS NOT NULL AND v_m3 IS NOT NULL THEN
        INSERT INTO public.direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at)
        VALUES ('93000000-0000-4000-8000-000000000003', v_m3, v_exp4, 'ACTIVE', now() - interval '3 days', now() - interval '5 hours')
        ON CONFLICT (conversation_id) DO NOTHING;
    END IF;

    IF v_exp5 IS NOT NULL AND v_m4 IS NOT NULL THEN
        INSERT INTO public.direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at)
        VALUES ('93000000-0000-4000-8000-000000000004', v_m4, v_exp5, 'ACTIVE', now() - interval '2 days', now() - interval '4 hours')
        ON CONFLICT (conversation_id) DO NOTHING;
    END IF;

    -- ========================================================================
    -- 4. SEED CONSULTATION REQUESTS (Yêu cầu tư vấn gửi tới Chuyên gia)
    -- ========================================================================

    -- Request 1: PENDING -> BS Đỗ Hải Long (expert@carebridge.dev)
    -- Giúp màn hình Home / Tab Requests của Expert hiển thị ngay ca đang chờ duyệt
    IF v_exp1 IS NOT NULL AND v_m1 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000001001', v_m1, v_exp1, '94100000-0000-4000-8000-000000001001',
            'Tư vấn kết quả siêu âm hình thái 22 tuần & cử động thai',
            'Em bầu tuần 22, gần đây cảm thấy bé đạp nhiều vào ban đêm và thỉnh thoảng gò nhẹ bụng dưới. Nhờ Bác sĩ xem giúp kết quả siêu âm 4D và tư vấn cách theo dõi cơn gò ạ.',
            v_tomorrow + interval '9 hours', v_tomorrow + interval '10 hours',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '3 days', now() - interval '2 hours', now() - interval '2 hours'
        ) ON CONFLICT (id) DO NOTHING;

        -- Request 2: PENDING -> BS Đỗ Hải Long từ mẹ 2
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000001002', v_m2, v_exp1, '94100000-0000-4000-8000-000000001002',
            'Tư vấn chế độ dinh dưỡng và xét nghiệm tiểu đường thai kỳ (OGTT)',
            'Em chuẩn bị làm nghiệm pháp dung nạp đường huyết ở tuần 24. Nhờ Bác sĩ hướng dẫn cách chuẩn bị trước khi xét nghiệm và chế độ ăn hạn chế đường huyết.',
            v_day3 + interval '14 hours', v_day3 + interval '15 hours',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '4 days', now() - interval '5 hours', now() - interval '5 hours'
        ) ON CONFLICT (id) DO NOTHING;

        -- Request 3: ACCEPTED -> BS Đỗ Hải Long (Đã duyệt)
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000001003', v_m1, v_exp1, '94100000-0000-4000-8000-000000001003',
            'Tư vấn dấu hiệu chuyển dạ sớm và chuẩn bị sinh',
            'Nhờ Bác sĩ hướng dẫn các dấu hiệu cần nhập viện cấp cứu và chuẩn bị hồ sơ sinh tại Bệnh viện Từ Dũ.',
            v_today + interval '19 hours', v_today + interval '20 hours',
            'ACCEPTED', NULL, '93000000-0000-4000-8000-000000000001', now() - interval '1 day', v_exp1,
            now() + interval '5 days', now() - interval '1 day', now() - interval '1 day'
        ) ON CONFLICT (id) DO NOTHING;

        -- Request 4: REJECTED -> BS Đỗ Hải Long (Từ chối kèm lý do)
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000001004', v_m3, v_exp1, '94100000-0000-4000-8000-000000001004',
            'Khám thai trực tiếp tại phòng khám',
            'Mẹ muốn hẹn khám ngoài giờ vào lúc 12h trưa.',
            v_today + interval '12 hours', v_today + interval '13 hours',
            'REJECTED', 'Bác sĩ có ca mổ cấp cứu đột xuất tại bệnh viện trong khung giờ này. Mẹ vui lòng chọn khung giờ buổi tối sau 18h nhé.',
            NULL, now() - interval '1 day', v_exp1,
            now() + interval '2 days', now() - interval '2 days', now() - interval '1 day'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- Requests for EXPERT 2: BS Trần Thị Thu Nga (expert2@carebridge.dev)
    IF v_exp2 IS NOT NULL AND v_m2 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000002001', v_m2, v_exp2, '94100000-0000-4000-8000-000000002001',
            'Tư vấn triệu chứng phù chân và huyết áp tuần 32',
            'Em bầu tuần 32, 2 hôm nay chân phù to và đo huyết áp tại nhà là 135/85 mmHg. Nhờ Bác sĩ tư vấn cách theo dõi tiền sản giật.',
            v_today + interval '14 hours 30 minutes', v_today + interval '15 hours 30 minutes',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '3 days', now() - interval '3 hours', now() - interval '3 hours'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- Requests for EXPERT 3: BS Trần Văn Hoàng (expert3@carebridge.dev)
    IF v_exp3 IS NOT NULL AND v_m2 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000003001', v_m2, v_exp3, '94100000-0000-4000-8000-000000003001',
            'Tư vấn sốt sau tiêm vắc xin 6 trong 1 cho bé 3 tháng',
            'Bé nhà em sau khi tiêm 6in1 mũi 2 về bị sốt 38.2 độ, quấy khóc và bú ít hơn. Nhờ Bác sĩ hướng dẫn cách chăm sóc tại nhà.',
            v_today + interval '10 hours 30 minutes', v_today + interval '11 hours 30 minutes',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '2 days', now() - interval '1 hour', now() - interval '1 hour'
        ),
        (
            '94000000-0000-4000-8000-000000003002', v_m4, v_exp3, '94100000-0000-4000-8000-000000003002',
            'Tư vấn bé sơ sinh bị nấc cụt và trớ sữa',
            'Bé 1 tháng tuổi hay bị nấc cụt sau khi bú xong và trớ sữa ra mũi miệng. Nhờ Bác sĩ tư vấn tư thế vỗ ợ hơi chuẩn.',
            v_tomorrow + interval '14 hours', v_tomorrow + interval '15 hours',
            'ACCEPTED', NULL, '93000000-0000-4000-8000-000000000002', now() - interval '6 hours', v_exp3,
            now() + interval '4 days', now() - interval '12 hours', now() - interval '6 hours'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- Requests for EXPERT 4: BS Nguyễn Văn Minh (expert4@carebridge.dev)
    IF v_exp4 IS NOT NULL AND v_m3 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000004001', v_m3, v_exp4, '94100000-0000-4000-8000-000000004001',
            'Tư vấn thực đơn dinh dưỡng cho mẹ bầu 3 tháng giữa',
            'Mẹ bầu tuần 16 tăng cân chậm, ăn uống hay đầy bụng. Nhờ Bác sĩ dinh dưỡng thiết kế thực đơn bổ sung vi chất hợp lý.',
            v_today + interval '14 hours', v_today + interval '15 hours',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '3 days', now() - interval '4 hours', now() - interval '4 hours'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- Requests for EXPERT 5: BS Nguyễn Thị Lan (expert5@carebridge.dev)
    IF v_exp5 IS NOT NULL AND v_m4 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000005001', v_m4, v_exp5, '94100000-0000-4000-8000-000000005001',
            'Tư vấn giải tỏa lo âu và mất ngủ trong thai kỳ 3 tháng cuối',
            'Em sắp đến ngày sinh nên tâm lý rất căng thẳng, đêm hay mất ngủ và giật mình. Nhờ Chuyên gia tâm lý hướng dẫn phương pháp thư giãn.',
            v_today + interval '20 hours', v_today + interval '21 hours',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '3 days', now() - interval '2 hours', now() - interval '2 hours'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- Requests for EXPERT 6: BS Lê Văn Bình (expert6@carebridge.dev)
    IF v_exp6 IS NOT NULL AND v_m5 IS NOT NULL THEN
        INSERT INTO public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id,
            topic, description, preferred_window_start, preferred_window_end,
            status, reject_reason, direct_conversation_id, responded_at, responded_by,
            expires_at, created_at, updated_at
        ) VALUES (
            '94000000-0000-4000-8000-000000006001', v_m5, v_exp6, '94100000-0000-4000-8000-000000006001',
            'Tư vấn tiêm ngừa Phế cầu và Rota virus cho trẻ 2 tháng',
            'Bé chuẩn bị uống vắc xin Rota và tiêm Synflorix. Nhờ Bác sĩ tư vấn khoảng cách giữa các mũi tiêm và lưu ý chăm sóc.',
            v_tomorrow + interval '8 hours 30 minutes', v_tomorrow + interval '9 hours 30 minutes',
            'PENDING', NULL, NULL, NULL, NULL,
            now() + interval '3 days', now() - interval '5 hours', now() - interval '5 hours'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- ========================================================================
    -- 5. SEED CONSULTATION BOOKINGS (Lịch đặt ca tư vấn & Buổi tư vấn)
    -- ========================================================================

    -- Booking 1: CONFIRMED - Sắp diễn ra hôm nay với BS Đỗ Hải Long
    IF v_exp1 IS NOT NULL AND v_m1 IS NOT NULL THEN
        INSERT INTO public.consultation_bookings (
            booking_id, requester_user_id, expert_profile_id, availability_id,
            topic, scheduled_start, scheduled_end, price_snapshot_amount,
            status, communication_room_id, session_status, session_started_at,
            expert_summary, session_created_at, created_at, updated_at
        ) VALUES (
            '91000000-0000-4000-8000-000000001001', v_m1, v_exp1, '90000000-0000-4000-8000-000000001005',
            'Tư vấn chuyên sâu theo dõi thai kỳ nguy cơ cao & kế hoạch sinh',
            v_today + interval '19 hours', v_today + interval '20 hours', 350000.00,
            'CONFIRMED', 'room-consult-exp1-m1-today', 'SCHEDULED', NULL,
            NULL, now() - interval '1 day', now() - interval '1 day', now()
        ) ON CONFLICT (booking_id) DO NOTHING;

        -- Booking 2: COMPLETED - Đã hoàn thành hôm qua (kèm summary y khoa của Bác sĩ)
        INSERT INTO public.consultation_bookings (
            booking_id, requester_user_id, expert_profile_id, availability_id,
            topic, scheduled_start, scheduled_end, price_snapshot_amount,
            status, communication_room_id, session_status, session_started_at, session_ended_at,
            expert_summary, session_created_at, created_at, updated_at
        ) VALUES (
            '91000000-0000-4000-8000-000000001002', v_m1, v_exp1, NULL,
            'Tư vấn đọc kết quả xét nghiệm Double Test & NIPT tuần 12',
            v_yesterday + interval '15 hours', v_yesterday + interval '16 hours', 350000.00,
            'COMPLETED', 'room-consult-exp1-m1-yesterday', 'COMPLETED',
            v_yesterday + interval '15 hours 2 minutes', v_yesterday + interval '15 hours 55 minutes',
            'Bác sĩ kết luận: Kết quả xét nghiệm NIPT nguy cơ thấp (Down, Edwards, Patau đều âm tính). Độ mờ da gáy 1.2mm nằm trong giới hạn an toàn. Đề nghị mẹ duy trì uống Axit Folic + Sắt hữu cơ, hẹn tái khám siêu âm hình thái học ở tuần 20-22.',
            v_yesterday - interval '2 days', v_yesterday - interval '2 days', v_yesterday + interval '16 hours'
        ) ON CONFLICT (booking_id) DO NOTHING;
    END IF;

    -- Booking 3: CONFIRMED - Với BS Trần Thị Thu Nga
    IF v_exp2 IS NOT NULL AND v_m2 IS NOT NULL THEN
        INSERT INTO public.consultation_bookings (
            booking_id, requester_user_id, expert_profile_id, availability_id,
            topic, scheduled_start, scheduled_end, price_snapshot_amount,
            status, communication_room_id, session_status, session_started_at,
            expert_summary, session_created_at, created_at, updated_at
        ) VALUES (
            '91000000-0000-4000-8000-000000002001', v_m2, v_exp2, '90000000-0000-4000-8000-000000002003',
            'Tư vấn phục hồi sau sinh mổ và chăm sóc tầng sinh môn',
            v_today + interval '14 hours 30 minutes', v_today + interval '15 hours 30 minutes', 280000.00,
            'CONFIRMED', 'room-consult-exp2-m2-today', 'SCHEDULED', NULL,
            NULL, now() - interval '2 days', now() - interval '2 days', now()
        ) ON CONFLICT (booking_id) DO NOTHING;
    END IF;

    -- Booking 4: COMPLETED - Với BS Trần Văn Hoàng (Nhi khoa)
    IF v_exp3 IS NOT NULL AND v_m4 IS NOT NULL THEN
        INSERT INTO public.consultation_bookings (
            booking_id, requester_user_id, expert_profile_id, availability_id,
            topic, scheduled_start, scheduled_end, price_snapshot_amount,
            status, communication_room_id, session_status, session_started_at, session_ended_at,
            expert_summary, session_created_at, created_at, updated_at
        ) VALUES (
            '91000000-0000-4000-8000-000000003001', v_m4, v_exp3, NULL,
            'Khám và tư vấn tình trạng vàng da sinh lý ở trẻ sơ sinh 10 ngày tuổi',
            v_yesterday + interval '9 hours', v_yesterday + interval '10 hours', 250000.00,
            'COMPLETED', 'room-consult-exp3-m4-yesterday', 'COMPLETED',
            v_yesterday + interval '9 hours 5 minutes', v_yesterday + interval '9 hours 45 minutes',
            'Bác sĩ kết luận: Bé vàng da nhẹ vùng mặt và ngực, phân vàng, bú tốt, không sốt, không li bì. Đây là vàng da sinh lý thông thường. Hướng dẫn mẹ cho bé bú mẹ hoàn toàn 8-10 cữ/ngày để tăng đào thải bilirubin. Tái khám nếu vàng da lan xuống bụng hoặc đùi.',
            v_yesterday - interval '3 days', v_yesterday - interval '3 days', v_yesterday + interval '10 hours'
        ) ON CONFLICT (booking_id) DO NOTHING;
    END IF;

    -- Booking 5: COMPLETED - Với BS Nguyễn Văn Minh (Dinh dưỡng)
    IF v_exp4 IS NOT NULL AND v_m3 IS NOT NULL THEN
        INSERT INTO public.consultation_bookings (
            booking_id, requester_user_id, expert_profile_id, availability_id,
            topic, scheduled_start, scheduled_end, price_snapshot_amount,
            status, communication_room_id, session_status, session_started_at, session_ended_at,
            expert_summary, session_created_at, created_at, updated_at
        ) VALUES (
            '91000000-0000-4000-8000-000000004001', v_m3, v_exp4, NULL,
            'Tư vấn dinh dưỡng kiểm soát cân nặng và phòng đái tháo đường thai kỳ',
            v_yesterday + interval '14 hours', v_yesterday + interval '15 hours', 320000.00,
            'COMPLETED', 'room-consult-exp4-m3-yesterday', 'COMPLETED',
            v_yesterday + interval '14 hours 2 minutes', v_yesterday + interval '14 hours 50 minutes',
            'Bác sĩ kết luận: Đã lên thực đơn chi tiết 7 ngày chia làm 3 bữa chính + 2 bữa phụ. Hạn chế tinh bột nhanh (cơm trắng, bánh mì trắng), tăng cường yến mạch, gạo lứt, rau xanh đậm và đạm từ cá hồi, ức gà, trứng.',
            v_yesterday - interval '4 days', v_yesterday - interval '4 days', v_yesterday + interval '15 hours'
        ) ON CONFLICT (booking_id) DO NOTHING;
    END IF;

    RAISE NOTICE 'CareBridge expert consultation seed data applied successfully!';
END
$carebridge_expert_consultation_seed$;
