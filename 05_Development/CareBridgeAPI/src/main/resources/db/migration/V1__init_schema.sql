create table audit_logs (
    created_at timestamp(6)
    with
        time zone not null,
        actor_user_id uuid,
        audit_log_id uuid not null,
        entity_id uuid,
        ip_address varchar(80),
        entity_type varchar(100),
        new_value_json jsonb,
        old_value_json jsonb,
        action varchar(80) not null,
        primary key (audit_log_id)
);

create table baby_daily_logs (
    quantity numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        ended_at timestamp(6)
    with
        time zone,
        started_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        baby_log_id uuid not null,
        recorded_by uuid,
        unit varchar(20),
        log_type varchar(30),
        note TEXT,
        primary key (baby_log_id)
);

create table baby_profiles (
    birth_date date,
    birth_length_cm numeric(38, 2),
    birth_weight_kg numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid not null,
        owner_user_id uuid,
        related_journey_id uuid,
        sex varchar(20),
        status varchar(20) not null,
        nickname varchar(100),
        primary key (baby_id)
);

create table care_facilities (
    latitude numeric(38, 2),
    longitude numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        facility_id uuid not null,
        partner_id uuid,
        verification_status varchar(20),
        phone varchar(30),
        source_type varchar(30),
        facility_type varchar(40),
        address varchar(500),
        name varchar(255),
        opening_hours_json jsonb,
        primary key (facility_id)
);

create table care_group_members (
    created_at timestamp(6)
    with
        time zone not null,
        joined_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        care_group_id uuid,
        care_group_member_id uuid not null,
        user_id uuid,
        invitation_status varchar(20),
        member_role varchar(30),
        permission_json jsonb,
        primary key (care_group_member_id)
);

create table care_groups (
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        care_group_id uuid not null,
        journey_id uuid,
        owner_user_id uuid,
        status varchar(20) not null,
        group_name varchar(150),
        primary key (care_group_id)
);

create table care_tasks (
    completed_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        due_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        assigned_by uuid,
        assigned_to uuid,
        care_group_id uuid,
        care_task_id uuid not null,
        status varchar(20) not null,
        title varchar(200),
        description TEXT,
        primary key (care_task_id)
);

create table checklist_items (
    is_required boolean,
    item_order integer,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        checklist_item_id uuid not null,
        checklist_template_id uuid,
        item_text varchar(500),
        note varchar(500),
        primary key (checklist_item_id)
);

create table checklist_templates (
    version_no integer,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        checklist_template_id uuid not null,
        content_item_id uuid,
        status varchar(20) not null,
        stage varchar(30),
        name varchar(200),
        primary key (checklist_template_id)
);

create table commission_records (
    commission_amount numeric(38, 2),
    commission_rate numeric(38, 2),
    expert_net_amount numeric(38, 2),
    gateway_fee numeric(38, 2),
    original_price numeric(38, 2),
    refund_amount numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        eligible_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        commission_id uuid not null,
        expert_profile_id uuid,
        payment_id uuid,
        settlement_status varchar(20),
        primary key (commission_id)
);

create table community_answers (
    helpful_count integer,
    created_at timestamp(6)
    with
        time zone not null,
        published_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        answer_id uuid not null,
        author_user_id uuid,
        expert_profile_id uuid,
        question_id uuid,
        answer_type varchar(20),
        moderation_status varchar(20),
        content TEXT,
        primary key (answer_id)
);

create table community_profiles (
    is_visible boolean,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        community_profile_id uuid not null,
        user_id uuid,
        interest_stage varchar(30),
        display_name varchar(100),
        region varchar(120),
        bio varchar(500),
        public_avatar_url varchar(500),
        primary key (community_profile_id)
);

create table community_questions (
    is_anonymous boolean,
    created_at timestamp(6)
    with
        time zone not null,
        published_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        author_user_id uuid,
        question_id uuid not null,
        topic_id uuid,
        moderation_status varchar(20),
        urgency_level varchar(20),
        title varchar(250),
        content TEXT,
        primary key (question_id)
);

