package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class ChecklistTemplateTypeEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ChecklistTemplateRepository templateRepository;
    @Autowired ChecklistItemRepository itemRepository;

    @Test
    @Transactional
    void optionalPrePregnancyApprovalIsLibraryVisibleButNotAutoDistributable() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_item_templates
                    (template_id, entry_type, title, stage, is_active, version,
                     template_status, content_status, template_lineage_id, template_version_id,
                     substage_id, migration_review_required, distribution_enabled, template_type,
                     created_at, updated_at)
                values (?, 'TEMPLATE_ROOT', 'Optional pre-pregnancy', 'PRE_PREGNANCY', true, 1,
                        'ACTIVE', 'PENDING_REVIEW', ?, ?,
                        (select substage_id from checklist_substages where code='PRE_PREGNANCY_ALL'),
                        false, false, 'OPTIONAL', now(), now())
                """, templateId, templateId, versionId);
        jdbcTemplate.update("""
                insert into checklist_template_recipient_roles
                    (template_version_id, recipient_role, created_at)
                values (?, 'MOTHER', now())
                """, versionId);
        jdbcTemplate.update("""
                insert into care_item_templates
                    (template_id, parent_template_id, entry_type, title, display_order,
                     is_active, version, template_status, content_status, target_subject,
                     is_required, created_at, updated_at)
                values (?, ?, 'CHECKLIST_ENTRY', 'Take folic acid', 1,
                        true, 1, 'ACTIVE', 'DRAFT', 'MOTHER', true, now(), now())
                """, itemId, templateId);

        int approved = jdbcTemplate.update("""
                update care_item_templates
                   set content_status='APPROVED', distribution_enabled=false,
                       approved_at=now(), approved_by=?
                 where template_id=?
                """, actorId, templateId);

        assertThat(approved).isOne();
        assertThat(templateRepository.findAllOptionalByStageAndStatus(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED))
                .extracting(template -> template.getId()).contains(templateId);
        assertThat(templateRepository.findAllDistributionEnabledByStageAndStatus(
                ContentStage.PRE_PREGNANCY, ChecklistTemplateStatus.APPROVED))
                .extracting(template -> template.getId()).doesNotContain(templateId);
        assertThat(itemRepository.findAllByApprovedTemplateIds(
                java.util.Set.of(templateId), ChecklistTemplateStatus.APPROVED))
                .extracting(item -> item.getId()).containsExactly(itemId);
    }

    @Test
    @Transactional
    void reviewedImportedOptionalTemplateCanActivateWithoutAutoDistribution() {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_item_templates
                    (template_id, entry_type, title, stage, is_active, version,
                     template_status, content_status, template_lineage_id, template_version_id,
                     substage_id, migration_review_required,
                     distribution_enabled, template_type, created_at, updated_at)
                values (?, 'TEMPLATE_ROOT', 'Reviewed optional pre-pregnancy', 'PRE_PREGNANCY', true, 1,
                        'ACTIVE', 'PENDING_REVIEW', ?, ?,
                        (select substage_id from checklist_substages where code='PRE_PREGNANCY_ALL'),
                        false, false, 'OPTIONAL', now(), now())
                """, templateId, templateId, versionId);
        jdbcTemplate.update("""
                insert into checklist_template_recipient_roles
                    (template_version_id, recipient_role, created_at)
                values (?, 'MOTHER', now())
                """, versionId);
        jdbcTemplate.update("""
                update care_item_templates
                   set migration_reviewed_at=now(), migration_reviewed_by=?
                 where template_id=?
                """, reviewerId, templateId);

        int activated = jdbcTemplate.update("""
                update care_item_templates
                   set content_status='APPROVED', distribution_enabled=false,
                       approved_at=now(), approved_by=?
                 where template_id=?
                """, reviewerId, templateId);

        assertThat(activated).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select distribution_enabled from care_item_templates where template_id=?",
                Boolean.class, templateId)).isFalse();
    }
}
