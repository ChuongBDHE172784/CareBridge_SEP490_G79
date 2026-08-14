-- Pregnancy WHO checklist content import (source-of-truth: detailed Markdown only).
--
-- This migration deliberately reuses care_item_templates.  It creates two
-- immutable, targetless V2 roots per Plan: one COMMON window and one WEEKLY
-- recurring recommendation.  All rows remain Draft/non-distributing until
-- the source/copy attestation and clinical-copy review gate is completed.
-- template_type = MANDATORY only selects system-template distribution; it does
-- not make the recommendation clinically required. V2 leaves keep
-- target_subject remains NULL; requiredness is explicit for every V2 leaf.
-- The eight "/ngày" recommendations remain children of WEEKLY roots; no
-- pregnancy DAILY root is created.

SET LOCAL search_path = public, extensions, pg_catalog;

-- P1's original cadence pair check omitted WEEKLY + ONCE_PER_WINDOW even
-- though that is the canonical Plan-wide COMMON cadence.  Replace the
-- already-applied constraint in a forward migration rather than editing its
-- Flyway source.
ALTER TABLE public.care_item_templates
    DROP CONSTRAINT IF EXISTS checklist_template_cadence_shape_ck,
    DROP CONSTRAINT IF EXISTS care_item_templates_target_ck;

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT checklist_template_cadence_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR entry_type <> 'TEMPLATE_ROOT'
        OR COALESCE((schedule_type IS NULL AND materialization_policy IS NULL)
             OR (schedule_type = 'LEGACY'
                 AND materialization_policy = 'LEGACY_WINDOW')
             OR (schedule_type = 'SET'
                 AND materialization_policy = 'SEQUENCE_STEP')
             OR (schedule_type = 'WEEKLY'
                 AND materialization_policy IN ('ONCE_PER_WINDOW', 'EACH_WEEK'))
             OR (schedule_type = 'DAILY'
                 AND materialization_policy = 'EACH_DAY'), false)
    ) NOT VALID;

-- Cadence closure/catch-up uses the existing History marker on the parent;
-- widen its controlled reason vocabulary without adding a ledger table.
ALTER TABLE public.checklist_instances
    DROP CONSTRAINT IF EXISTS checklist_instances_history_reason_ck;
ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_history_reason_ck CHECK (
        history_reason_code IS NULL
        OR (length(history_reason_code) <= 80
            AND (history_reason_code LIKE 'LIFECYCLE_STAGE_OBSOLETE%'
                 OR history_reason_code = 'SEQUENCE_STEP_COMPLETED'
                 OR history_reason_code IN (
                     'CADENCE_PERIOD_CLOSED', 'CADENCE_SCOPE_EXIT',
                     'DATING_CORRECTED', 'ACCESS_REVOKED')))
    ) NOT VALID;

