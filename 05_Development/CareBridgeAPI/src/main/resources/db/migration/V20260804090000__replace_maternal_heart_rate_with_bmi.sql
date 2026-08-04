UPDATE public.health_metric_definitions
SET is_active = false,
    effective_until = COALESCE(effective_until, now()),
    updated_at = now()
WHERE metric_code = 'MATERNAL_HEART_RATE'
  AND is_active = true;

INSERT INTO public.health_metric_definitions (
    metric_code,
    version,
    display_name,
    observation_shape,
    manual_entry_supported,
    device_import_supported,
    canonical_unit,
    accepted_input_units_jsonb,
    precision_scale,
    required_context_schema_jsonb,
    aggregation_policy_jsonb,
    chart_policy_jsonb,
    quality_policy_jsonb,
    allowed_journey_stages_jsonb
)
VALUES (
    'BMI',
    1,
    'Chỉ số BMI',
    'POINT',
    true,
    false,
    'kg/m²',
    '["kg/m²"]'::jsonb,
    2,
    '{"required":["weightKg","heightCm"],"weightUnit":"kg","heightUnit":"cm"}'::jsonb,
    '{"method":"REPRESENTATIVE_DAILY_POINT","retainSourceMeasurements":true}'::jsonb,
    '{"type":"LINE","xAxis":"GESTATIONAL_WEEK_OR_DATE"}'::jsonb,
    '{"required":false,"pregnancyUse":"BMI is most clinically meaningful from pre-pregnancy weight"}'::jsonb,
    '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
)
ON CONFLICT (metric_code, version) DO UPDATE
SET display_name = EXCLUDED.display_name,
    observation_shape = EXCLUDED.observation_shape,
    manual_entry_supported = EXCLUDED.manual_entry_supported,
    device_import_supported = EXCLUDED.device_import_supported,
    canonical_unit = EXCLUDED.canonical_unit,
    accepted_input_units_jsonb = EXCLUDED.accepted_input_units_jsonb,
    precision_scale = EXCLUDED.precision_scale,
    required_context_schema_jsonb = EXCLUDED.required_context_schema_jsonb,
    aggregation_policy_jsonb = EXCLUDED.aggregation_policy_jsonb,
    chart_policy_jsonb = EXCLUDED.chart_policy_jsonb,
    quality_policy_jsonb = EXCLUDED.quality_policy_jsonb,
    allowed_journey_stages_jsonb = EXCLUDED.allowed_journey_stages_jsonb,
    is_active = true,
    effective_until = NULL,
    updated_at = now();