create table community_topics (
    is_active boolean,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        topic_id uuid not null,
        risk_level varchar(20),
        name varchar(150),
        slug varchar(160),
        description varchar(500),
        primary key (topic_id)
);

create table consent_grants (
    version integer not null,
    consent_given_at timestamp(6)
    with
        time zone not null,
        created_at timestamp(6)
    with
        time zone not null,
        expiry_at timestamp(6)
    with
        time zone not null,
        id bigint generated by default as identity,
        revoked_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone not null,
        revoked_by uuid,
        user_id uuid not null,
        recipient varchar(120),
        scope_text TEXT,
        data_type varchar(50) not null,
        purpose varchar(50) not null,
        primary key (id)
);

create table consultation_bookings (
    commission_rate_snapshot numeric(38, 2),
    duration_minutes integer,
    price_snapshot_amount numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        price_locked_at timestamp(6)
    with
        time zone,
        scheduled_end timestamp(6)
    with
        time zone,
        scheduled_start timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        availability_id uuid,
        booking_id uuid not null,
        expert_price_id uuid,
        expert_profile_id uuid,
        requester_user_id uuid,
        shared_summary_id uuid,
        channel_type varchar(20),
        status varchar(25) not null,
        topic varchar(200),
        cancellation_policy_snapshot TEXT,
        currency varchar(255),
        primary key (booking_id)
);

create table consultation_disputes (
    created_at timestamp(6)
    with
        time zone not null,
        resolved_at timestamp(6)
    with
        time zone,
        submitted_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        booking_id uuid,
        dispute_id uuid not null,
        resolved_by uuid,
        submitted_by uuid,
        status varchar(20) not null,
        resolution_type varchar(30),
        reason_code varchar(50),
        description TEXT,
        evidence_json jsonb,
        resolution_note TEXT,
        primary key (dispute_id)
);

create table consultation_messages (
    read_at timestamp(6)
    with
        time zone,
        sent_at timestamp(6)
    with
        time zone,
        message_id uuid not null,
        sender_user_id uuid,
        session_id uuid,
        message_type varchar(20),
        status varchar(20) not null,
        file_url varchar(500),
        message_body TEXT,
        primary key (message_id)
);

create table consultation_price_bands (
    commission_rate numeric(38, 2),
    duration_minutes integer,
    maximum_price numeric(38, 2),
    minimum_price numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        effective_from timestamp(6)
    with
        time zone,
        effective_to timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        configured_by uuid,
        price_band_id uuid not null,
        channel_type varchar(20),
        status varchar(20) not null,
        specialty_scope varchar(150),
        currency varchar(255),
        primary key (price_band_id)
);

create table consultation_sessions (
    created_at timestamp(6)
    with
        time zone not null,
        ended_at timestamp(6)
    with
        time zone,
        started_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        booking_id uuid,
        session_id uuid not null,
        session_status varchar(20),
        communication_room_id varchar(255),
        expert_summary TEXT,
        technical_log_json jsonb,
        primary key (session_id)
);

create table content_items (
    version_no integer,
    created_at timestamp(6)
    with
        time zone not null,
        published_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        author_user_id uuid,
        content_item_id uuid not null,
        topic_id uuid,
        status varchar(20) not null,
        content_type varchar(30),
        title varchar(250),
        body TEXT,
        source_label varchar(255),
        primary key (content_item_id)
);

create table content_reports (
    created_at timestamp(6)
    with
        time zone not null,
        resolved_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        assigned_moderator_id uuid,
        report_id uuid not null,
        reporter_user_id uuid,
        target_id uuid,
        status varchar(20) not null,
        target_type varchar(30),
        reason_code varchar(40),
        description TEXT,
        primary key (report_id)
);

