CREATE INDEX IF NOT EXISTS idx_health_device_connections_status
  ON public.health_device_connections (status);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'maternal_health_metrics_source_reference_id_fkey'
  ) THEN
    ALTER TABLE public.maternal_health_metrics
      ADD CONSTRAINT maternal_health_metrics_source_reference_id_fkey
      FOREIGN KEY (source_reference_id)
      REFERENCES public.health_device_connections(connection_id);
  END IF;
END $$;
