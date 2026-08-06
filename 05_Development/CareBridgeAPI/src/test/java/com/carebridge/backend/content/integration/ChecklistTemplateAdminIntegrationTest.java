package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// UC-243 (CB-CONTENT-IMP-011-TS §4) — CHKTPL-TC-INT-001..004, full stack against real Testcontainers
// PostgreSQL. TC-INT-004 specifically guards UC-50 (importFromTemplate) is never broken by archive.
@Transactional
class ChecklistTemplateAdminIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ChecklistTemplateRepository checklistTemplateRepository;
    @Autowired private ChecklistItemRepository checklistItemRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private MotherJourneyRepository motherJourneyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    private static final String BASE_URL = "/api/v1/admin/checklist-templates";

    // Inline eligibility is mandatory for every anchored stage: the admin API derives the
    // template's window from this block and rejects the request without it.
    private static final String PREGNANCY_SUBSTAGE = """
            {"code":"TRIMESTER_1","anchor":"LMP","unit":"WEEK","startInclusive":0,"endInclusive":13}""";

    @Test
    void authenticatedListing_returnsOnlyApprovedTemplatesWithAndWithoutStage() throws Exception {
        String token = seedUser("chk.list@test.com", Role.MOTHER);
        seedTemplateWithItem("Public approved pregnancy", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.APPROVED, "Visible pregnancy item");
        seedTemplateWithItem("Public approved postpartum", ContentStage.POSTPARTUM,
                ChecklistTemplateStatus.APPROVED, "Visible postpartum item");
        seedTemplateWithItem("Hidden draft", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.DRAFT, "Hidden draft item");
        seedTemplateWithItem("Hidden pending", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.PENDING_REVIEW, "Hidden pending item");
        seedTemplateWithItem("Hidden archived", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.ARCHIVED, "Hidden archived item");

        mockMvc.perform(get("/api/v1/content/checklists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", containsInAnyOrder(
                        "Public approved pregnancy", "Public approved postpartum")));

        mockMvc.perform(get("/api/v1/content/checklists")
                        .param("stage", "PREGNANCY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Public approved pregnancy"))
                .andExpect(jsonPath("$.data[0].items[0].itemText").value("Visible pregnancy item"));
    }

    // CHKTPL-TC-INT-001
    @Test
    void create_endToEnd_writesTemplateAndItems() throws Exception {
        String token = seedUser("chk.create@test.com", Role.CONTENT_ADMIN);

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Checklist khám thai tháng 3","description":"Mô tả",
                                 "stage":"PREGNANCY","recipientRoles":["MOTHER"],
                                 "substage":%s,
                                 "items":[{"itemText":"Siêu âm đo độ mờ da gáy","order":1,"isRequired":true,
                                           "targetSubject":"MOTHER"},
                                          {"itemText":"Xét nghiệm Double test","order":2,"isRequired":true,
                                           "targetSubject":"MOTHER"}]}
                                """.formatted(PREGNANCY_SUBSTAGE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        List<ChecklistTemplate> templates = checklistTemplateRepository.findAll().stream()
                .filter(t -> "Checklist khám thai tháng 3".equals(t.getName())).toList();
        assertThat(templates).hasSize(1);
        ChecklistTemplate saved = templates.get(0);
        assertThat(saved.getStatus()).isEqualTo(ChecklistTemplateStatus.DRAFT);
        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(saved.getId());
        assertThat(items).hasSize(2);
    }

    // CHKTPL-TC-INT-002 — regression guard for the immutable-list bug (Logic Issue L3)
    @Test
    void update_replaceItems_doesNotThrowAndPersistsNewItems() throws Exception {
        String token = seedUser("chk.update@test.com", Role.CONTENT_ADMIN);
        ChecklistTemplate template = checklistTemplateRepository.saveAndFlush(draftTemplate("Update target"));
        checklistItemRepository.saveAndFlush(item(template, "Mục cũ 1", 1));
        checklistItemRepository.saveAndFlush(item(template, "Mục cũ 2", 2));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(BASE_URL + "/{id}", template.getId()).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Update target","description":"Mô tả mới","stage":"PREGNANCY","status":"DRAFT",
                                 "recipientRoles":["MOTHER"],"substage":%s,
                                 "items":[{"itemText":"Mục mới 1","order":1,"isRequired":true,
                                           "targetSubject":"MOTHER"},
                                          {"itemText":"Mục mới 2","order":2,"isRequired":false,
                                           "targetSubject":"MOTHER"},
                                          {"itemText":"Mục mới 3","order":3,"isRequired":true,
                                           "targetSubject":"MOTHER"}]}
                                """.formatted(PREGNANCY_SUBSTAGE)))
                .andExpect(status().isOk());

        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
        assertThat(items).hasSize(3);
        assertThat(items).extracting(ChecklistItem::getItemText)
                .containsExactly("Mục mới 1", "Mục mới 2", "Mục mới 3");
    }

    @Test
    void update_preservesReferencedEntryIdAndSoftDeactivatesOmittedEntry() throws Exception {
        String email = "chk.identity@test.com";
        String token = seedUser(email, Role.CONTENT_ADMIN);
        User admin = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        ChecklistTemplate template = checklistTemplateRepository.saveAndFlush(
                draftTemplate("Identity-preserving update"));
        ChecklistItem retained = checklistItemRepository.saveAndFlush(
                item(template, "Retained entry", 1));
        ChecklistItem omitted = checklistItemRepository.saveAndFlush(
                item(template, "Omitted entry", 2));
        // The legacy imported-item assertion used preparation_checklist_items, retired at
        // R12. Its v2 counterpart is a distributed task whose template_item_version_id
        // points at the retained entry — the same downstream reference, one table over.
        UUID distributedTask = distributeSystemTemplateTask(admin, template, retained);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(BASE_URL + "/{id}", template.getId()).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Identity-preserving update","description":"Updated",
                                 "stage":"PREGNANCY","status":"DRAFT","recipientRoles":["MOTHER"],
                                 "substage":%s,
                                 "items":[{"id":"%s","itemText":"Retained entry updated","order":1,
                                           "isRequired":true,"targetSubject":"MOTHER"},
                                          {"itemText":"New entry","order":2,"isRequired":false,
                                           "targetSubject":"MOTHER"}]}
                                """.formatted(PREGNANCY_SUBSTAGE, retained.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(retained.getId().toString()));

        ChecklistItem retainedAfter = checklistItemRepository.findById(retained.getId()).orElseThrow();
        ChecklistItem omittedAfter = checklistItemRepository.findById(omitted.getId()).orElseThrow();
        assertThat(retainedAfter.getItemText()).isEqualTo("Retained entry updated");
        assertThat(retainedAfter.getIsActive()).isTrue();
        assertThat(omittedAfter.getIsActive()).isFalse();
        assertThat(checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId())).hasSize(2);
        assertThat(checklistItemRepository.findAllByTemplateIdOrderByOrder(template.getId())).hasSize(3);
        assertThat(referencedItemVersionOf(distributedTask)).isEqualTo(retained.getId());
        // The reference is only meaningful while the entry still resolves back to the
        // root that owns the task's version; a task can keep its column values and
        // still be orphaned.
        assertThat(taskTemplateLinkResolves(distributedTask)).isTrue();
    }

    // CHKTPL-TC-INT-003
    @Test
    void archive_keepsChecklistItemsRowCountUnchanged() throws Exception {
        String token = seedUser("chk.archive@test.com", Role.CONTENT_ADMIN);
        ChecklistTemplate template = checklistTemplateRepository.saveAndFlush(draftTemplate("Archive target"));
        checklistItemRepository.saveAndFlush(item(template, "Mục 1", 1));
        checklistItemRepository.saveAndFlush(item(template, "Mục 2", 2));

        mockMvc.perform(post(BASE_URL + "/{id}/archive", template.getId()).with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nội dung lỗi thời\"}"))
                .andExpect(status().isOk());

        ChecklistTemplate persisted = checklistTemplateRepository.findById(template.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ChecklistTemplateStatus.ARCHIVED);
        assertThat(checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId())).hasSize(2);
    }

    // CHKTPL-TC-INT-004 — archive must not break checklist tasks already distributed
    // from the template. Pre-R12 this was asserted against preparation_checklist_items;
    // the canonical downstream record is now checklist_task_instances.
    @Test
    void archive_doesNotBreakDownstreamDistributedTasks() throws Exception {
        String contentAdminToken = seedUser("chk.downstream.admin@test.com", Role.CONTENT_ADMIN);
        seedUser("chk.downstream.mother@test.com", Role.MOTHER);
        User mother = userRepository.findByEmailIgnoreCase("chk.downstream.mother@test.com").orElseThrow();

        ChecklistTemplate template = seedTemplateWithItem("Import source", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.APPROVED, "Mục nhập khẩu");
        ChecklistItem templateItem =
                checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId()).get(0);

        // The mother already holds a task distributed from this template item.
        UUID distributedTask = distributeSystemTemplateTask(mother, template, templateItem);
        assertThat(referencedItemVersionOf(distributedTask)).isEqualTo(templateItem.getId());

        // CONTENT_ADMIN archives the template afterwards
        mockMvc.perform(post(BASE_URL + "/{id}/archive", template.getId()).with(csrf())
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nội dung lỗi thời\"}"))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        // Archiving withdraws the template from distribution; it must not retract or
        // repoint work already handed to a user, nor delete the item version that work
        // references.
        assertThat(checklistTemplateRepository.findById(template.getId()).orElseThrow().getStatus())
                .isEqualTo(ChecklistTemplateStatus.ARCHIVED);
        assertThat(referencedItemVersionOf(distributedTask)).isEqualTo(templateItem.getId());
        assertThat(taskStatusOf(distributedTask)).isEqualTo("PENDING");
        assertThat(checklistItemRepository.findById(templateItem.getId())).isPresent();
        assertThat(taskTemplateLinkResolves(distributedTask)).isTrue();
    }

    private ChecklistTemplate draftTemplate(String name) {
        return template(name, ContentStage.PREGNANCY, ChecklistTemplateStatus.DRAFT);
    }

    /**
     * Seeds a template that already carries one entry.
     *
     * <p>Entries can only be attached while the root is still mutable —
     * {@code checklist_guard_approved_item_mutation} freezes a template's entries the
     * moment it reaches APPROVED or ARCHIVED — so the status is applied last, exactly
     * as the authoring flow does it.
     */
    private ChecklistTemplate seedTemplateWithItem(
            String name, ContentStage stage, ChecklistTemplateStatus status, String itemText) {
        ChecklistTemplate template = checklistTemplateRepository.saveAndFlush(
                template(name, stage, ChecklistTemplateStatus.DRAFT));
        checklistItemRepository.saveAndFlush(item(template, itemText, 1));
        if (status == ChecklistTemplateStatus.DRAFT) {
            return template;
        }
        template.setStatus(status);
        if (status == ChecklistTemplateStatus.APPROVED) {
            // care_item_templates_approved_gate_ck: an APPROVED root must carry its
            // approval provenance.
            template.setApprovedAt(Instant.now());
            template.setApprovedBy(UUID.randomUUID());
        }
        return checklistTemplateRepository.saveAndFlush(template);
    }

    /**
     * Inline template metadata is the sole template authority since the support-table
     * retirement, and the schema enforces its whole shape: a TEMPLATE_ROOT must carry a
     * lineage/version pair ({@code care_item_templates_root_version_ck}), a recipient
     * scope, and eligibility bounds matching its stage
     * ({@code checklist_validate_inline_template_shape}). The admin API never supplies
     * any of that, so fixtures must.
     */
    private ChecklistTemplate template(String name, ContentStage stage, ChecklistTemplateStatus status) {
        return ChecklistTemplate.builder()
                .name(name)
                .stage(stage)
                .status(status)
                .description("Mô tả kiểm thử")
                // The public /content/checklists listing only surfaces OPTIONAL
                // templates — the self-assignable ones.
                .templateType(ChecklistTemplateType.OPTIONAL)
                .templateLineageId(UUID.randomUUID())
                .templateVersionId(UUID.randomUUID())
                .recipientScope(ChecklistRecipientScope.MOTHER)
                // PRE_PREGNANCY is the one stage the validator pins to a zero-width
                // NONE/DAY window; the anchored stages take a real range.
                .eligibilityAnchorType(stage == ContentStage.PRE_PREGNANCY
                        ? ChecklistAnchorType.NONE
                        : stage == ContentStage.PREGNANCY
                                ? ChecklistAnchorType.LMP
                                : ChecklistAnchorType.DELIVERY_DATE)
                .eligibilityRangeUnit(stage == ContentStage.PRE_PREGNANCY
                        ? ChecklistRangeUnit.DAY : ChecklistRangeUnit.WEEK)
                .eligibilityStartInclusive(0)
                .eligibilityEndInclusive(stage == ContentStage.PRE_PREGNANCY ? 0 : 42)
                .build();
    }

    /**
     * Seeds the v2 counterpart of a legacy imported checklist row: a SYSTEM_TEMPLATE
     * checklist instance for {@code recipient} holding one task whose
     * {@code template_item_version_id} points at {@code item}.
     *
     * <p>A MOTHER-scoped instance carries no care group — the schema rejects one — but
     * it does need a journey the recipient owns, which
     * {@code checklist_instances_journey_owner_fk} enforces.
     *
     * @return the distributed task id
     */
    private UUID distributeSystemTemplateTask(
            User recipient, ChecklistTemplate template, ChecklistItem item) {
        UUID careSubjectId = createMotherCareSubject(recipient);
        MotherJourney journey = motherJourneyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(recipient.getId())
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build());

        UUID instanceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO checklist_instances (
                    checklist_instance_id, distribution_key, key_version, template_lineage_id,
                    template_version_id, recipient_user_id, recipient_role, care_group_id,
                    care_context_type, care_context_id, context_owner_user_id, origin,
                    status, lock_version, created_at, updated_at)
                VALUES (?, ?, 'v1', ?, ?, ?, 'MOTHER', NULL, 'JOURNEY', ?, ?, 'SYSTEM_TEMPLATE',
                        'PENDING', 0, now(), now())
                """,
                instanceId,
                ChecklistDistributionKeyFactory.instanceKey(
                        template.getTemplateVersionId(), recipient.getId(), "MOTHER",
                        null, "JOURNEY", journey.getId(), null, null),
                template.getTemplateLineageId(), template.getTemplateVersionId(),
                recipient.getId(), journey.getId(), recipient.getId());
        jdbcTemplate.update("""
                INSERT INTO checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, template_version_id,
                    template_item_version_id, task_key, key_version, title_snapshot,
                    display_order, is_required, target_subject, status, lock_version,
                    created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'v1', ?, 1, true, 'MOTHER', 'PENDING', 0, now(), now())
                """,
                taskId, instanceId, template.getTemplateVersionId(), item.getId(),
                ChecklistDistributionKeyFactory.childKey(instanceId, item.getId()),
                item.getItemText());
        return taskId;
    }

    private UUID referencedItemVersionOf(UUID taskId) {
        return jdbcTemplate.queryForObject("""
                SELECT template_item_version_id FROM checklist_task_instances
                 WHERE checklist_task_instance_id = ?
                """, UUID.class, taskId);
    }

    private String taskStatusOf(UUID taskId) {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM checklist_task_instances WHERE checklist_task_instance_id = ?
                """, String.class, taskId);
    }

    /**
     * Re-derives what {@code checklist_validate_task_template} demands of a system task:
     * the referenced entry must still hang off the root that owns the referenced version.
     * A task can keep its column values and still be orphaned if that link breaks.
     */
    private boolean taskTemplateLinkResolves(UUID taskId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT exists(
                    SELECT 1 FROM checklist_task_instances task
                      JOIN care_item_templates item
                        ON item.template_id = task.template_item_version_id
                       AND item.entry_type = 'CHECKLIST_ENTRY'
                      JOIN care_item_templates root
                        ON root.template_id = item.parent_template_id
                       AND root.entry_type = 'TEMPLATE_ROOT'
                       AND root.template_version_id = task.template_version_id
                     WHERE task.checklist_task_instance_id = ?)
                """, Boolean.class, taskId));
    }

    private ChecklistItem item(ChecklistTemplate template, String text, int order) {
        return ChecklistItem.builder()
                .template(template)
                .itemText(text)
                .order(order)
                .isRequired(true)
                .build();
    }

    private String seedUser(String email, Role role) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, person_id, email, role, password_hash, enabled, locked,
                    email_verified, phone_verified, account_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, false, true, false, 'ACTIVE', now(), now())
                """, userId, userId, email, role.name(), passwordEncoder.encode("SecureP@ss1"));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return jwtTokenProvider.generateAccessToken(user);
    }

    private UUID createMotherCareSubject(User owner) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'MOTHER', u.display_name,
                       'ACTIVE', now(), now()
                  FROM users u
                 WHERE u.user_id = ?
                """, careSubjectId, owner.getId());
        return careSubjectId;
    }
}