create table contribution_points (
    points integer,
    recorded_at timestamp(6)
    with
        time zone,
        point_record_id uuid not null,
        source_id uuid,
        user_id uuid,
        source_type varchar(40),
        reason varchar(255),
        primary key (point_record_id)
);

create table data_permissions (
    created_at timestamp(6)
    with
        time zone not null,
        expires_at timestamp(6)
    with
        time zone,
        granted_at timestamp(6)
    with
        time zone,
        revoked_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        grantee_user_id uuid,
        owner_user_id uuid,
        permission_id uuid not null,
        scope_reference_id uuid,
        status varchar(20) not null,
        scope_type varchar(50),
        purpose varchar(255),
        primary key (permission_id)
);

create table development_milestones (
    achieved_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        milestone_id uuid not null,
        recorded_by uuid,
        source_type varchar(30),
        milestone_type varchar(60),
        note TEXT,
        primary key (milestone_id)
);

create table device_measurements (
    value_numeric numeric(38, 2),
    value_secondary numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        measured_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        connection_id uuid,
        device_measurement_id uuid not null,
        quality_label varchar(30),
        unit varchar(30),
        measurement_type varchar(40),
        source_record_id varchar(150),
        raw_metadata_json jsonb,
        primary key (device_measurement_id)
);

create table emergency_events (
    closed_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        opened_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        risk_level varchar(10),
        emergency_event_id uuid not null,
        selected_expert_id uuid,
        selected_facility_id uuid,
        source_reference_id uuid,
        user_id uuid,
        status varchar(20) not null,
        action_type varchar(30),
        source_type varchar(30),
        primary key (emergency_event_id)
);

create table exercise_safety_checks (
    red_flag_detected boolean,
    completed_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        exercise_id uuid,
        journey_id uuid,
        safety_check_id uuid not null,
        user_id uuid,
        result_status varchar(20),
        answer_json jsonb,
        blocked_reason TEXT,
        primary key (safety_check_id)
);

create table exercise_sessions (
    completion_percent numeric(38, 2),
    paused_seconds integer,
    posture_score numeric(38, 2),
    warning_count integer,
    created_at timestamp(6)
    with
        time zone not null,
        ended_at timestamp(6)
    with
        time zone,
        started_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        exercise_id uuid,
        exercise_session_id uuid not null,
        journey_id uuid,
        safety_check_id uuid,
        user_id uuid,
        session_status varchar(20),
        summary_json jsonb,
        primary key (exercise_session_id)
);

create table expenses (
    amount numeric(38, 2),
    expense_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        expense_id uuid not null,
        journey_id uuid,
        owner_user_id uuid,
        category varchar(40),
        note varchar(500),
        currency varchar(255),
        primary key (expense_id)
);

create table expert_availability (
    created_at timestamp(6)
    with
        time zone not null,
        end_at timestamp(6)
    with
        time zone,
        start_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        availability_id uuid not null,
        expert_profile_id uuid,
        channel_type varchar(20),
        status varchar(20) not null,
        primary key (availability_id)
);

create table expert_consultation_prices (
    duration_minutes integer,
    price_amount numeric(38, 2),
    version_no integer,
    created_at timestamp(6)
    with
        time zone not null,
        effective_from timestamp(6)
    with
        time zone,
        effective_to timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        expert_price_id uuid not null,
        expert_profile_id uuid,
        price_band_id uuid,
        channel_type varchar(20),
        status varchar(20) not null,
        cancellation_policy TEXT,
        currency varchar(255),
        primary key (expert_price_id)
);

create table expert_credentials (
    expiry_date date,
    issued_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        credential_id uuid not null,
        expert_profile_id uuid,
        review_status varchar(20),
        credential_type varchar(50),
        credential_number varchar(120),
        file_url varchar(500),
        issuer varchar(255),
        review_note TEXT,
        primary key (credential_id)
);

