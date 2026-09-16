-- Ensure general lifecycle milestone, nutrition, and essential care articles are not tagged
-- with narrow rec-* constraint tags so they serve as universal fallback articles
-- across PRE_PREGNANCY, PREGNANCY, and POSTPARTUM stages.

-- 1. Postpartum fallback articles (newborn care & postpartum recovery)
DELETE FROM public.content_item_topics cit
WHERE cit.content_item_id IN (
    '3404cca3-db42-499c-ba69-927b2736b54d', -- Chăm sóc sau sinh thường
    '5d0b09eb-bfed-4433-99e3-15ac7539b07c', -- Tắm cho trẻ sơ sinh
    'b7350698-382f-420d-9254-3eb47306402a', -- Chăm sóc sức khỏe trẻ sơ sinh
    '65592152-dcd2-40ec-8021-ed162798a3a4', -- Phát triển của trẻ 0-3 tháng
    'f4c60bc8-905b-4d94-a797-63cda576edd7'  -- Trẻ sơ sinh cần bú bao nhiêu
)
AND cit.topic_id IN (
    SELECT id FROM public.community_topics WHERE slug LIKE 'rec-%'
);

-- 2. Pregnancy fallback articles (trimester milestones, nutrition, vaccination)
DELETE FROM public.content_item_topics cit
WHERE cit.content_item_id IN (
    '5c1cd988-28c9-47e7-8ba5-b6a1f279846d', -- Vừa biết mình có thai (tuần 1-6)
    'd5d516cf-43e4-4de5-9c5f-79eabf04b97c', -- Tam cá nguyệt thứ nhất (tuần 1-13)
    'fda0242a-e99d-48f2-b71e-6ecbedc578a2', -- Dinh dưỡng thai kỳ (tuần 1-40)
    'a2ace661-8cb6-4116-aef6-15861c8d062e', -- Tam cá nguyệt thứ hai (tuần 14-27)
    'f20882f0-8b7d-44d7-8f2d-9b4fe2b3c3f0', -- Tiêm vaccine thai kỳ (tuần 14-36)
    '314d533e-b431-478b-9090-5d50a7c84141'  -- Tam cá nguyệt thứ ba (tuần 28-41)
)
AND cit.topic_id IN (
    SELECT id FROM public.community_topics WHERE slug LIKE 'rec-%'
);

-- 3. Pre-pregnancy fallback articles (preconception health, planning, folic acid)
DELETE FROM public.content_item_topics cit
WHERE cit.content_item_id IN (
    'da93e7d8-6a78-496c-8bf0-278a0a123376', -- Khám và tư vấn trước khi mang thai
    '94ee99a8-6615-4cb2-9e98-000325e5ac41', -- Chuẩn bị cho việc mang thai
    'b2d7b144-1f28-43c1-b5a5-9ecfd9dea8ec', -- Chăm sóc trước khi mang thai
    '4659af5c-06c4-4940-b589-6a9869a1c201', -- Thai kỳ khỏe mạnh trước khi mang thai
    'e7173fb5-b1d9-4998-963c-16753d1e54a4'  -- Axit folic khuyến nghị
)
AND cit.topic_id IN (
    SELECT id FROM public.community_topics WHERE slug LIKE 'rec-%'
);
