-- === RED FLAG RULES SCHEMA (UC-110) ===
-- Admin-manageable AI safety keyword rules. Additive to (NOT a replacement for) the
-- hardcoded floor list in TriageRedFlagPolicy.FLOOR_KEYWORDS — see ADR-001 (BR-SAFETY).

CREATE TABLE red_flag_rules (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    keyword             VARCHAR(255)    NOT NULL,                    -- substring/phrase matched case-insensitively against user query
    severity            VARCHAR(20)     NOT NULL,                    -- GREEN | YELLOW | RED
    action              VARCHAR(20)     NOT NULL,                    -- BLOCK | WARN | ESCALATE
    is_active           BOOLEAN         NOT NULL DEFAULT true,
    is_system_default   BOOLEAN         NOT NULL DEFAULT false,      -- true = seeded from original hardcoded floor list; cannot be deleted/deactivated via API (BR-SAFETY-RFR-003)
    created_by          UUID            NULL,                        -- nullable: seed rows have no human actor (see note below); REFERENCES users(user_id) for admin-created rows
    updated_by          UUID            NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_red_flag_rules_severity CHECK (severity IN ('GREEN', 'YELLOW', 'RED')),
    CONSTRAINT chk_red_flag_rules_action   CHECK (action IN ('BLOCK', 'WARN', 'ESCALATE')),
    CONSTRAINT uq_red_flag_rules_keyword   UNIQUE (keyword),
    CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

CREATE INDEX idx_red_flag_rules_active_severity ON red_flag_rules(is_active, severity);
CREATE INDEX idx_red_flag_rules_is_system_default ON red_flag_rules(is_system_default);

-- Seed: mirror the existing hardcoded TriageRedFlagPolicy.RED_FLAG_KEYWORDS (19 phrases) as
-- non-deletable system-default RED/ESCALATE rows so the Admin UI can display the full effective
-- rule set. created_by is NULL because no fixed-UUID seed user exists at Flyway-migration time —
-- test/admin accounts (e.g. admin@carebridge.dev) are created at application startup by
-- DevDataSeeder.java, not via Flyway (verified: no `INSERT INTO users` in any migration file).
INSERT INTO red_flag_rules (keyword, severity, action, is_active, is_system_default, created_by) VALUES
    ('chảy máu nhiều',        'RED', 'ESCALATE', true, true, NULL),
    ('ngất xỉu',               'RED', 'ESCALATE', true, true, NULL),
    ('khó thở',                'RED', 'ESCALATE', true, true, NULL),
    ('co giật',                'RED', 'ESCALATE', true, true, NULL),
    ('tim ngừng đập',          'RED', 'ESCALATE', true, true, NULL),
    ('xuất huyết',             'RED', 'ESCALATE', true, true, NULL),
    ('hôn mê',                 'RED', 'ESCALATE', true, true, NULL),
    ('đau ngực dữ dội',        'RED', 'ESCALATE', true, true, NULL),
    ('sảy thai',               'RED', 'ESCALATE', true, true, NULL),
    ('sinh non',               'RED', 'ESCALATE', true, true, NULL),
    ('ngộ độc',                'RED', 'ESCALATE', true, true, NULL),
    ('bất tỉnh',               'RED', 'ESCALATE', true, true, NULL),
    ('đuối nước',              'RED', 'ESCALATE', true, true, NULL),
    ('gãy xương hở',           'RED', 'ESCALATE', true, true, NULL),
    ('bỏng nặng',              'RED', 'ESCALATE', true, true, NULL),
    ('mất ý thức',             'RED', 'ESCALATE', true, true, NULL),
    ('không thở',              'RED', 'ESCALATE', true, true, NULL),
    ('đau bụng dữ dội',        'RED', 'ESCALATE', true, true, NULL),
    ('chảy máu âm đạo nhiều',  'RED', 'ESCALATE', true, true, NULL)
ON CONFLICT (keyword) DO NOTHING;