create table expert_location_shares (
    accuracy_meters numeric(38, 2),
    latitude numeric(38, 2),
    longitude numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        expires_at timestamp(6)
    with
        time zone,
        shared_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        consent_reference uuid,
        expert_profile_id uuid,
        location_share_id uuid not null,
        availability_status varchar(20),
        primary key (location_share_id)
);

create table expert_profiles (
    experience_years integer,
    rating_avg numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        verified_at timestamp(6)
    with
        time zone,
        expert_profile_id uuid not null,
        user_id uuid,
        verified_by uuid,
        verification_status varchar(20),
        professional_title varchar(150),
        specialty varchar(150),
        consultation_scope TEXT,
        workplace varchar(255),
        primary key (expert_profile_id)
);

create table expert_reviews (
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        booking_id uuid,
        expert_profile_id uuid,
        review_id uuid not null,
        reviewer_user_id uuid,
        moderation_status varchar(20),
        comment TEXT,
        rating varchar(255),
        primary key (review_id)
);

create table growth_measurements (
    head_circumference_cm numeric(38, 2),
    height_cm numeric(38, 2),
    measured_date date,
    weight_kg numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        growth_measurement_id uuid not null,
        source_type varchar(30),
        note varchar(500),
        primary key (growth_measurement_id)
);

create table health_device_connections (
    consent_granted_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        last_synced_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        connection_id uuid not null,
        user_id uuid,
        status varchar(20) not null,
        provider_name varchar(80),
        device_name varchar(150),
        scopes_json jsonb,
        token_reference varchar(255),
        primary key (connection_id)
);

create table health_records (
    record_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        health_record_id uuid not null,
        journey_id uuid,
        owner_user_id uuid,
        status varchar(20) not null,
        source_type varchar(30),
        record_type varchar(50),
        title varchar(200),
        file_url varchar(500),
        source_name varchar(255),
        primary key (health_record_id)
);

create table health_summaries (
    created_at timestamp(6)
    with
        time zone not null,
        period_end timestamp(6)
    with
        time zone,
        period_start timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        journey_id uuid,
        owner_user_id uuid,
        summary_id uuid not null,
        generated_by varchar(20),
        status varchar(20) not null,
        summary_period varchar(30),
        summary_json jsonb,
        primary key (summary_id)
);

create table location_snapshots (
    accuracy_meters numeric(38, 2),
    latitude numeric(38, 2),
    longitude numeric(38, 2),
    captured_at timestamp(6)
    with
        time zone,
        expires_at timestamp(6)
    with
        time zone,
        context_id uuid,
        location_snapshot_id uuid not null,
        user_id uuid,
        consent_status varchar(20),
        context_type varchar(30),
        primary key (location_snapshot_id)
);

create table maternal_health_metrics (
    value_numeric numeric(38, 2),
    value_secondary numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        measured_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        journey_id uuid,
        metric_id uuid not null,
        source_reference_id uuid,
        source_type varchar(30),
        unit varchar(30),
        metric_type varchar(40),
        note varchar(500),
        primary key (metric_id)
);

create table moderation_actions (
    action_at timestamp(6)
    with
        time zone,
        expires_at timestamp(6)
    with
        time zone,
        moderation_action_id uuid not null,
        moderator_user_id uuid,
        report_id uuid,
        target_id uuid,
        action_type varchar(30),
        target_type varchar(30),
        reason TEXT,
        primary key (moderation_action_id)
);

create table mother_journeys (
    delivery_date date,
    estimated_due_date date,
    last_menstrual_date date,
    start_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        journey_id uuid not null,
        owner_user_id uuid,
        status varchar(20) not null,
        journey_type varchar(30),
        notes TEXT,
        primary key (journey_id)
);

create table notification_preferences (
    email_enabled boolean,
    in_app_enabled boolean,
    push_enabled boolean,
    quiet_hours_end time(0),
    quiet_hours_start time(0),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        preference_id uuid not null,
        user_id uuid,
        notification_type varchar(50),
        primary key (preference_id)
);