WITH root_data(
    root_id, plan_no, section, root_title, root_description,
    policy, end_mode, start_week, end_week, leaf_count
) AS (
    VALUES
        ('f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON',
         'WHO Plan 1 — Khám và sàng lọc ban đầu',
         'Khuyến nghị chung cho 20 tuần đầu; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 0, 19, 5),
        ('f8100000-0000-0000-0000-000000000102'::uuid, 1, 'WEEKLY',
         'WHO Plan 1 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong 20 tuần đầu.',
         'EACH_WEEK', 'FIXED_OFFSET', 0, 19, 3),
        ('f8100000-0000-0000-0000-000000000201'::uuid, 2, 'COMMON',
         'WHO Plan 2 — Sàng lọc giai đoạn giữa thai kỳ',
         'Khuyến nghị chung cho tuần 21 đến 25; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 20, 24, 2),
        ('f8100000-0000-0000-0000-000000000202'::uuid, 2, 'WEEKLY',
         'WHO Plan 2 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 21 đến 25.',
         'EACH_WEEK', 'FIXED_OFFSET', 20, 24, 4),
        ('f8100000-0000-0000-0000-000000000301'::uuid, 3, 'COMMON',
         'WHO Plan 3 — Sàng lọc đái tháo đường và Rh',
         'Khuyến nghị chung cho tuần 26 đến 29; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 25, 28, 4),
        ('f8100000-0000-0000-0000-000000000302'::uuid, 3, 'WEEKLY',
         'WHO Plan 3 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 26 đến 29.',
         'EACH_WEEK', 'FIXED_OFFSET', 25, 28, 4),
        ('f8100000-0000-0000-0000-000000000401'::uuid, 4, 'COMMON',
         'WHO Plan 4 — Chuẩn bị kế hoạch sinh và sau sinh',
         'Khuyến nghị chung cho tuần 30 đến 33; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 29, 32, 4),
        ('f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY',
         'WHO Plan 4 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 30 đến 33.',
         'EACH_WEEK', 'FIXED_OFFSET', 29, 32, 5),
        ('f8100000-0000-0000-0000-000000000501'::uuid, 5, 'COMMON',
         'WHO Plan 5 — Theo dõi sức khỏe giai đoạn 3 tháng cuối',
         'Khuyến nghị chung cho tuần 34 đến 35; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 33, 34, 1),
        ('f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY',
         'WHO Plan 5 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 34 đến 35.',
         'EACH_WEEK', 'FIXED_OFFSET', 33, 34, 5),
        ('f8100000-0000-0000-0000-000000000601'::uuid, 6, 'COMMON',
         'WHO Plan 6 — Sàng lọc GBS và chuẩn bị nuôi con bằng sữa mẹ',
         'Khuyến nghị chung cho tuần 36 đến 37; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 35, 36, 3),
        ('f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY',
         'WHO Plan 6 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 36 đến 37.',
         'EACH_WEEK', 'FIXED_OFFSET', 35, 36, 5),
        ('f8100000-0000-0000-0000-000000000701'::uuid, 7, 'COMMON',
         'WHO Plan 7 — Sẵn sàng cho chuyển dạ và sinh nở',
         'Khuyến nghị chung cho tuần 38 đến 39; nội dung mang tính tham khảo.',
         'ONCE_PER_WINDOW', 'FIXED_OFFSET', 37, 38, 4),
        ('f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY',
         'WHO Plan 7 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần trong tuần 38 đến 39.',
         'EACH_WEEK', 'FIXED_OFFSET', 37, 38, 5),
        ('f8100000-0000-0000-0000-000000000801'::uuid, 8, 'COMMON',
         'WHO Plan 8 — Rà soát cuối cùng và theo dõi quá ngày',
         'Khuyến nghị từ tuần 40 đến khi sinh; cửa sổ kết thúc khi thai kỳ kết thúc.',
         'ONCE_PER_WINDOW', 'STAGE_EXIT', 39, 2147483647, 3),
        ('f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY',
         'WHO Plan 8 — Theo dõi định kỳ hàng tuần',
         'Khuyến nghị duy trì hành vi theo dõi hàng tuần từ tuần 40 đến khi sinh.',
         'EACH_WEEK', 'STAGE_EXIT', 39, 2147483647, 5)
), root_payload AS (
    SELECT r.*, jsonb_build_object(
        'schema', 'CHECKLIST_METADATA_V1',
        'sourceArtifactPath', '08_References/Checklist giai đoạn đang mai thai.md',
        'sourceArtifactSha256', 'D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133',
        'importBatchId', 'PREGNANCY_WHO_20260812_V1',
        'importCorrelationId', '8a2dfd42-8ce0-5de6-9d30-0fe3e50210c5',
        'normalizerId', 'PREGNANCY_CHECKLIST_NORMALIZER_V1',
        'copyReviewPolicy', 'PREGNANCY_COPY_REVIEW_POLICY_V1',
        'provenanceStatus', 'PENDING_CLINICAL_COPY_SIGN_OFF',
        'cadenceReviewStatus', 'PENDING',
        'cadenceReviewerUserId', NULL,
        'cadenceReviewedAt', NULL,
        'reviewAuthorityId', NULL,
        'copyReviewerUserId', NULL,
        'qualificationEvidenceRef', NULL,
        'credentialVerifiedAt', NULL,
        'contentOwnerUserId', NULL,
        'contentOwnerApprovedAt', NULL,
        'copyReviewedAt', NULL,
        'sourceTitle', 'Checklist Giai Đoạn Đang Mang Thai (WHO)',
        'sourceRelationship', 'PRODUCT_AUTHORED_WHO_INFORMED',
        'sourceOrganization', 'World Health Organization',
        'sourceVersionOrPublicationDate', NULL,
        'sourceUrl', NULL,
        'sourceLanguage', 'vi',
        'renderedLanguage', 'vi',
        'translationProvenance', 'NOT_A_VERBATIM_TRANSLATION',
        'priorityNarrative', CASE WHEN r.plan_no = 1
                                  THEN 'Ưu tiên trong 12 tuần đầu' ELSE NULL END,
        'priorityNarrativeMode', CASE WHEN r.plan_no = 1
                                      THEN 'DISPLAY_ONLY' ELSE NULL END,
        'sourceLocator', concat('Plan ', r.plan_no),
        'plan', r.plan_no,
        'section', r.section,
        'leafCount', r.leaf_count,
        'scheduleGroupKey', concat('PREGNANCY_WHO_PLAN_', lpad(r.plan_no::text, 2, '0')),
        'recipientScope', 'MOTHER',
        'validityMode', 'NON_EXPIRING',
        'renderedManifestSchema', 'CHECKLIST_COPY_MANIFEST_V1',
        'renderedManifestCanonicalization', 'ASCII_LOCATOR_ORDERED_JSON_V1',
        'renderedManifestHash', 'c2eb657b7477ce6f0be8debfa556154ac7fe461b1da082eb48328811a4e5aa86'
    ) AS metadata
    FROM root_data r
)
INSERT INTO public.care_item_templates (
    template_id, parent_template_id, entry_type, title, description,
    display_order, stage, is_active, version, effective_from, effective_to,
    configuration_jsonb, created_at, updated_at, template_lineage_id,
    template_version_id, target_subject, migration_review_required,
    distribution_enabled, approved_at, approved_by, template_type,
    content_status, recipient_scope, eligibility_anchor_type,
    eligibility_range_unit, eligibility_start_inclusive, eligibility_end_inclusive,
    schedule_type, materialization_policy, schedule_group_key,
    schedule_context_type, schedule_end_mode, week_boundary_rule,
    checklist_contract_version, checklist_metadata_jsonb, checklist_metadata_hash
)
SELECT
    root_id, NULL, 'TEMPLATE_ROOT', root_title, root_description,
    0, 'PREGNANCY', true, 1, timestamptz '2026-08-12 00:00:00+07', NULL,
    '{}'::jsonb, timestamptz '2026-08-12 00:00:00+07',
    timestamptz '2026-08-12 00:00:00+07', root_id, root_id, NULL, true,
    false, NULL, NULL, 'MANDATORY', 'DRAFT', 'MOTHER', 'LMP', 'WEEK',
    start_week, end_week, 'WEEKLY', policy,
    concat('PREGNANCY_WHO_PLAN_', lpad(plan_no::text, 2, '0')), 'JOURNEY',
    end_mode, CASE WHEN policy = 'EACH_WEEK' THEN 'ANCHOR_RELATIVE_7D' ELSE 'NONE' END,
    2, metadata, encode(sha256(convert_to(metadata::text, 'UTF8')), 'hex')
FROM root_payload
ON CONFLICT (template_id) DO NOTHING;

WITH item_data(item_id, root_id, plan_no, section, display_order, leaf_locator,
               ancestor_path, item_text) AS (
    VALUES
        ('f8200000-0000-0000-0000-000000000101'::uuid, 'f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON', 1, 'P01/COMMON/01', NULL, 'Đi khám lần đầu.'),
        ('f8200000-0000-0000-0000-000000000102'::uuid, 'f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON', 2, 'P01/COMMON/02', NULL, 'Xét nghiệm haemoglobin để phát hiện thiếu máu.'),
        ('f8200000-0000-0000-0000-000000000103'::uuid, 'f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON', 3, 'P01/COMMON/03', NULL, 'Xác định nhóm máu và tình trạng Rh.'),
        ('f8200000-0000-0000-0000-000000000104'::uuid, 'f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON', 4, 'P01/COMMON/04', NULL, 'Sàng lọc HIV, giang mai, viêm gan B.'),
        ('f8200000-0000-0000-0000-000000000105'::uuid, 'f8100000-0000-0000-0000-000000000101'::uuid, 1, 'COMMON', 5, 'P01/COMMON/05', NULL, 'Sàng lọc dị tật bẩm sinh (nếu có).'),
        ('f8200000-0000-0000-0000-000000000111'::uuid, 'f8100000-0000-0000-0000-000000000102'::uuid, 1, 'WEEKLY', 1, 'P01/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000112'::uuid, 'f8100000-0000-0000-0000-000000000102'::uuid, 1, 'WEEKLY', 2, 'P01/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000113'::uuid, 'f8100000-0000-0000-0000-000000000102'::uuid, 1, 'WEEKLY', 3, 'P01/WEEKLY/03', NULL, 'Bổ sung Axit Folic 400mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000201'::uuid, 'f8100000-0000-0000-0000-000000000201'::uuid, 2, 'COMMON', 1, 'P02/COMMON/01', NULL, 'Thực hiện hoặc xác nhận đã thực hiện siêu âm trước tuần 24.'),
        ('f8200000-0000-0000-0000-000000000202'::uuid, 'f8100000-0000-0000-0000-000000000201'::uuid, 2, 'COMMON', 2, 'P02/COMMON/02', NULL, 'Hoàn thành các xét nghiệm hoặc sàng lọc còn thiếu từ lần đầu.'),
        ('f8200000-0000-0000-0000-000000000211'::uuid, 'f8100000-0000-0000-0000-000000000202'::uuid, 2, 'WEEKLY', 1, 'P02/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000212'::uuid, 'f8100000-0000-0000-0000-000000000202'::uuid, 2, 'WEEKLY', 2, 'P02/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000213'::uuid, 'f8100000-0000-0000-0000-000000000202'::uuid, 2, 'WEEKLY', 3, 'P02/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000214'::uuid, 'f8100000-0000-0000-0000-000000000202'::uuid, 2, 'WEEKLY', 4, 'P02/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000301'::uuid, 'f8100000-0000-0000-0000-000000000301'::uuid, 3, 'COMMON', 1, 'P03/COMMON/01', NULL, 'Xét nghiệm đường huyết để sàng lọc đái tháo đường thai kỳ.'),
        ('f8200000-0000-0000-0000-000000000302'::uuid, 'f8100000-0000-0000-0000-000000000301'::uuid, 3, 'COMMON', 2, 'P03/COMMON/02', NULL, 'Kiểm tra kết quả nhóm máu và tình trạng Rh.'),
        ('f8200000-0000-0000-0000-000000000303'::uuid, 'f8100000-0000-0000-0000-000000000301'::uuid, 3, 'COMMON', 3, 'P03/COMMON/RH_NEGATIVE/01', 'Nếu mẹ có Rh âm', 'Nếu mẹ có Rh âm: Lên lịch tiêm globulin miễn dịch Anti-D vào khoảng tuần 28.'),
        ('f8200000-0000-0000-0000-000000000304'::uuid, 'f8100000-0000-0000-0000-000000000301'::uuid, 3, 'COMMON', 4, 'P03/COMMON/RH_NEGATIVE/02', 'Nếu mẹ có Rh âm', 'Nếu mẹ có Rh âm: Theo dõi và thực hiện các hướng dẫn tiếp theo của cơ sở y tế.'),
        ('f8200000-0000-0000-0000-000000000311'::uuid, 'f8100000-0000-0000-0000-000000000302'::uuid, 3, 'WEEKLY', 1, 'P03/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000312'::uuid, 'f8100000-0000-0000-0000-000000000302'::uuid, 3, 'WEEKLY', 2, 'P03/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000313'::uuid, 'f8100000-0000-0000-0000-000000000302'::uuid, 3, 'WEEKLY', 3, 'P03/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000314'::uuid, 'f8100000-0000-0000-0000-000000000302'::uuid, 3, 'WEEKLY', 4, 'P03/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000401'::uuid, 'f8100000-0000-0000-0000-000000000401'::uuid, 4, 'COMMON', 1, 'P04/COMMON/01', NULL, 'Bắt đầu tư vấn chi tiết về kế hoạch sinh.'),
        ('f8200000-0000-0000-0000-000000000402'::uuid, 'f8100000-0000-0000-0000-000000000401'::uuid, 4, 'COMMON', 2, 'P04/COMMON/02', NULL, 'Xác định cơ sở dự kiến sinh.'),
        ('f8200000-0000-0000-0000-000000000403'::uuid, 'f8100000-0000-0000-0000-000000000401'::uuid, 4, 'COMMON', 3, 'P04/COMMON/03', NULL, 'Lên kế hoạch xử trí khi có tình huống khẩn cấp.'),
        ('f8200000-0000-0000-0000-000000000404'::uuid, 'f8100000-0000-0000-0000-000000000401'::uuid, 4, 'COMMON', 4, 'P04/COMMON/04', NULL, 'Bắt đầu tư vấn kế hoạch hóa gia đình sau sinh.'),
        ('f8200000-0000-0000-0000-000000000411'::uuid, 'f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY', 1, 'P04/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000412'::uuid, 'f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY', 2, 'P04/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000413'::uuid, 'f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY', 3, 'P04/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000414'::uuid, 'f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY', 4, 'P04/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000415'::uuid, 'f8100000-0000-0000-0000-000000000402'::uuid, 4, 'WEEKLY', 5, 'P04/WEEKLY/05', NULL, 'Theo dõi cử động của thai nhi.'),
        ('f8200000-0000-0000-0000-000000000501'::uuid, 'f8100000-0000-0000-0000-000000000501'::uuid, 5, 'COMMON', 1, 'P05/COMMON/01', NULL, 'Khám thai và theo dõi sức khỏe theo chỉ định của bác sĩ.'),
        ('f8200000-0000-0000-0000-000000000511'::uuid, 'f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY', 1, 'P05/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000512'::uuid, 'f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY', 2, 'P05/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000513'::uuid, 'f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY', 3, 'P05/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000514'::uuid, 'f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY', 4, 'P05/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000515'::uuid, 'f8100000-0000-0000-0000-000000000502'::uuid, 5, 'WEEKLY', 5, 'P05/WEEKLY/05', NULL, 'Theo dõi cử động của thai nhi.'),
        ('f8200000-0000-0000-0000-000000000601'::uuid, 'f8100000-0000-0000-0000-000000000601'::uuid, 6, 'COMMON', 1, 'P06/COMMON/01', NULL, 'Sàng lọc liên cầu khuẩn nhóm B — GBS (nếu áp dụng tại cơ sở y tế).'),
        ('f8200000-0000-0000-0000-000000000602'::uuid, 'f8100000-0000-0000-0000-000000000601'::uuid, 6, 'COMMON', 2, 'P06/COMMON/02', NULL, 'Ghi nhận kết quả GBS và kế hoạch xử trí khi sinh nếu kết quả dương tính.'),
        ('f8200000-0000-0000-0000-000000000603'::uuid, 'f8100000-0000-0000-0000-000000000601'::uuid, 6, 'COMMON', 3, 'P06/COMMON/03', NULL, 'Bắt đầu tư vấn nuôi con bằng sữa mẹ.'),
        ('f8200000-0000-0000-0000-000000000611'::uuid, 'f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY', 1, 'P06/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000612'::uuid, 'f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY', 2, 'P06/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000613'::uuid, 'f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY', 3, 'P06/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000614'::uuid, 'f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY', 4, 'P06/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000615'::uuid, 'f8100000-0000-0000-0000-000000000602'::uuid, 6, 'WEEKLY', 5, 'P06/WEEKLY/05', NULL, 'Theo dõi cử động của thai nhi.'),
        ('f8200000-0000-0000-0000-000000000701'::uuid, 'f8100000-0000-0000-0000-000000000701'::uuid, 7, 'COMMON', 1, 'P07/COMMON/01', NULL, 'Xác nhận cơ sở dự kiến sinh.'),
        ('f8200000-0000-0000-0000-000000000702'::uuid, 'f8100000-0000-0000-0000-000000000701'::uuid, 7, 'COMMON', 2, 'P07/COMMON/02', NULL, 'Xác nhận phương tiện di chuyển.'),
        ('f8200000-0000-0000-0000-000000000703'::uuid, 'f8100000-0000-0000-0000-000000000701'::uuid, 7, 'COMMON', 3, 'P07/COMMON/03', NULL, 'Xác nhận người hỗ trợ khi chuyển dạ.'),
        ('f8200000-0000-0000-0000-000000000704'::uuid, 'f8100000-0000-0000-0000-000000000701'::uuid, 7, 'COMMON', 4, 'P07/COMMON/04', NULL, 'Tìm hiểu các dấu hiệu cần đến cơ sở y tế ngay.'),
        ('f8200000-0000-0000-0000-000000000711'::uuid, 'f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY', 1, 'P07/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000712'::uuid, 'f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY', 2, 'P07/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000713'::uuid, 'f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY', 3, 'P07/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000714'::uuid, 'f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY', 4, 'P07/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000715'::uuid, 'f8100000-0000-0000-0000-000000000702'::uuid, 7, 'WEEKLY', 5, 'P07/WEEKLY/05', NULL, 'Theo dõi cử động của thai nhi.'),
        ('f8200000-0000-0000-0000-000000000801'::uuid, 'f8100000-0000-0000-0000-000000000801'::uuid, 8, 'COMMON', 1, 'P08/COMMON/01', NULL, 'Rà soát lần cuối kế hoạch sinh.'),
        ('f8200000-0000-0000-0000-000000000802'::uuid, 'f8100000-0000-0000-0000-000000000801'::uuid, 8, 'COMMON', 2, 'P08/COMMON/02', NULL, 'Rà soát phương án đi lại và người hỗ trợ khi chuyển dạ.'),
        ('f8200000-0000-0000-0000-000000000803'::uuid, 'f8100000-0000-0000-0000-000000000801'::uuid, 8, 'COMMON', 3, 'P08/COMMON/03', NULL, 'Trao đổi với nhân viên y tế về kế hoạch theo dõi tiếp theo nếu chưa sinh (quá ngày dự sinh).'),
        ('f8200000-0000-0000-0000-000000000811'::uuid, 'f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY', 1, 'P08/WEEKLY/01', NULL, 'Đo huyết áp.'),
        ('f8200000-0000-0000-0000-000000000812'::uuid, 'f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY', 2, 'P08/WEEKLY/02', NULL, 'Đo cân nặng và cập nhật BMI.'),
        ('f8200000-0000-0000-0000-000000000813'::uuid, 'f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY', 3, 'P08/WEEKLY/03', NULL, 'Kiểm tra protein niệu để sàng lọc tiền sản giật.'),
        ('f8200000-0000-0000-0000-000000000814'::uuid, 'f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY', 4, 'P08/WEEKLY/04', NULL, 'Bổ sung Axit Folic 600mcg/ngày.'),
        ('f8200000-0000-0000-0000-000000000815'::uuid, 'f8100000-0000-0000-0000-000000000802'::uuid, 8, 'WEEKLY', 5, 'P08/WEEKLY/05', NULL, 'Theo dõi cử động của thai nhi.')
), item_payload AS (
    SELECT i.*, jsonb_build_object(
        'schema', 'CHECKLIST_METADATA_V1',
        'sourceArtifactPath', '08_References/Checklist giai đoạn đang mai thai.md',
        'sourceArtifactSha256', 'D68EDC9F3D2D595876F8B1F9D3332E6FCFA55986535B52D8AE01BD25FAAFE133',
        'importBatchId', 'PREGNANCY_WHO_20260812_V1',
        'importCorrelationId', '8a2dfd42-8ce0-5de6-9d30-0fe3e50210c5',
        'normalizerId', 'PREGNANCY_CHECKLIST_NORMALIZER_V1',
        'plan', i.plan_no,
        'section', i.section,
        'leafLocator', i.leaf_locator,
        'ancestorContextPath', i.ancestor_path,
        -- The source hash covers the exact normalized Markdown leaf line,
        -- including its list/checkbox syntax and indentation.  Conditional
        -- descendants hash their source child text (without the rendered
        -- ancestor prefix), while the stored title retains that context.
        'originalLeafSha256', encode(sha256(convert_to(
            CASE WHEN i.ancestor_path IS NULL
                 THEN '  - [ ] ' || i.item_text
                 ELSE '    - [ ] ' || substring(
                         i.item_text FROM char_length(i.ancestor_path) + 3)
            END, 'UTF8')), 'hex'),
        'renderedTextSha256', encode(sha256(convert_to(i.item_text, 'UTF8')), 'hex'),
        'sourceSliceEncoding', 'UTF8_NFC_LF_NO_EOL',
        'cadenceReviewStatus', 'PENDING',
        'cadenceReviewerUserId', NULL,
        'cadenceReviewedAt', NULL,
        'reviewAuthorityId', NULL,
        'copyReviewerUserId', NULL,
        'qualificationEvidenceRef', NULL,
        'credentialVerifiedAt', NULL,
        'contentOwnerUserId', NULL,
        'contentOwnerApprovedAt', NULL,
        'copyReviewedAt', NULL,
        'sourceTitle', 'Checklist Giai Đoạn Đang Mang Thai (WHO)',
        'sourceRelationship', 'PRODUCT_AUTHORED_WHO_INFORMED',
        'sourceOrganization', 'World Health Organization',
        'sourceVersionOrPublicationDate', NULL,
        'sourceUrl', NULL,
        'sourceLanguage', 'vi',
        'renderedLanguage', 'vi',
        'translationProvenance', 'NOT_A_VERBATIM_TRANSLATION',
        'sourceLocator', concat('Plan ', i.plan_no),
        'renderedManifestHash', 'c2eb657b7477ce6f0be8debfa556154ac7fe461b1da082eb48328811a4e5aa86',
        'provenanceStatus', 'PENDING_CLINICAL_COPY_SIGN_OFF',
        'validityMode', 'NON_EXPIRING'
    ) AS metadata
    FROM item_data i
)
INSERT INTO public.care_item_templates (
    template_id, parent_template_id, entry_type, title, description,
    display_order, stage, is_active, version, configuration_jsonb,
    created_at, updated_at, target_subject, is_required, checklist_contract_version,
    checklist_metadata_jsonb, checklist_metadata_hash
)
SELECT
    item_id, root_id, 'CHECKLIST_ENTRY', item_text, NULL, display_order,
    'PREGNANCY', true, 1, '{}'::jsonb,
    timestamptz '2026-08-12 00:00:00+07', timestamptz '2026-08-12 00:00:00+07',
    NULL, TRUE, 2, metadata,
    encode(sha256(convert_to(metadata::text, 'UTF8')), 'hex')
FROM item_payload
ON CONFLICT (template_id) DO NOTHING;

-- Flush deferred inline-template checks emitted by the root/item inserts
-- before PostgreSQL validates the replacement table constraint.
SET CONSTRAINTS ALL IMMEDIATE;

ALTER TABLE public.care_item_templates
    VALIDATE CONSTRAINT checklist_template_cadence_shape_ck;
ALTER TABLE public.checklist_instances
    VALIDATE CONSTRAINT checklist_instances_history_reason_ck;

DO $$
DECLARE
    root_count integer;
    item_count integer;
    daily_count integer;
BEGIN
    SELECT count(*) INTO root_count
      FROM public.care_item_templates
     WHERE template_id::text LIKE 'f8100000-%'
       AND entry_type = 'TEMPLATE_ROOT';
    SELECT count(*) INTO item_count
      FROM public.care_item_templates
     WHERE template_id::text LIKE 'f8200000-%'
       AND entry_type = 'CHECKLIST_ENTRY';
    SELECT count(*) INTO daily_count
      FROM public.care_item_templates
     WHERE template_id::text LIKE 'f8100000-%'
       AND entry_type = 'TEMPLATE_ROOT'
       AND schedule_type = 'DAILY';
    IF root_count <> 16 OR item_count <> 62 OR daily_count <> 0 THEN
        RAISE EXCEPTION 'PREGNANCY_WHO_CHECKLIST_SEED_SHAPE_FAILED roots=% items=% daily=%',
            root_count, item_count, daily_count;
    END IF;
END $$;
