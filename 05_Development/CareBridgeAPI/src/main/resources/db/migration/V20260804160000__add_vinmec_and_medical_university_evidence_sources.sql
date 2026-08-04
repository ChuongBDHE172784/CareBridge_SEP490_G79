-- Adds Vinmec (private hospital system covering OB/GYN + pediatrics) and two
-- well-known Vietnamese medical university domains to the evidence source
-- whitelist (knowledge_sources), following the same direct-APPROVED/SEED
-- pattern already used by V2__seed_reference_data.sql for moh.gov.vn,
-- who.int, benhviennhitrunguong.gov.vn, etc. Domains verified live via
-- independent web search before insertion (no fabricated URLs).
--
-- This only whitelists the DOMAIN for citation retrieval (local KB +
-- realtime official-source search). It does not assert that any specific
-- article/excerpt is clinically approved; individual pages are still
-- validated per-request against normalized symptoms before being cited.
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('f1a2b3c4-5d6e-4f70-8a91-b2c3d4e5f601', 'vinmec.com', 'https://www.vinmec.com', 'Hệ thống Y tế Vinmec', 'HOSPITAL', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,POSTPARTUM,INFANT,TODDLER', NULL, NULL, '2026-08-04 16:00:00+07', 'Seeded private hospital system source (maternal + pediatric coverage)', NULL, '2026-08-04 16:00:00+07', '2026-08-04 16:00:00+07') ON CONFLICT DO NOTHING;
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('f1a2b3c4-5d6e-4f70-8a91-b2c3d4e5f602', 'hmu.edu.vn', 'https://hmu.edu.vn', 'Trường Đại học Y Hà Nội', 'UNIVERSITY', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,POSTPARTUM,INFANT,TODDLER', NULL, NULL, '2026-08-04 16:00:00+07', 'Seeded medical university source', NULL, '2026-08-04 16:00:00+07', '2026-08-04 16:00:00+07') ON CONFLICT DO NOTHING;
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('f1a2b3c4-5d6e-4f70-8a91-b2c3d4e5f603', 'ump.edu.vn', 'https://ump.edu.vn', 'Đại học Y Dược Thành phố Hồ Chí Minh', 'UNIVERSITY', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,POSTPARTUM,INFANT,TODDLER', NULL, NULL, '2026-08-04 16:00:00+07', 'Seeded medical university source', NULL, '2026-08-04 16:00:00+07', '2026-08-04 16:00:00+07') ON CONFLICT DO NOTHING;
