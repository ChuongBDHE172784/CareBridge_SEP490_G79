package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskServiceImpl;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** REQUEST-mode proof that one Today call synchronously creates and returns personal work. */
@EnabledOnOs(OS.WINDOWS)
@TestPropertySource(properties = {
        "carebridge.features.checklist-distribution-v2=true",
        "spring.task.scheduling.enabled=false"
})
class ChecklistRequestMaterializationEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 7, 31);
    private static final String ZONE = "Asia/Ho_Chi_Minh";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UnifiedTodayTaskServiceImpl todayTaskService;

    @Test
    void todayRequestMaterializesAndReturnsOnePersonalTaskWithoutLegacyProcessing() throws Exception {
        UUID motherId = UUID.randomUUID();
        UUID journeyId = seedWeekFourPregnancy(motherId);
        TemplateIds template = seedApprovedMandatoryMotherTemplate(motherId);

        assertThat(todayTaskService).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select to_regclass('public.checklist_care_group_contexts') is null",
                Boolean.class)).isTrue();

        assertThat(personalParentCount(motherId, template.versionId())).isZero();
        assertThat(personalTaskCount(motherId, template.versionId())).isZero();

        HttpTodayResponse firstHttp = today(motherId);
        TodayTasksResponse first = firstHttp.dto();
        assertStableTopLevelDto(firstHttp.json());

        TodayTaskItemResponse materialized = templateTasks(first, template.versionId()).getFirst();
        assertThat(materialized.taskKind()).isEqualTo(TaskKind.CHECKLIST);
        assertThat(materialized.instanceId()).isNotNull();
        assertThat(materialized.templateVersionId()).isEqualTo(template.versionId());
        assertThat(materialized.careGroupId()).isNull();
        assertThat(materialized.careContextType()).isEqualTo(ChecklistCareContextType.JOURNEY);
        assertThat(materialized.careContextId()).isEqualTo(journeyId);
        assertThat(materialized.title()).isEqualTo("REQUEST week-four task");
        assertThat(templateTaskNodes(firstHttp.json(), template.versionId()))
                .singleElement()
                .satisfies(task -> assertThat(task.path("careGroupId").isNull()).isTrue());
        assertThat(personalParentCount(motherId, template.versionId())).isOne();
        assertThat(personalTaskCount(motherId, template.versionId())).isOne();
        assertThat(materialized.taskId()).isEqualTo(jdbcTemplate.queryForObject("""
                select task.checklist_task_instance_id
                  from checklist_task_instances task
                  join checklist_instances parent
                    on parent.checklist_instance_id=task.checklist_instance_id
                 where parent.recipient_user_id=?
                   and parent.template_version_id=?
                   and parent.care_group_id is null
                """, UUID.class, motherId, template.versionId()));

        HttpTodayResponse secondHttp = today(motherId);
        TodayTasksResponse second = secondHttp.dto();
        assertStableTopLevelDto(secondHttp.json());
        assertThat(second.correlationId()).isNotEqualTo(first.correlationId());

        assertThat(templateTasks(second, template.versionId()))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.taskId()).isEqualTo(materialized.taskId());
                    assertThat(task.instanceId()).isEqualTo(materialized.instanceId());
                    assertThat(task.careGroupId()).isNull();
                });
        assertThat(personalParentCount(motherId, template.versionId())).isOne();
        assertThat(personalTaskCount(motherId, template.versionId())).isOne();
    }

    @Test
    void currentChecklistRequestMaterializesAndReturnsOnePersonalTaskIdempotently() throws Exception {
        UUID motherId = UUID.randomUUID();
        UUID journeyId = seedWeekFourPregnancy(motherId);
        TemplateIds template = seedApprovedMandatoryMotherTemplate(motherId);

        assertThat(personalParentCount(motherId, template.versionId())).isZero();
        assertThat(personalTaskCount(motherId, template.versionId())).isZero();

        JsonNode first = currentChecklist(motherId);
        assertStableTopLevelDto(first);
        List<JsonNode> firstTasks = templateTaskNodes(first, template.versionId());
        assertThat(firstTasks).hasSize(1);
        JsonNode materialized = firstTasks.getFirst();
        UUID taskId = UUID.fromString(materialized.path("taskId").asText());
        UUID instanceId = UUID.fromString(materialized.path("instanceId").asText());
        assertThat(materialized.path("templateVersionId").asText())
                .isEqualTo(template.versionId().toString());
        assertThat(materialized.path("careGroupId").isNull()).isTrue();
        assertThat(materialized.path("careContextType").asText()).isEqualTo("JOURNEY");
        assertThat(materialized.path("careContextId").asText()).isEqualTo(journeyId.toString());
        assertThat(materialized.path("title").asText()).isEqualTo("REQUEST week-four task");
        assertThat(personalParentCount(motherId, template.versionId())).isOne();
        assertThat(personalTaskCount(motherId, template.versionId())).isOne();

        JsonNode second = currentChecklist(motherId);
        assertStableTopLevelDto(second);
        assertThat(second.path("correlationId").asText())
                .isNotEqualTo(first.path("correlationId").asText());
        assertThat(templateTaskNodes(second, template.versionId()))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.path("taskId").asText()).isEqualTo(taskId.toString());
                    assertThat(task.path("instanceId").asText()).isEqualTo(instanceId.toString());
                    assertThat(task.path("careGroupId").isNull()).isTrue();
                });
        assertThat(personalParentCount(motherId, template.versionId())).isOne();
        assertThat(personalTaskCount(motherId, template.versionId())).isOne();
    }

    private UUID seedWeekFourPregnancy(UUID motherId) {
        CanonicalUserFixture.insertUser(
                jdbcTemplate,
                motherId,
                "REQUEST Mother",
                String.format("09%08d", Math.floorMod(motherId.hashCode(), 100_000_000)),
                "MOTHER");
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, user_row.person_id, user_row.user_id, 'MOTHER',
                       user_row.display_name, 'ACTIVE', now(), now()
                  from users user_row
                 where user_row.user_id=?
                """, careSubjectId, motherId);
        LocalDate lmp = EFFECTIVE_DATE.minusWeeks(4);
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(motherId)
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(lmp)
                .lastMenstrualDate(lmp)
                .estimatedDueDate(lmp.plusWeeks(40))
                .build());
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey.getId(), careSubjectId);
        return journey.getId();
    }

    private TemplateIds seedApprovedMandatoryMotherTemplate(UUID approvedBy) {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(ignored -> {
            jdbcTemplate.update("""
                    insert into care_item_templates (
                        template_id, entry_type, title, description, stage, is_active,
                        version, template_status, content_status, template_lineage_id,
                        template_version_id, migration_review_required,
                        distribution_enabled, template_type, recipient_scope,
                        eligibility_anchor_type, eligibility_range_unit,
                        eligibility_start_inclusive, eligibility_end_inclusive,
                        created_at, updated_at)
                    values (?, 'TEMPLATE_ROOT', 'REQUEST week-four template', 'request materialization fixture',
                        'PREGNANCY', true, 1, 'ACTIVE', 'DRAFT', ?, ?,
                        false, false, 'MANDATORY', 'MOTHER', 'LMP', 'WEEK', 0, 12,
                        now(), now())
                    """, templateId, templateId, versionId);
            jdbcTemplate.update("""
                    insert into care_item_templates (
                        template_id, parent_template_id, entry_type, title, display_order,
                        stage, is_active, version, template_status, content_status,
                        target_subject, is_required, due_anchor_type, due_offset_start,
                        due_offset_end, due_offset_unit, created_at, updated_at)
                    values (?, ?, 'CHECKLIST_ENTRY', 'REQUEST week-four task', 1,
                        'PREGNANCY', true, 1, 'ACTIVE', 'APPROVED',
                        'MOTHER', true, 'LMP', 28, 28, 'DAY', now(), now())
                    """, itemId, templateId);
            jdbcTemplate.update("""
                    update care_item_templates
                       set content_status='APPROVED', distribution_enabled=true,
                           approved_at=now(), approved_by=?
                     where template_id=?
                    """, approvedBy, templateId);
        });
        assertThat(jdbcTemplate.queryForMap("""
                select recipient_scope, eligibility_anchor_type, eligibility_range_unit,
                       eligibility_start_inclusive, eligibility_end_inclusive
                  from care_item_templates
                 where template_id=?
                """, templateId))
                .containsEntry("recipient_scope", "MOTHER")
                .containsEntry("eligibility_anchor_type", "LMP")
                .containsEntry("eligibility_range_unit", "WEEK")
                .containsEntry("eligibility_start_inclusive", 0)
                .containsEntry("eligibility_end_inclusive", 12);
        return new TemplateIds(versionId);
    }

    private List<TodayTaskItemResponse> templateTasks(TodayTasksResponse response, UUID versionId) {
        return Stream.of(
                        response.sections().overdue(),
                        response.sections().today(),
                        response.sections().upcoming(),
                        response.sections().unscheduled())
                .flatMap(List::stream)
                .filter(task -> versionId.equals(task.templateVersionId()))
                .toList();
    }

    private HttpTodayResponse today(UUID motherId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tasks/today")
                        .param("date", EFFECTIVE_DATE.toString())
                        .header("X-User-Timezone", ZONE)
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new HttpTodayResponse(json, objectMapper.treeToValue(json, TodayTasksResponse.class));
    }

    private JsonNode currentChecklist(UUID motherId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/checklists/current/tasks")
                        .param("date", EFFECTIVE_DATE.toString())
                        .header("X-User-Timezone", ZONE)
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static void assertStableTopLevelDto(JsonNode json) {
        Set<String> fields = new TreeSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder(
                "asOf", "zoneId", "horizonDays", "sections", "counts", "correlationId");
        assertThat(json.path("zoneId").asText()).isEqualTo(ZONE);
        assertThat(json.path("horizonDays").asInt()).isEqualTo(7);
        assertThat(UUID.fromString(json.path("correlationId").asText())).isNotNull();
    }

    private static List<JsonNode> templateTaskNodes(JsonNode response, UUID versionId) {
        List<JsonNode> tasks = new ArrayList<>();
        response.path("sections").fields().forEachRemaining(section ->
                section.getValue().forEach(task -> {
                    if (versionId.toString().equals(task.path("templateVersionId").asText())) {
                        tasks.add(task);
                    }
                }));
        return List.copyOf(tasks);
    }

    private long personalParentCount(UUID motherId, UUID versionId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                  from checklist_instances
                 where recipient_user_id=?
                   and template_version_id=?
                   and recipient_role='MOTHER'
                   and care_group_id is null
                """, Long.class, motherId, versionId);
    }

    private long personalTaskCount(UUID motherId, UUID versionId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                  from checklist_task_instances task
                  join checklist_instances parent
                    on parent.checklist_instance_id=task.checklist_instance_id
                 where parent.recipient_user_id=?
                   and parent.template_version_id=?
                   and parent.care_group_id is null
                """, Long.class, motherId, versionId);
    }

    private record TemplateIds(UUID versionId) {
    }

    private record HttpTodayResponse(JsonNode json, TodayTasksResponse dto) {
    }
}
