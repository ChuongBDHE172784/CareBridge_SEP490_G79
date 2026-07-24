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
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
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
    @Autowired private UserChecklistItemRepository userChecklistItemRepository;
    @Autowired private MotherJourneyRepository motherJourneyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    private static final String BASE_URL = "/api/v1/admin/checklist-templates";

    @Test
    void authenticatedListing_returnsOnlyApprovedTemplatesWithAndWithoutStage() throws Exception {
        String token = seedUser("chk.list@test.com", Role.MOTHER);
        ChecklistTemplate approvedPregnancy = checklistTemplateRepository.saveAndFlush(
                template("Public approved pregnancy", ContentStage.PREGNANCY, ContentStatus.APPROVED));
        ChecklistTemplate approvedPostpartum = checklistTemplateRepository.saveAndFlush(
                template("Public approved postpartum", ContentStage.POSTPARTUM, ContentStatus.APPROVED));
        ChecklistTemplate draft = checklistTemplateRepository.saveAndFlush(
                template("Hidden draft", ContentStage.PREGNANCY, ContentStatus.DRAFT));
        ChecklistTemplate pending = checklistTemplateRepository.saveAndFlush(
                template("Hidden pending", ContentStage.PREGNANCY, ContentStatus.PENDING_REVIEW));
        ChecklistTemplate archived = checklistTemplateRepository.saveAndFlush(
                template("Hidden archived", ContentStage.PREGNANCY, ContentStatus.ARCHIVED));

        checklistItemRepository.saveAllAndFlush(List.of(
                item(approvedPregnancy, "Visible pregnancy item", 1),
                item(approvedPostpartum, "Visible postpartum item", 1),
                item(draft, "Hidden draft item", 1),
                item(pending, "Hidden pending item", 1),
                item(archived, "Hidden archived item", 1)));

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
                                 "stage":"PREGNANCY",
                                 "items":[{"itemText":"Siêu âm đo độ mờ da gáy","order":1,"isRequired":true},
                                          {"itemText":"Xét nghiệm Double test","order":2,"isRequired":true}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        List<ChecklistTemplate> templates = checklistTemplateRepository.findAll().stream()
                .filter(t -> "Checklist khám thai tháng 3".equals(t.getName())).toList();
        assertThat(templates).hasSize(1);
        ChecklistTemplate saved = templates.get(0);
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
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
                                 "items":[{"itemText":"Mục mới 1","order":1,"isRequired":true},
                                          {"itemText":"Mục mới 2","order":2,"isRequired":false},
                                          {"itemText":"Mục mới 3","order":3,"isRequired":true}]}
                                """))
                .andExpect(status().isOk());

        List<ChecklistItem> items = checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId());
        assertThat(items).hasSize(3);
        assertThat(items).extracting(ChecklistItem::getItemText)
                .containsExactly("Mục mới 1", "Mục mới 2", "Mục mới 3");
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
        assertThat(persisted.getStatus()).isEqualTo(ContentStatus.ARCHIVED);
        assertThat(checklistItemRepository.findByTemplate_IdOrderByOrder(template.getId())).hasSize(2);
    }

    // CHKTPL-TC-INT-004 — archive must not break canonical preparation checklist items.
    @Test
    void archive_doesNotBreakDownstreamUserChecklistItems() throws Exception {
        String contentAdminToken = seedUser("chk.downstream.admin@test.com", Role.CONTENT_ADMIN);
        String motherToken = seedUser("chk.downstream.mother@test.com", Role.MOTHER);
        User mother = userRepository.findByEmailIgnoreCase("chk.downstream.mother@test.com").orElseThrow();
        UUID careSubjectId = createMotherCareSubject(mother);
        MotherJourney journey = motherJourneyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(mother.getId())
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build());

        ChecklistTemplate template = checklistTemplateRepository.saveAndFlush(
                template("Import source", ContentStage.PREGNANCY, ContentStatus.APPROVED));
        ChecklistItem templateItem = checklistItemRepository.saveAndFlush(item(template, "Mục nhập khẩu", 1));

        // MOTHER imports the template item into her personal checklist (real UC-50 endpoint)
        mockMvc.perform(post("/api/v1/user-checklist-items/import").with(csrf())
                        .header("Authorization", "Bearer " + motherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"journeyId\":\"" + journey.getId()
                                + "\",\"templateItemIds\":[\"" + templateItem.getId() + "\"]}"))
                .andExpect(status().isCreated());

        List<UserChecklistItem> imported = userChecklistItemRepository.findByOwnerFiltered(
                userRepository.findByEmailIgnoreCase("chk.downstream.mother@test.com").orElseThrow().getId(),
                null, null);
        assertThat(imported).hasSize(1);
        assertThat(imported.get(0).getTemplateItemId()).isEqualTo(templateItem.getId());

        // CONTENT_ADMIN archives the template afterwards
        mockMvc.perform(post(BASE_URL + "/{id}/archive", template.getId()).with(csrf())
                        .header("Authorization", "Bearer " + contentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nội dung lỗi thời\"}"))
                .andExpect(status().isOk());

        // The mother's already-imported row must remain intact, still pointing at the same item
        List<UserChecklistItem> afterArchive = userChecklistItemRepository.findByOwnerFiltered(
                userRepository.findByEmailIgnoreCase("chk.downstream.mother@test.com").orElseThrow().getId(),
                null, null);
        assertThat(afterArchive).hasSize(1);
        assertThat(afterArchive.get(0).getTemplateItemId()).isEqualTo(templateItem.getId());
        assertThat(checklistItemRepository.findById(templateItem.getId())).isPresent();
    }

    private ChecklistTemplate draftTemplate(String name) {
        return template(name, ContentStage.PREGNANCY, ContentStatus.DRAFT);
    }

    private ChecklistTemplate template(String name, ContentStage stage, ContentStatus status) {
        return ChecklistTemplate.builder()
                .name(name)
                .stage(stage)
                .status(status)
                .description("Mô tả kiểm thử")
                .build();
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
        User user = userRepository.save(User.builder()
                .email(email)
                .role(role)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        return jwtTokenProvider.generateAccessToken(user);
    }

    private UUID createMotherCareSubject(User owner) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'MOTHER', p.display_name,
                       'ACTIVE', now(), now()
                  FROM users u
                  JOIN persons p ON p.person_id = u.person_id
                 WHERE u.user_id = ?
                """, careSubjectId, owner.getId());
        return careSubjectId;
    }
}
