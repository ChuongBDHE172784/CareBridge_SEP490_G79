-- ============================================================================
-- Canonical Vietnamese childhood vaccination catalogue — schedule_version 'vn-2026'
--
-- Source: 08_References/lich_tiem_chung.md (hieuvetiemchung.com, tra cứu 2026-08-07).
--
-- The seven pre-existing rows in vaccination_schedules each carry their own
-- per-row schedule_version ('legacy-<uuid-prefix>'), so they are NOT part of any
-- coherent catalogue and cannot be scanned as a set. This migration introduces a
-- single versioned catalogue that the vaccination book initializer scans when a
-- baby is registered. The legacy rows are left in place — vaccination_records
-- may reference them through vaccination_schedule_id — but they are excluded from
-- the active catalogue by version.
--
-- offset_days is measured from the baby's birth date, using 30-day months to stay
-- consistent with the existing rows (30/60/90/120). Age ranges from the source
-- table ("Từ 16–24 tháng") are materialised at the start of the range, with the
-- full range preserved in description.
--
-- Idempotent: ON CONFLICT on vaccination_schedules_key_uk (vaccine_name,
-- dose_number, schedule_version).
-- ============================================================================

INSERT INTO public.vaccination_schedules
    (vaccine_name, dose_number, offset_days, description, schedule_version)
VALUES
    -- Sơ sinh
    ('Viêm gan B (HepB) sơ sinh',            1,   0, 'Trong vòng 24 giờ sau sinh — phòng viêm gan B',                              'vn-2026'),
    ('Lao (BCG)',                            1,   0, 'Trong vòng 1 tháng sau sinh — phòng lao',                                    'vn-2026'),

    -- 02 tháng
    ('Vắc-xin 6 trong 1',                    1,  60, '02 tháng — bạch hầu, uốn ván, ho gà, bại liệt, Hib, viêm gan B',             'vn-2026'),
    ('Rota (uống)',                          1,  60, '02 tháng — tiêu chảy do vi rút Rota, liều uống 1',                           'vn-2026'),
    ('Phế cầu (PCV-10/PCV-13)',              1,  60, '02 tháng — viêm phổi, viêm màng não, viêm tai giữa do phế cầu',              'vn-2026'),
    ('Não mô cầu nhóm B',                    1,  60, '02 tháng — não mô cầu nhóm huyết thanh B, mũi 1',                            'vn-2026'),

    -- 03 tháng
    ('Vắc-xin 6 trong 1',                    2,  90, '03 tháng — bạch hầu, uốn ván, ho gà, bại liệt, Hib, viêm gan B',             'vn-2026'),
    ('Rota (uống)',                          2,  90, '03 tháng — tiêu chảy do vi rút Rota, liều uống 2',                           'vn-2026'),
    ('Phế cầu (PCV-10/PCV-13)',              2,  90, '03 tháng — viêm phổi, viêm màng não, viêm tai giữa do phế cầu',              'vn-2026'),

    -- 04 tháng
    ('Vắc-xin 6 trong 1',                    3, 120, '04 tháng — bạch hầu, uốn ván, ho gà, bại liệt, Hib, viêm gan B',             'vn-2026'),
    ('Rota (uống)',                          3, 120, '04 tháng — tiêu chảy do vi rút Rota, liều uống 3 (tùy loại vắc xin)',        'vn-2026'),
    ('Phế cầu (PCV-10/PCV-13)',              3, 120, '04 tháng — viêm phổi, viêm màng não, viêm tai giữa do phế cầu',              'vn-2026'),
    ('Não mô cầu nhóm B',                    2, 120, '04 tháng — não mô cầu nhóm huyết thanh B, mũi 2',                            'vn-2026'),

    -- 06 tháng
    ('Cúm mùa',                              1, 180, '06 tháng — mũi cơ bản 1, nhắc lại hàng năm',                                 'vn-2026'),
    ('Cúm mùa',                              2, 210, '07 tháng — mũi cơ bản 2, cách mũi 1 một tháng',                              'vn-2026'),
    ('Não mô cầu B+C',                       1, 180, '06 tháng — viêm màng não do não mô cầu B+C, mũi 1',                          'vn-2026'),
    ('Não mô cầu B+C',                       2, 225, 'Cách mũi 1 khoảng 6–8 tuần — não mô cầu B+C, mũi 2',                         'vn-2026'),

    -- Từ 09 tháng
    ('Sởi hoặc Sởi - Quai bị - Rubella',     1, 270, 'Từ 09 tháng — Sởi (M) hoặc MMR',                                             'vn-2026'),
    ('Não mô cầu A,C,Y,W-135',               1, 270, 'Từ 09 tháng — viêm màng não do não mô cầu ACWY, mũi 1',                      'vn-2026'),
    ('Viêm não Nhật Bản (JE-CV)',            1, 270, 'Từ 09 tháng — viêm não Nhật Bản, mũi 1 cơ bản',                              'vn-2026'),
    ('Thủy đậu',                             1, 270, 'Từ 09 tháng — phòng thủy đậu, mũi 1',                                        'vn-2026'),

    -- Từ 12 tháng
    ('Não mô cầu A,C,Y,W-135',               2, 360, 'Từ 12 tháng — viêm màng não do não mô cầu ACWY, mũi 2',                      'vn-2026'),
    ('Viêm não Nhật Bản bất hoạt (JEEV)',    1, 360, 'Từ 12 tháng — VNNB bất hoạt trên tế bào Vero, mũi 1',                        'vn-2026'),
    ('Viêm não Nhật Bản bất hoạt (JEEV)',    2, 388, 'Cách mũi 1 đúng 28 ngày — VNNB bất hoạt trên tế bào Vero, mũi 2',            'vn-2026'),
    ('Viêm gan A',                           1, 360, 'Từ 12 tháng — viêm gan A hoặc vắc-xin kết hợp VG A + VGB, mũi 1',            'vn-2026'),
    ('Não mô cầu nhóm B',                    3, 360, 'Từ 12 tháng — não mô cầu nhóm 4CMenB, mũi 3',                                'vn-2026'),
    ('Thủy đậu',                             2, 360, 'Từ 12 tháng — phòng thủy đậu, mũi 2',                                        'vn-2026'),

    -- Từ 16–24 tháng
    ('Vắc-xin 6 trong 1',                    4, 480, 'Từ 16–24 tháng — mũi 4 nhắc lại',                                            'vn-2026'),
    ('Phế cầu (PCV-10/PCV-13)',              4, 480, 'Từ 16–24 tháng — phế cầu, mũi 4 nhắc lại',                                   'vn-2026'),
    ('Viêm gan A',                           2, 540, 'Nhắc lại sau mũi 1 khoảng 18 tháng — viêm gan A đơn giá hoặc kết hợp A+B',   'vn-2026')
ON CONFLICT (vaccine_name, dose_number, schedule_version) DO NOTHING;
