-- UC-228: ViewVaccinationSchedule — MoH VN standard reference schedule
CREATE TABLE IF NOT EXISTS vaccination_reference_schedules (
    ref_id       UUID         NOT NULL DEFAULT gen_random_uuid(),
    vaccine_name VARCHAR(200) NOT NULL,
    dose_number  SMALLINT     NOT NULL,
    offset_days  INT          NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_vaccination_ref PRIMARY KEY (ref_id)
);

INSERT INTO vaccination_reference_schedules (vaccine_name, dose_number, offset_days, description)
SELECT vaccine_name, dose_number, offset_days, description FROM (
    VALUES
    ('BCG',         1, 0,   'BCG — phòng lao, tiêm ngay sau sinh'),
    ('Viem gan B',  1, 0,   'Viêm gan B liều 1 — tiêm trong 24h sau sinh'),
    ('Viem gan B',  2, 30,  'Viêm gan B liều 2 — 1 tháng tuổi'),
    ('Viem gan B',  3, 60,  'Viêm gan B liều 3 — 2 tháng tuổi'),
    ('DTP-VGB-Hib', 1, 60,  'DTP-VGB-Hib liều 1 — 2 tháng tuổi'),
    ('DTP-VGB-Hib', 2, 90,  'DTP-VGB-Hib liều 2 — 3 tháng tuổi'),
    ('DTP-VGB-Hib', 3, 120, 'DTP-VGB-Hib liều 3 — 4 tháng tuổi')
) AS new_rows (vaccine_name, dose_number, offset_days, description)
WHERE NOT EXISTS (
    SELECT 1 FROM vaccination_reference_schedules
);
