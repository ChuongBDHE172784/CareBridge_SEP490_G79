CREATE OR REPLACE FUNCTION enforce_pregnancy_outcome_evidence_owner()
RETURNS trigger AS $$
DECLARE
    journey_owner UUID;
BEGIN
    SELECT owner_user_id
      INTO journey_owner
      FROM mother_journeys
     WHERE journey_id = NEW.journey_id;

    IF journey_owner IS NULL
       OR NEW.owner_user_id <> journey_owner
       OR NEW.actor_user_id <> journey_owner THEN
        RAISE EXCEPTION 'pregnancy outcome evidence owner must match journey owner';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pregnancy_outcome_evidence_owner
BEFORE INSERT ON pregnancy_outcome_evidence
FOR EACH ROW EXECUTE FUNCTION enforce_pregnancy_outcome_evidence_owner();