create table notifications (
    is_read boolean,
    created_at timestamp(6)
    with
        time zone not null,
        sent_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        notification_id uuid not null,
        recipient_user_id uuid,
        reference_id uuid,
        delivery_status varchar(20),
        notification_type varchar(50),
        reference_type varchar(50),
        title varchar(200),
        body TEXT,
        primary key (notification_id)
);

create table otp_verifications (
    attempts integer not null,
    verified boolean not null,
    created_at timestamp(6)
    with
        time zone not null,
        expires_at timestamp(6)
    with
        time zone not null,
        id bigint generated by default as identity,
        used_at timestamp(6)
    with
        time zone,
        user_id uuid,
        phone varchar(20),
        code_hash varchar(255) not null,
        email varchar(255),
        purpose varchar(50) not null,
        requested_role varchar(50),
        primary key (id)
);

create table partner_expert_links (
    end_date date,
    start_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        approved_by uuid,
        expert_profile_id uuid,
        partner_expert_link_id uuid not null,
        partner_id uuid,
        status varchar(20) not null,
        relationship_type varchar(40),
        primary key (partner_expert_link_id)
);

create table partner_organizations (
    latitude numeric(38, 2),
    longitude numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        partner_id uuid not null,
        representative_user_id uuid,
        verified_by uuid,
        verification_status varchar(20),
        partner_type varchar(30),
        license_number varchar(120),
        address varchar(500),
        name varchar(255),
        primary key (partner_id)
);

create table partner_services (
    price_from numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        partner_id uuid,
        service_id uuid not null,
        approval_status varchar(20),
        booking_url varchar(500),
        currency varchar(255),
        description TEXT,
        service_name varchar(255),
        primary key (service_id)
);

create table payment_transactions (
    gateway_fee numeric(38, 2),
    gross_amount numeric(38, 2),
    net_paid_amount numeric(38, 2),
    refund_amount numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        paid_at timestamp(6)
    with
        time zone,
        refunded_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        booking_id uuid,
        payer_user_id uuid,
        payment_id uuid not null,
        status varchar(20) not null,
        gateway_name varchar(50),
        gateway_transaction_id varchar(150),
        currency varchar(255),
        primary key (payment_id)
);

create table postpartum_logs (
    log_date date,
    sleep_hours numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        journey_id uuid,
        postpartum_log_id uuid not null,
        bleeding_level varchar(20),
        breastfeeding_note TEXT,
        mood_level varchar(255),
        pain_level varchar(255),
        symptom_note TEXT,
        primary key (postpartum_log_id)
);

create table posture_analysis_configs (
    confidence_threshold numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        effective_from timestamp(6)
    with
        time zone,
        effective_to timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        configured_by uuid,
        exercise_id uuid,
        posture_config_id uuid not null,
        analysis_mode varchar(20),
        feedback_level varchar(20),
        status varchar(20) not null,
        rule_or_model_version varchar(100),
        config_json jsonb,
        primary key (posture_config_id)
);

create table posture_feedback_events (
    confidence_score numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        event_time_ms bigint,
        exercise_session_id uuid,
        feedback_event_id uuid not null,
        posture_config_id uuid,
        severity varchar(20),
        posture_code varchar(80),
        feedback_text varchar(500),
        keypoint_summary_json jsonb,
        primary key (feedback_event_id)
);

create table pregnancy_exercises (
    duration_minutes integer,
    supports_posture_analysis boolean,
    version_no integer,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        created_by uuid,
        exercise_id uuid not null,
        difficulty_level varchar(20),
        status varchar(20) not null,
        trimester_scope varchar(30),
        title varchar(200),
        media_url varchar(500),
        description TEXT,
        instruction_content TEXT,
        safety_warning TEXT,
        primary key (exercise_id)
);

