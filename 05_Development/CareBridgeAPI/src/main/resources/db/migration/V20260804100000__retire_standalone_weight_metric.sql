UPDATE public.health_metric_definitions
SET is_active = false,
    effective_until = COALESCE(effective_until, now()),
    updated_at = now()
WHERE metric_code = 'WEIGHT'
  AND is_active = true;
