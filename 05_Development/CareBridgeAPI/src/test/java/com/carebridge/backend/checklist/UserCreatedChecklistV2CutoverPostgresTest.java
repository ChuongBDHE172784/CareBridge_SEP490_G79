package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** PostgreSQL proof for CHK-025/CHK-029 V2 cutover and canonical user-created tasks. */
class UserCreatedChecklistV2CutoverPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private CareGroupRepository careGroupRepository;

    private UUID motherId;
    private UUID journeyId;
    private UUID groupId;
    private long legacyRowsBefore;

    @BeforeEach
    void setUp() {
        motherId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, motherId, "V2 Mother", uniquePhone(), "MOTHER");
        UUID subjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', u.display_name, 'ACTIVE', now(), now()
                  from users u where u.user_id=?
                """, subjectId, motherId);
        journeyId = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(motherId).careSubjectId(subjectId).journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE).startDate(LocalDate.of(2026, 1, 1)).build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                journeyId, subjectId);
        groupId = careGroupRepository.saveAndFlush(CareGroup.builder().ownerUserId(motherId)
                .groupName("V2 group").status(CareGroupStatus.ACTIVE).linkedJourneyId(journeyId).build()).getId();
        legacyRowsBefore = countLegacyRows();
    }

    @Test
    void userCreatedTaskUsesV2AndTodayAndActionNeverWritesLegacyRows() throws Exception {
        UUID clientTaskId = UUID.randomUUID();
        String body = """
                {"journeyId":"%s","itemText":"Pack water","category":"GENERAL", "itemOrder":2,
                 "targetSubject":"MOTHER","clientTaskId":"%s"}
                """.formatted(journeyId, clientTaskId);

        String response = mockMvc.perform(post("/api/v1/user-checklist-items")
                        .with(csrf()).with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.origin").value("USER_CREATED"))
                .andExpect(jsonPath("$.data.targetSubject").value("MOTHER"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/user-checklist-items")
                        .with(csrf()).with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemId").value(
                        com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                                .readTree(response).path("data").path("itemId").asText()));

        UUID taskId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).path("data").path("itemId").asText());
        assertThat(jdbcTemplate.queryForObject("select count(*) from preparation_checklist_items",
                Long.class)).isEqualTo(legacyRowsBefore);
        assertThat(jdbcTemplate.queryForObject("select count(*) from checklist_instances "
                + "where recipient_user_id=? and origin='USER_CREATED' and care_group_id is null", Long.class,
                motherId)).isOne();
        assertThat(jdbcTemplate.queryForObject("select count(*) from checklist_task_instances "
                + "where checklist_task_instance_id=? and target_subject='MOTHER' "
                + "and category='GENERAL'", Long.class, taskId)).isOne();

        mockMvc.perform(get("/api/v1/user-checklist-items").param("journeyId", journeyId.toString())
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].itemId")
                        .value(org.hamcrest.Matchers.hasItem(taskId.toString())))
                .andExpect(jsonPath("$.data[?(@.itemId == '%s')].origin".formatted(taskId))
                        .value(org.hamcrest.Matchers.hasItem("USER_CREATED")));

        mockMvc.perform(get("/api/v1/tasks/today").param("date", "2026-07-30")
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sections.unscheduled[*].taskId")
                        .value(org.hamcrest.Matchers.hasItem(taskId.toString())));

        mockMvc.perform(post("/api/v1/tasks/CHECKLIST/%s/actions".formatted(taskId))
                        .with(csrf()).with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"COMPLETE\",\"clientRequestId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(jdbcTemplate.queryForObject("select status from checklist_task_instances where checklist_task_instance_id=?",
                String.class, taskId)).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from preparation_checklist_items",
                Long.class)).isEqualTo(legacyRowsBefore);
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_events "
                + "where actor_user_id=? and checklist_task_instance_id=? "
                + "and event_category='CHECKLIST_COMPLETED'", Long.class, motherId, taskId)).isOne();

        UUID skippedTaskId = createTask("Skip me", "DELIVERY", UUID.randomUUID());
        mockMvc.perform(post("/api/v1/tasks/CHECKLIST/%s/actions".formatted(skippedTaskId))
                        .with(csrf()).with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"SKIP\",\"clientRequestId\":\"%s\","
                                .formatted(UUID.randomUUID()) + "\"reason\":\"USER_CHOICE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SKIPPED"));
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_events "
                + "where actor_user_id=? and checklist_task_instance_id=? "
                + "and event_category='CHECKLIST_SKIPPED' and reason_code='USER_CHOICE'",
                Long.class, motherId, skippedTaskId)).isOne();
    }

    @Test
    void explicitTargetIsRequiredAndRetiredCompatibilityMutationsAreGone() throws Exception {
        String missingTarget = "{\"journeyId\":\"%s\",\"itemText\":\"No target\",\"clientTaskId\":\"%s\"}"
                .formatted(journeyId, UUID.randomUUID());
        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(missingTarget))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ITEM_TARGET_REQUIRED"));

        String id = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/user-checklist-items/import").with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"journeyId\":\"%s\",\"templateItemIds\":[\"%s\"]}".formatted(journeyId, id)))
                .andExpect(status().isGone());
        mockMvc.perform(patch("/api/v1/user-checklist-items/%s/toggle".formatted(id)).with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isGone());
        mockMvc.perform(put("/api/v1/user-checklist-items/%s".formatted(id)).with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itemText\":\"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/user-checklist-items/%s".formatted(id)).with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isNotFound());
        assertThat(countLegacyRows()).isEqualTo(legacyRowsBefore);
    }

    @Test
    void foreignContextAndClientKeyPayloadReuseAreRejectedWithoutCrossOwnerWrites() throws Exception {
        UUID clientTaskId = UUID.randomUUID();
        String body = """
                {"journeyId":"%s","itemText":"Stable task","targetSubject":"MOTHER",
                 "clientTaskId":"%s"}
                """.formatted(journeyId, clientTaskId);
        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Stable task", "Changed task")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSE"));

        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"itemText\":\"Stable task\"",
                                "\"itemText\":\"Stable task\",\"category\":\"DELIVERY\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSE"));

        UUID otherActor = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(otherActor.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CHECKLIST_CONTEXT_UNAVAILABLE"));
        assertThat(jdbcTemplate.queryForObject("select count(*) from checklist_instances "
                + "where recipient_user_id=?", Long.class, otherActor)).isZero();
        assertThat(countLegacyRows()).isEqualTo(legacyRowsBefore);
    }

    private long countLegacyRows() {
        Long count = jdbcTemplate.queryForObject("select count(*) from preparation_checklist_items", Long.class);
        return count == null ? 0 : count;
    }

    private UUID createTask(String text, String category, UUID clientTaskId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/user-checklist-items")
                        .with(csrf()).with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"journeyId":"%s","itemText":"%s","category":"%s",
                                 "targetSubject":"MOTHER","clientTaskId":"%s"}
                                """.formatted(journeyId, text, category, clientTaskId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).path("data").path("itemId").asText());
    }

    private String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }
}
