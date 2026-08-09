-- Intake pre-screen keywords for maternal haemorrhage and self-harm.
--
-- The pre-screen (TriageRedFlagPreScreenPolicy) is the layer that runs in Spring before the
-- Python engine is called, so it is the one that still works when that engine is degraded.
-- It matched none of these presentations: a mother writing "tôi bị băng huyết" or "tôi không
-- muốn sống nữa" passed the pre-screen untouched, because the seeded set carried only
-- 'chảy máu nhiều', 'xuất huyết' and 'muốn chết'. Postpartum haemorrhage is the leading
-- direct cause of maternal death, and these are the ordinary ways of reporting it.
--
-- Every keyword here was checked for an accent-stripped twin first. The pre-screen now keeps
-- diacritics (TriageRedFlagPreScreenPolicy.matches), so an accented keyword no longer matches
-- a differently-accented word — but a writer typing without accents still cannot be told
-- apart, and this layer escalates rather than annotates. Three otherwise-obvious candidates
-- are deliberately NOT added, because their accent-free form is a common everyday word:
--   'ra máu nhiều' -> "ra mau nhieu" is also "ra màu nhiều" (dye running in the wash)
--   'tự tử'        -> "tu tu"        is also "từ từ"        ("slowly", as in "bé bú từ từ")
--   'tự sát'       -> "tu sat"       is also "tủ sát"       ("wardrobe against the wall")
-- The Python engine covers all three for writers who do use diacritics, which is the only
-- place the two readings separate. 'muốn chết' (already seeded) and the two "sống" phrases
-- below carry the same clinical signal without an everyday twin.
--
-- Idempotent, matching the V2 seed convention: keyword is unique and re-running is a no-op.
INSERT INTO public.red_flag_rules
    (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at)
VALUES
    ('0f1c3a52-6d47-4d1e-9a2b-5c8e1f9d4a70', 'băng huyết',         'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('1a2d4b63-7e58-4e2f-8b3c-6d9f2a0e5b81', 'ướt đẫm băng',       'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('2b3e5c74-8f69-4f30-9c4d-7e0a3b1f6c92', 'thấm ướt băng',      'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('3c4f6d85-9a7a-4041-8d5e-8f1b4c2a7d03', 'máu ồ ạt',           'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('4d5a7e96-ab8b-4152-9e6f-9a2c5d3b8e14', 'mất máu nhiều',      'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('5e6b8fa7-bc9c-4263-8f70-ab3d6e4c9f25', 'không muốn sống',    'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('6f7c90b8-cdad-4374-9081-bc4e7f5d0a36', 'không thiết sống',   'RED', 'ESCALATE', true, true, NULL, NULL, now(), now()),
    ('708da1c9-debe-4485-a192-cd5f806e1b47', 'kết liễu',           'RED', 'ESCALATE', true, true, NULL, NULL, now(), now())
ON CONFLICT (keyword) DO NOTHING;
