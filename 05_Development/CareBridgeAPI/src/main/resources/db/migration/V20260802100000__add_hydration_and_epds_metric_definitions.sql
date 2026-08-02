INSERT INTO public.health_metric_definitions (metric_code, version, display_name, observation_shape, manual_entry_supported, device_import_supported, canonical_unit, accepted_input_units_jsonb, precision_scale, required_context_schema_jsonb, aggregation_policy_jsonb, chart_policy_jsonb, quality_policy_jsonb, allowed_journey_stages_jsonb)
SELECT 'HYDRATION', 1, 'Nước uống', 'POINT', true, false, 'ml', '["ml"]'::jsonb, 0, '{}'::jsonb, '{"method":"SUM_DAILY"}'::jsonb, '{"type":"BAR"}'::jsonb, '{"required":false}'::jsonb, '["PRE_PREGNANCY","PREGNANCY","POSTPARTUM"]'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM public.health_metric_definitions WHERE metric_code = 'HYDRATION' AND version = 1);

INSERT INTO public.health_metric_definitions (metric_code, version, display_name, observation_shape, manual_entry_supported, device_import_supported, canonical_unit, accepted_input_units_jsonb, precision_scale, required_context_schema_jsonb, aggregation_policy_jsonb, chart_policy_jsonb, quality_policy_jsonb, allowed_journey_stages_jsonb)
SELECT 'EPDS_SCORE', 1, 'Sàng lọc EPDS', 'POINT', true, false, 'điểm', '["điểm"]'::jsonb, 0, '{}'::jsonb, '{"method":"LATEST"}'::jsonb, '{"type":"SESSION_TIMELINE"}'::jsonb, '{"required":false}'::jsonb, '["PREGNANCY","POSTPARTUM"]'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM public.health_metric_definitions WHERE metric_code = 'EPDS_SCORE' AND version = 1);
