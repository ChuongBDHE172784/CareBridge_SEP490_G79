ALTER TABLE public.mother_baseline_contexts
    ADD COLUMN source varchar(30) NOT NULL DEFAULT 'SELF_REPORTED';

ALTER TABLE public.mother_baseline_contexts
    ALTER COLUMN source DROP DEFAULT;

ALTER TABLE public.mother_baseline_contexts
    ADD CONSTRAINT chk_mother_baseline_source
    CHECK (source IN ('SELF_REPORTED'));
