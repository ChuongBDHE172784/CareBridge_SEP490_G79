CREATE TABLE IF NOT EXISTS evidence_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    organization VARCHAR(255) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    discovery_mode VARCHAR(40) NOT NULL,
    applicable_stages TEXT NOT NULL,
    added_by UUID NULL REFERENCES users(user_id),
    reviewed_by UUID NULL REFERENCES users(user_id),
    reviewed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evidence_sources_domain UNIQUE (domain),
    CONSTRAINT chk_evidence_sources_category CHECK (category IN ('GOVERNMENT', 'WHO_UNICEF', 'HOSPITAL', 'CDC', 'OTHER')),
    CONSTRAINT chk_evidence_sources_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'DEPRECATED', 'ARCHIVED')),
    CONSTRAINT chk_evidence_sources_discovery CHECK (discovery_mode IN ('SEED', 'REALTIME_SEARCH_DISCOVERED', 'MANUAL_ADMIN_ADD')),
    CONSTRAINT chk_evidence_sources_https CHECK (base_url LIKE 'https://%')
);

CREATE TABLE IF NOT EXISTS evidence_source_review_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evidence_source_id UUID NOT NULL REFERENCES evidence_sources(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    actor_user_id UUID NULL REFERENCES users(user_id),
    actor_role VARCHAR(80),
    notes TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evidence_sources_status ON evidence_sources(status);
CREATE INDEX IF NOT EXISTS idx_evidence_sources_domain_status ON evidence_sources(domain, status);
CREATE INDEX IF NOT EXISTS idx_evidence_review_log_source ON evidence_source_review_log(evidence_source_id, changed_at DESC);

INSERT INTO evidence_sources (domain, base_url, organization, category, status, discovery_mode, applicable_stages, reviewed_at, notes)
VALUES
    ('moh.gov.vn', 'https://moh.gov.vn', 'Bộ Y tế Việt Nam', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official government source'),
    ('adminmoh.moh.gov.vn', 'https://adminmoh.moh.gov.vn', 'Bộ Y tế Việt Nam', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official government source'),
    ('mch.moh.gov.vn', 'https://mch.moh.gov.vn', 'Cục Bà mẹ và Trẻ em', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official maternal and child health source'),
    ('who.int', 'https://www.who.int', 'World Health Organization', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official international source'),
    ('iris.who.int', 'https://iris.who.int', 'World Health Organization IRIS', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official international source'),
    ('unicef.org', 'https://www.unicef.org', 'UNICEF', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official international source'),
    ('cdc.gov', 'https://www.cdc.gov', 'Centers for Disease Control and Prevention', 'CDC', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NOW(), 'Seeded official international source'),
    ('benhviennhitrunguong.gov.vn', 'https://benhviennhitrunguong.gov.vn', 'Bệnh viện Nhi Trung ương', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NOW(), 'Seeded pediatric hospital source'),
    ('nhidong.org.vn', 'https://nhidong.org.vn', 'Bệnh viện Nhi Đồng 1', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NOW(), 'Seeded pediatric hospital source'),
    ('bvndtp.org.vn', 'https://bvndtp.org.vn', 'Bệnh viện Nhi Đồng Thành phố', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NOW(), 'Seeded pediatric hospital source')
ON CONFLICT (domain) DO NOTHING;
