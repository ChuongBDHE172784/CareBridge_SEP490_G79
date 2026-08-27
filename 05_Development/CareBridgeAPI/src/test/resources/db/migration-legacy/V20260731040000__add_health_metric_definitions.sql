CREATE TABLE public.health_metric_definitions (
    metric_definition_id uuid DEFAULT gen_random_uuid() NOT NULL,
    metric_code varchar(60) NOT NULL,
    version integer NOT NULL,
    display_name varchar(120) NOT NULL,
    observation_shape varchar(30) NOT NULL,
    subject_type varchar(30) DEFAULT 'MOTHER' NOT NULL,
    manual_entry_supported boolean DEFAULT false NOT NULL,
    device_import_supported boolean DEFAULT false NOT NULL,
    canonical_unit varchar(30),
    accepted_input_units_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    precision_scale smallint,
    required_context_schema_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    plausibility_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    aggregation_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    chart_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    quality_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    safety_policy_version varchar(40),
    allowed_journey_stages_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    effective_from timestamptz DEFAULT now() NOT NULL,
    effective_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT health_metric_definitions_pk PRIMARY KEY (metric_definition_id),
    CONSTRAINT health_metric_definitions_code_version_uk UNIQUE (metric_code, version),
    CONSTRAINT health_metric_definitions_code_ck CHECK (btrim(metric_code) <> ''),
    CONSTRAINT health_metric_definitions_display_name_ck CHECK (btrim(display_name) <> ''),
    CONSTRAINT health_metric_definitions_version_ck CHECK (version > 0),
    CONSTRAINT health_metric_definitions_shape_ck CHECK (observation_shape IN (
        'POINT', 'PAIRED_POINT', 'SESSION', 'INTERVAL_AGGREGATE'
    )),
    CONSTRAINT health_metric_definitions_subject_ck CHECK (subject_type = 'MOTHER'),
    CONSTRAINT health_metric_definitions_precision_ck CHECK (
        precision_scale IS NULL OR precision_scale >= 0
    ),
    CONSTRAINT health_metric_definitions_effective_period_ck CHECK (
        effective_until IS NULL OR effective_until > effective_from
    ),
    CONSTRAINT health_metric_definitions_units_json_ck CHECK (
        jsonb_typeof(accepted_input_units_jsonb) = 'array'
    ),
    CONSTRAINT health_metric_definitions_context_json_ck CHECK (
        jsonb_typeof(required_context_schema_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_plausibility_json_ck CHECK (
        jsonb_typeof(plausibility_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_aggregation_json_ck CHECK (
        jsonb_typeof(aggregation_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_chart_json_ck CHECK (
        jsonb_typeof(chart_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_quality_json_ck CHECK (
        jsonb_typeof(quality_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_stages_json_ck CHECK (
        jsonb_typeof(allowed_journey_stages_jsonb) = 'array'
    )
);

CREATE UNIQUE INDEX health_metric_definitions_active_code_uk
    ON public.health_metric_definitions(metric_code)
    WHERE is_active = true;

CREATE INDEX health_metric_definitions_active_display_ix
    ON public.health_metric_definitions(is_active, display_name);

CREATE INDEX health_metric_definitions_effective_period_ix
    ON public.health_metric_definitions(effective_from, effective_until);

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
VALUES
    (
        'WEIGHT', 1, 'Cân nặng', 'POINT', true, false, 'kg',
        '["kg", "lb"]'::jsonb, 2, '{}'::jsonb,
        '{"method":"REPRESENTATIVE_DAILY_POINT","baselineAware":true}'::jsonb,
        '{"type":"LINE","xAxis":"GESTATIONAL_WEEK_OR_DATE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_PRESSURE', 1, 'Huyết áp', 'PAIRED_POINT', true, true, 'mmHg',
        '["mmHg"]'::jsonb, 0,
        '{"required":["systolic","diastolic"]}'::jsonb,
        '{"method":"NONE"}'::jsonb,
        '{"type":"DUAL_LINE","series":["systolic","diastolic"]}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_GLUCOSE', 1, 'Đường huyết', 'POINT', true, false, 'mg/dL',
        '["mg/dL", "mmol/L"]'::jsonb, 2,
        '{"required":["measurementContext"],"measurementContextValues":["FASTING","PRE_MEAL","POST_MEAL_1H","POST_MEAL_2H","RANDOM","OTHER_APPROVED"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementContext"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'FETAL_MOVEMENT_SESSION', 1, 'Cử động thai', 'SESSION', true, false, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","protocolCode","completionStatus","gestationalAgeSnapshot"]}'::jsonb,
        '{"method":"SESSION_HISTORY"}'::jsonb,
        '{"type":"SESSION_TIMELINE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PREGNANCY"]'::jsonb
    ),
    (
        'MATERNAL_HEART_RATE', 1, 'Nhịp tim mẹ', 'POINT', true, true, 'bpm',
        '["bpm"]'::jsonb, 0,
        '{"required":["measurementState"],"measurementStateValues":["RESTING","ACTIVE","POST_EXERCISE","UNKNOWN"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT","partitionBy":"measurementState"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementState"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SLEEP_SESSION', 1, 'Giấc ngủ', 'INTERVAL_AGGREGATE', false, true, 'min',
        '["min", "h"]'::jsonb, 2,
        '{"required":["periodStart","periodEnd","timeZone","sleepType"]}'::jsonb,
        '{"method":"MERGE_PROVIDER_INTERVALS","requiresCompleteness":true}'::jsonb,
        '{"type":"INTERVAL_TIMELINE","showDataGaps":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'STEPS', 1, 'Số bước', 'INTERVAL_AGGREGATE', false, true, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","timeZone","aggregationLevel"]}'::jsonb,
        '{"method":"DAILY_TOTAL_AFTER_DEDUPLICATION","requiresCompleteness":true}'::jsonb,
        '{"type":"DAILY_BAR","showPartialDay":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SPO2', 1, 'SpO2', 'POINT', false, true, '%',
        '["%"]'::jsonb, 2,
        '{"required":[]}'::jsonb,
        '{"method":"QUALITY_FILTERED_SERIES"}'::jsonb,
        '{"type":"LINE","showDataGaps":true,"showQuality":true}'::jsonb,
        '{"requiredForDevice":true,"allowedLabels":["VALID","LOW_QUALITY","INCOMPLETE","UNKNOWN","REJECTED"]}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'TEMPERATURE', 1, 'Nhiệt độ', 'POINT', true, true, 'Cel',
        '["Cel", "°C", "°F"]'::jsonb, 2,
        '{"required":["measurementSite"]}'::jsonb,
        '{"method":"SAME_METHOD_TIME_SERIES"}'::jsonb,
        '{"type":"METHOD_SERIES","partitionBy":"measurementSite"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    );