create table refresh_tokens (
    revoked boolean not null,
    created_at timestamp(6)
    with
        time zone not null,
        expires_at timestamp(6)
    with
        time zone not null,
        id bigint generated by default as identity,
        user_id uuid not null,
        token varchar(160) not null unique,
        primary key (id)
);

create table refund_records (
    refund_amount numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        processed_at timestamp(6)
    with
        time zone,
        requested_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        approved_by uuid,
        dispute_id uuid,
        payment_id uuid,
        refund_id uuid not null,
        status varchar(20) not null,
        gateway_refund_id varchar(150),
        currency varchar(255),
        reason varchar(255),
        primary key (refund_id)
);

create table reminders (
    created_at timestamp(6)
    with
        time zone not null,
        scheduled_at timestamp(6)
    with
        time zone,
        snoozed_until timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        journey_id uuid,
        owner_user_id uuid,
        reminder_id uuid not null,
        status varchar(20) not null,
        reminder_type varchar(40),
        title varchar(200),
        recurrence_rule varchar(255),
        primary key (reminder_id)
);

create table roles (
    is_active boolean,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        role_id uuid not null,
        role_code varchar(50) unique,
        role_name varchar(100),
        description varchar(500),
        primary key (role_id)
);

create table safety_alerts (
    acknowledged_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        sent_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        location_snapshot_id uuid,
        recipient_user_id uuid,
        safety_alert_id uuid not null,
        safety_event_id uuid,
        delivery_status varchar(20),
        alert_reason varchar(30),
        payload_json jsonb,
        primary key (safety_alert_id)
);

create table safety_events (
    angular_velocity numeric(38, 2),
    confidence_score numeric(38, 2),
    inactivity_seconds integer,
    peak_acceleration numeric(38, 2),
    created_at timestamp(6)
    with
        time zone not null,
        detected_at timestamp(6)
    with
        time zone,
        response_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        safety_event_id uuid not null,
        setting_id uuid,
        user_id uuid,
        status varchar(20) not null,
        event_type varchar(30),
        user_response varchar(30),
        false_positive_reason varchar(80),
        primary key (safety_event_id)
);

create table safety_monitoring_settings (
    countdown_seconds integer,
    is_enabled boolean,
    location_sharing_enabled boolean,
    created_at timestamp(6)
    with
        time zone not null,
        location_consent_at timestamp(6)
    with
        time zone,
        sensor_consent_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        emergency_contact_user_id uuid,
        setting_id uuid not null,
        user_id uuid,
        disclaimer_version varchar(50),
        monitoring_schedule_json jsonb,
        primary key (setting_id)
);

create table security_events (
    id bigint generated by default as identity,
    timestamp timestamp(6)
    with
        time zone not null,
        user_id uuid,
        ip_address varchar(80),
        details TEXT,
        event_type varchar(80) not null,
        primary key (id)
);

create table settlement_records (
    commission_amount numeric(38, 2),
    expert_net_amount numeric(38, 2),
    gateway_fee numeric(38, 2),
    gross_amount numeric(38, 2),
    refund_amount numeric(38, 2),
    settlement_period_end date,
    settlement_period_start date,
    created_at timestamp(6)
    with
        time zone not null,
        settled_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        commission_id uuid,
        expert_profile_id uuid,
        settlement_id uuid not null,
        status varchar(20) not null,
        reference_code varchar(100),
        primary key (settlement_id)
);

create table sponsored_campaigns (
    end_date date,
    start_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        campaign_id uuid not null,
        partner_id uuid,
        reviewed_by uuid,
        approval_status varchar(20),
        sponsor_label varchar(150),
        title varchar(250),
        description TEXT,
        primary key (campaign_id)
);

create table token_blacklist (
    expires_at timestamp(6)
    with
        time zone not null,
        revoked_at timestamp(6)
    with
        time zone not null,
        id uuid not null,
        reason varchar(100),
        token_hash varchar(255) not null unique,
        primary key (id)
);

