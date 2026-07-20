ALTER TABLE expert_profiles
    ADD COLUMN IF NOT EXISTS specialty_id VARCHAR(5),
    ADD COLUMN IF NOT EXISTS hospital_id VARCHAR(8);

UPDATE expert_profiles ep
SET specialty_id = s.specialty_id
FROM specialties s
WHERE ep.specialty_id IS NULL AND LOWER(TRIM(ep.specialty)) = LOWER(TRIM(s.name));

UPDATE expert_profiles ep
SET hospital_id = h.hospital_id
FROM hospitals h
WHERE ep.hospital_id IS NULL AND LOWER(TRIM(ep.workplace)) = LOWER(TRIM(h.name));

ALTER TABLE expert_profiles
    ADD CONSTRAINT fk_expert_profile_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties(specialty_id),
    ADD CONSTRAINT fk_expert_profile_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id);
