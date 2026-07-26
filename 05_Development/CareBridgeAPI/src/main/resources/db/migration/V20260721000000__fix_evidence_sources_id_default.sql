-- Repair migration for V20260716092000 evidence_sources
-- Fix: table exists but id column missing DEFAULT gen_random_uuid()
-- Backfill any NULL id values, then ensure default is set

-- 1. Ensure id column has DEFAULT gen_random_uuid()
ALTER TABLE evidence_sources
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- 2. Backfill any existing NULL id values
UPDATE evidence_sources
SET id = gen_random_uuid()
WHERE id IS NULL;

-- 3. Ensure id is NOT NULL (should already be from primary key, but ensure constraint)
ALTER TABLE evidence_sources
    ALTER COLUMN id SET NOT NULL;

-- 4. Now the seed insert from V20260716092000 will work
-- Re-run the seed insert (ON CONFLICT DO NOTHING handles duplicates)
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