create index idx_token_blacklist_expires_at on token_blacklist (expires_at);

create table triage_answers (
    answer_order integer,
    red_flag_triggered boolean,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        assessment_id uuid,
        triage_answer_id uuid not null,
        question_code varchar(80),
        question_text varchar(500),
        answer_value TEXT,
        primary key (triage_answer_id)
);

create table triage_assessments (
    disclaimer_accepted boolean,
    completed_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        risk_level varchar(10),
        assessment_id uuid not null,
        baby_id uuid,
        journey_id uuid,
        user_id uuid,
        rule_version varchar(50),
        recommended_action TEXT,
        symptom_summary TEXT,
        primary key (assessment_id)
);

create table user_roles (
    assigned_at timestamp(6)
    with
        time zone,
        created_at timestamp(6)
    with
        time zone not null,
        expires_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone,
        assigned_by uuid,
        role_id uuid,
        user_id uuid,
        user_role_id uuid not null,
        status varchar(20) not null,
        primary key (user_role_id)
);

create table user_sessions (
    session_id uuid not null,
    user_id uuid not null,
    refresh_token_hash varchar(255) not null,
    device_name varchar(150),
    ip_address varchar(64),
    browser varchar(150),
    location varchar(200),
    last_activity_at timestamp(6)
    with
        time zone,
        is_current boolean not null default false,
        expires_at timestamp(6)
    with
        time zone not null,
        revoked boolean not null default false,
        status varchar(20) not null,
        created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        primary key (session_id)
);

-- Enforce canonical status values
alter table if exists user_sessions
add constraint CHK_user_sessions_status check (
    status in (
        'active',
        'inactive',
        'expired',
        'revoked'
    )
);

-- Security indexes for user_sessions
create index idx_user_sessions_user_id on user_sessions (user_id);

create index idx_user_sessions_last_activity on user_sessions (last_activity_at);

create index idx_user_sessions_refresh_token_hash on user_sessions (refresh_token_hash);

create index idx_user_sessions_user_revoked on user_sessions (user_id, revoked)
where
    not revoked;

create unique index idx_user_sessions_refresh_token_hash_unique on user_sessions (refresh_token_hash)
where
    refresh_token_hash is not null;

create table users (
    email_verified boolean,
    enabled boolean not null,
    locked boolean not null,
    phone_verified boolean,
    created_at timestamp(6)
    with
        time zone not null,
        last_login_at timestamp(6)
    with
        time zone,
        locked_at timestamp(6)
    with
        time zone,
        updated_at timestamp(6)
    with
        time zone not null,
        user_id uuid not null,
        account_status varchar(30),
        phone varchar(30),
        full_name varchar(150),
        avatar_url varchar(500),
        email varchar(255) unique,
        password_hash varchar(255),
        role varchar(50) not null,
        primary key (user_id)
);

-- Unique constraint for non-null phone numbers
create unique index idx_users_phone_unique on users (phone)
where
    phone is not null;

-- Add FK from user_sessions to users (users table created after user_sessions)
alter table if exists user_sessions
add constraint FK_user_sessions_user_id foreign key (user_id) references users;

create table vaccination_records (
    administered_date date,
    scheduled_date date,
    created_at timestamp(6)
    with
        time zone not null,
        updated_at timestamp(6)
    with
        time zone,
        baby_id uuid,
        proof_record_id uuid,
        vaccination_record_id uuid not null,
        status varchar(20) not null,
        dose_number varchar(30),
        vaccine_name varchar(200),
        facility_name varchar(255),
        primary key (vaccination_record_id)
);

alter table if exists otp_verifications
add constraint FKssn2wgmijxelm0u26ti271lcf foreign key (user_id) references users;

alter table if exists refresh_tokens
add constraint FK1lih5y2npsf8u5o3vhdb9y0os foreign key (user_id) references users;