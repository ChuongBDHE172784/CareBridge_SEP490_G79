package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.carebridge.backend.checklist.key.ChecklistDistributionKeyFactory;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.family.dto.FamilyPermission;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/** Real API/PG evidence for neutral direct-ID authorization and the unified Today union. */
@EnabledOnOs(OS.WINDOWS)
class ChecklistAuthorizationAndTodayApiEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 7, 29);
    private static final String ZONE = "Asia/Ho_Chi_Minh";
    private static final Instant OVERDUE_AT = Instant.parse("2026-07-28T03:00:00Z");
    private static final Instant TODAY_AT = Instant.parse("2026-07-29T03:00:00Z");
    private static final Instant UPCOMING_AT = Instant.parse("2026-07-30T03:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CareGroupRepository careGroupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private MotherJourneyRepository journeyRepository;

    private UUID m1;
    private UUID m2;
    private UUID f1Ok;
    private UUID f2;
    private UUID fRevoked;
    private UUID fNoPermission;
    private UUID subject;
    private UUID journey;
    private UUID primaryGroup;
    private UUID otherGroup;
    private UUID authorizedBaby;
    private UUID authorizedBabyGroup;
    private UUID sharedReminderDefinition;
    private UUID foreignBaby;
    private UUID adminTemplateId;
    private UUID adminTemplateVersionId;
    private UUID adminTemplateItemId;
    private final List<UUID> checklistInstances = new ArrayList<>();
    private final List<UUID> checklistTasks = new ArrayList<>();
    private final List<UUID> careTaskRows = new ArrayList<>();
    private final List<UUID> reminderRows = new ArrayList<>();

    private TaskIds m1Tasks;
    private TaskIds f1Tasks;
    private TaskIds revokedTasks;
    private TaskIds noPermissionTasks;

    @BeforeEach
    void seedRealProvidersAndAuthorizationStates() {
        m1 = UUID.randomUUID();
        m2 = UUID.randomUUID();
        f1Ok = UUID.randomUUID();
        f2 = UUID.randomUUID();
        fRevoked = UUID.randomUUID();
        fNoPermission = UUID.randomUUID();
        insertUser(m1, "Today M1", "MOTHER");
        insertUser(m2, "Today M2", "MOTHER");
        insertUser(f1Ok, "Today F1 OK", "FAMILY");
        insertUser(f2, "Today F2", "FAMILY");
        insertUser(fRevoked, "Today Family Revoked", "FAMILY");
        insertUser(fNoPermission, "Today Family No Permission", "FAMILY");

        subject = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'MOTHER', 'Today M1', 'ACTIVE', now(), now())
                """, subject, m1, m1);
        journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(m1)
                .careSubjectId(subject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(EFFECTIVE_DATE.minusDays(70))
                .lastMenstrualDate(EFFECTIVE_DATE.minusDays(70))
                .estimatedDueDate(EFFECTIVE_DATE.plusDays(210))
                .build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey, subject);

        primaryGroup = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(m1)
                .groupName("Today primary group")
                .status(CareGroupStatus.ACTIVE)
                .linkedJourneyId(journey)
                .build()).getId();
        otherGroup = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(m2)
                .groupName("Today other group")
                .status(CareGroupStatus.ACTIVE)
                .build()).getId();

        String fullPermission = new FamilyPermission(false, false, false, false, true, true).toJson();
        insertMember(primaryGroup, f1Ok, InviteStatus.ACCEPTED, fullPermission);
        // Seed recipient rows while the insert-time invariant is true, then establish the
        // two negative lifecycle states before any API call.
        insertMember(primaryGroup, fRevoked, InviteStatus.ACCEPTED, fullPermission);
        insertMember(primaryGroup, fNoPermission, InviteStatus.ACCEPTED, fullPermission);
        insertMember(otherGroup, f2, InviteStatus.ACCEPTED, fullPermission);

        sharedReminderDefinition = insertReminder(m1, "M1 shared group reminder");
        UUID sharedOccurrence = ReminderOccurrenceIdFactory.create(
                sharedReminderDefinition, UPCOMING_AT);
        m1Tasks = insertUnionRows(m1, "MOTHER", "M1", sharedOccurrence);
        f1Tasks = insertUnionRows(f1Ok, "FAMILY", "F1", sharedOccurrence);
        revokedTasks = insertUnionRows(
                fRevoked, "FAMILY", "revoked", sharedOccurrence);
        noPermissionTasks = insertUnionRows(
                fNoPermission, "FAMILY", "no-permission", sharedOccurrence);
    }

    @AfterEach
    void cleanHermeticFixture() {
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        JdbcTemplate provisioner = new JdbcTemplate(provisionerDataSource);
        new TransactionTemplate(new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> {
                    provisioner.execute("set local session_replication_role = replica");
                    for (UUID userId : users()) {
                        provisioner.update("delete from audit_events where actor_user_id=? or subject_user_id=?",
                                userId, userId);
                        provisioner.update("delete from checklist_action_commands where actor_user_id=?", userId);
                    }
                    for (UUID reminderId : reminderRows) {
                        provisioner.update("delete from reminder_occurrence_aliases where reminder_definition_id=?",
                                reminderId);
                    }
                    if (adminTemplateVersionId != null) {
                        provisioner.update("""
                                delete from checklist_action_commands
                                 where task_id in (
                                     select child.checklist_task_instance_id
                                       from checklist_task_instances child
                                       join checklist_instances parent
                                         on parent.checklist_instance_id=child.checklist_instance_id
                                      where parent.template_version_id=?)
                                """, adminTemplateVersionId);
                        provisioner.update("""
                                delete from checklist_task_instances child
                                 where exists (
                                     select 1 from checklist_instances parent
                                      where parent.checklist_instance_id=child.checklist_instance_id
                                        and parent.template_version_id=?)
                                """, adminTemplateVersionId);
                        provisioner.update(
                                "delete from checklist_instances where template_version_id=?",
                                adminTemplateVersionId);
                    }
                    for (UUID taskId : checklistTasks) {
                        provisioner.update("delete from checklist_action_commands where task_id=?", taskId);
                        provisioner.update("delete from checklist_task_instances where checklist_task_instance_id=?",
                                taskId);
                    }
                    for (UUID instanceId : checklistInstances) {
                        provisioner.update("delete from checklist_instances where checklist_instance_id=?", instanceId);
                    }
                    for (UUID taskId : careTaskRows) {
                        provisioner.update("delete from care_tasks where task_id=?", taskId);
                    }
                    for (UUID reminderId : reminderRows) {
                        provisioner.update("delete from care_tasks where task_id=?", reminderId);
                    }
                    if (adminTemplateItemId != null) {
                        provisioner.update("delete from care_item_templates where template_id=?", adminTemplateItemId);
                    }
                    if (adminTemplateId != null) {
                        provisioner.update("delete from care_item_templates where template_id=?", adminTemplateId);
                    }
                    for (UUID groupId : new UUID[] {primaryGroup, otherGroup, authorizedBabyGroup}) {
                        if (groupId != null) {
                            provisioner.update("delete from care_group_members where care_group_id=?", groupId);
                            provisioner.update("delete from care_groups where care_group_id=?", groupId);
                        }
                    }
                    if (journey != null) {
                        provisioner.update("delete from mother_journeys where journey_id=?", journey);
                    }
                    if (subject != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", subject);
                    }
                    if (foreignBaby != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", foreignBaby);
                    }
                    if (authorizedBaby != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", authorizedBaby);
                    }
                    for (UUID userId : users()) {
                        provisioner.update("delete from users where user_id=?", userId);
                    }
                });
        checklistInstances.clear();
        checklistTasks.clear();
        careTaskRows.clear();
        reminderRows.clear();
    }

    @Test
    void directIdsUseTheSameNotFoundEnvelopeAndHaveNoSideEffectsForEveryUnauthorizedActor()
            throws Exception {
        assertAcceptedFamilyControl(fRevoked, revokedTasks);
        assertAcceptedFamilyControl(fNoPermission, noPermissionTasks);
        establishNegativeFamilyStates();
        List<Actor> unauthorized = List.of(
                new Actor(m2, "MOTHER"),
                new Actor(f2, "FAMILY"),
                new Actor(fRevoked, "FAMILY"),
                new Actor(fNoPermission, "FAMILY"));
        DatabaseState beforeState = databaseState();
        Set<UUID> beforeAudits = auditIds();
        Set<UUID> beforeCommands = commandIds();

        for (Actor actor : unauthorized) {
            for (var entry : protectedIds(actor).entrySet()) {
                DeniedResponse missing = invokeDenied(actor, entry.getKey(), UUID.randomUUID());
                DeniedResponse real = invokeDenied(actor, entry.getKey(), entry.getValue());

                assertCanonicalNotFound(missing);
                assertThat(real)
                        .as("neutral envelope for %s acting on %s", actor.userId(), entry.getKey())
                        .isEqualTo(missing);
            }
        }

        assertThat(databaseState()).isEqualTo(beforeState);
        assertThat(auditIds()).containsExactlyInAnyOrderElementsOf(beforeAudits);
        assertThat(commandIds()).containsExactlyInAnyOrderElementsOf(beforeCommands);
    }

    @Test
    void unifiedTodayReturnsExactStableThreeKindUnionAndIsolatesFamilyPermissions()
            throws Exception {
        assertAcceptedFamilyControl(fRevoked, revokedTasks);
        assertAcceptedFamilyControl(fNoPermission, noPermissionTasks);
        TemplateIds adminTemplate = seedApprovedMandatoryMotherTemplate();
        establishNegativeFamilyStates();
        JsonNode firstMother = today(m1, "MOTHER");
        JsonNode secondMother = today(m1, "MOTHER");
        JsonNode firstFamily = today(f1Ok, "FAMILY");
        JsonNode secondFamily = today(f1Ok, "FAMILY");

        assertExactUnion(firstMother, m1Tasks, null, m1);
        assertExactUnion(secondMother, m1Tasks, null, m1);
        assertExactUnion(firstFamily, f1Tasks, primaryGroup, f1Ok);
        assertExactUnion(secondFamily, f1Tasks, primaryGroup, f1Ok);
        JsonNode firstAdminTask = requiredSystemTemplateTask(firstMother, adminTemplate.versionId());
        JsonNode secondAdminTask = requiredSystemTemplateTask(secondMother, adminTemplate.versionId());
        assertThat(secondAdminTask.path("taskId").asText())
                .isEqualTo(firstAdminTask.path("taskId").asText());
        assertThat(secondAdminTask.path("instanceId").asText())
                .isEqualTo(firstAdminTask.path("instanceId").asText());
        assertThat(flatten(firstFamily)).noneMatch(task ->
                adminTemplate.versionId().toString().equals(task.path("templateVersionId").asText()));
        trackGeneratedChecklistTask(firstAdminTask);
        assertThat(kindToId(secondMother)).isEqualTo(kindToId(firstMother));
        assertThat(kindToId(secondFamily)).isEqualTo(kindToId(firstFamily));

        assertThat(kindToId(firstMother).get("CHECKLIST"))
                .isNotEqualTo(kindToId(firstFamily).get("CHECKLIST"));
        assertThat(kindToId(firstMother).get("CARE_TASK"))
                .isNotEqualTo(kindToId(firstFamily).get("CARE_TASK"));
        assertThat(kindToId(firstMother).get("REMINDER"))
                .isEqualTo(kindToId(firstFamily).get("REMINDER"));
        assertThat(flatten(today(fRevoked, "FAMILY")))
                .as("revoked recipient/assignee rows stay hidden").isEmpty();
        assertThat(flatten(today(fNoPermission, "FAMILY")))
                .as("accepted membership without checklist permissions stays hidden").isEmpty();
        assertThat(flatten(today(f2, "FAMILY")))
                .as("membership in another group does not widen the union").isEmpty();
    }

    @Test
    void chk033_authorizedJourneyTasksExposeGroupAndDeterministicLifecycleLabels()
            throws Exception {
        JsonNode response = today(f1Ok, "FAMILY");
        List<JsonNode> journeyTasks = flatten(response).stream()
                .filter(task -> "JOURNEY".equals(task.path("careContextType").asText()))
                .toList();

        assertThat(journeyTasks).hasSize(3);
        assertThat(journeyTasks)
                .allSatisfy(task -> {
                    assertThat(task.path("careGroupLabel").asText())
                            .isNotBlank()
                            .isEqualTo("Today primary group");
                    assertThat(task.path("careContextLabel").asText())
                            .isNotBlank()
                            .isEqualTo("Mang thai");
                });
        assertThat(response.toString())
                .doesNotContain(otherGroup.toString(), "Today other group");
    }

    @Test
    void chk033_authorizedBabyTaskExposesGroupAndNicknameLabels() throws Exception {
        ChecklistIds babyChecklist = insertAuthorizedBabyChecklist(f1Ok);

        JsonNode babyTask = flatten(today(f1Ok, "FAMILY")).stream()
                .filter(task -> babyChecklist.taskId().toString().equals(task.path("taskId").asText()))
                .findFirst()
                .orElseThrow();

        assertThat(babyTask.path("careGroupId").asText()).isEqualTo(authorizedBabyGroup.toString());
        assertThat(babyTask.path("careGroupLabel").asText())
                .isNotBlank()
                .isEqualTo("Today baby group");
        assertThat(babyTask.path("careContextType").asText()).isEqualTo("BABY");
        assertThat(babyTask.path("careContextId").asText()).isEqualTo(authorizedBaby.toString());
        assertThat(babyTask.path("careContextLabel").asText())
                .isNotBlank()
                .isEqualTo("Bé An");
    }

    @Test
    void acceptedFamilyCanCompleteMotherOwnedCurrentContextReminder() throws Exception {
        MvcResult result = mockMvc.perform(post(
                        "/api/v1/tasks/REMINDER/{taskId}/actions",
                        f1Tasks.reminderOccurrenceId())
                        .with(csrf())
                        .with(user(f1Ok.toString()).roles("FAMILY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "COMPLETE",
                                "clientRequestId", UUID.randomUUID()))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("taskKind").asText()).isEqualTo("REMINDER");
        assertThat(response.path("taskId").asText())
                .isEqualTo(f1Tasks.reminderOccurrenceId().toString());
        assertThat(response.path("action").asText()).isEqualTo("COMPLETE");
        assertThat(response.path("previousStatus").asText()).isEqualTo("PENDING");
        assertThat(response.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "select status from care_tasks where task_id=?",
                String.class, sharedReminderDefinition)).isEqualTo("COMPLETED");
    }

    @Test
    void acceptedFamilyCanCompleteOwnScopedChecklistTaskWithViewPermissionOnly() throws Exception {
        MvcResult result = mockMvc.perform(post(
                        "/api/v1/care-groups/{groupId}/checklists/tasks/{taskId}/actions",
                        primaryGroup, f1Tasks.checklistTaskId())
                        .with(csrf())
                        .with(user(f1Ok.toString()).roles("FAMILY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "COMPLETE",
                                "clientRequestId", UUID.randomUUID()))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("action").asText()).isEqualTo("COMPLETE");
        assertThat(response.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "select status from checklist_task_instances where checklist_task_instance_id=?",
                String.class, f1Tasks.checklistTaskId())).isEqualTo("COMPLETED");
    }

    @Test
    void acceptedMembershipRemainsAuthorizedAfterInvitationExpiry() throws Exception {
        jdbcTemplate.update("""
                update care_group_members
                   set invite_expires_at = now() - interval '1 day', updated_at = now()
                 where care_group_id=? and user_id=? and invitation_status='ACCEPTED'
                """, primaryGroup, f1Ok);

        assertExactUnion(today(f1Ok, "FAMILY"), f1Tasks, primaryGroup, f1Ok);
    }

    @Test
    void permittedFamilyDefinitionUuidUsesNeutralNotFoundWhileOccurrenceAliasRemainsAuthorized()
            throws Exception {
        DeniedResponse missing = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, UUID.randomUUID());
        DeniedResponse definition = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, sharedReminderDefinition);

        assertCanonicalNotFound(missing);
        assertThat(definition).isEqualTo(missing);
        assertThat(kindToId(today(f1Ok, "FAMILY")).get("REMINDER"))
                .isEqualTo(f1Tasks.reminderOccurrenceId().toString());
    }

    @Test
    void familyCannotViewOrActWhenJourneyRowBelongsToAnotherOwner() throws Exception {
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        JdbcTemplate provisioner = new JdbcTemplate(provisionerDataSource);
        new TransactionTemplate(new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> {
                    // Simulate a legacy/corrupt foreign-owner link without rewriting the
                    // canonical authority rows that deliberately protect normal writes.
                    provisioner.execute("set local session_replication_role = replica");
                    provisioner.update(
                            "update mother_journeys set owner_user_id=? where journey_id=?",
                            m2, journey);
                });

        assertThat(flatten(today(f1Ok, "FAMILY")).stream()
                .filter(task -> "REMINDER".equals(task.path("taskKind").asText())))
                .isEmpty();
        DeniedResponse missing = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, UUID.randomUUID());
        DeniedResponse foreignJourney = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, f1Tasks.reminderOccurrenceId());
        assertCanonicalNotFound(missing);
        assertThat(foreignJourney).isEqualTo(missing);
    }

    @Test
    void familyCannotViewOrActWhenBabyRowBelongsToAnotherOwner() throws Exception {
        foreignBaby = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'BABY', 'Foreign baby', 'ACTIVE', now(), now())
                """, foreignBaby, m2, m2);
        UUID definition = insertBabyReminder(m1, foreignBaby, "Foreign baby reminder");
        UUID occurrence = ReminderOccurrenceIdFactory.create(definition, UPCOMING_AT);

        assertThat(flatten(today(f1Ok, "FAMILY")).stream()
                .map(task -> task.path("taskId").asText()))
                .doesNotContain(occurrence.toString());
        DeniedResponse missing = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, UUID.randomUUID());
        DeniedResponse foreignBabyResponse = invokeDenied(
                new Actor(f1Ok, "FAMILY"), TaskKind.REMINDER, occurrence);
        assertCanonicalNotFound(missing);
        assertThat(foreignBabyResponse).isEqualTo(missing);
    }

    private TaskIds insertUnionRows(
            UUID actor, String recipientRole, String label, UUID sharedOccurrence) {
        ChecklistIds checklist = insertChecklist(actor, recipientRole, label + " checklist");
        UUID careTask = insertCareTask(actor, label + " care task");
        return new TaskIds(checklist.instanceId(), checklist.taskId(), careTask, sharedOccurrence);
    }

    private ChecklistIds insertChecklist(UUID recipient, String role, String title) {
        UUID instanceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID recipientCareGroupId = "MOTHER".equals(role) ? null : primaryGroup;
        String instanceKey = ChecklistDistributionKeyFactory.userCreatedInstanceKey(
                recipient, role, recipientCareGroupId, "JOURNEY", journey, null, null);
        String taskKey = ChecklistDistributionKeyFactory.userCreatedChildKey(instanceId, taskId);
        jdbcTemplate.update("""
                insert into checklist_instances (
                    checklist_instance_id, distribution_key, key_version, recipient_user_id,
                    recipient_role, care_group_id, care_context_type, care_context_id,
                    context_owner_user_id, origin, status, lock_version, created_at, updated_at)
                values (?, ?, 'v1', ?, ?, ?, 'JOURNEY', ?, ?, 'USER_CREATED',
                        'PENDING', 0, now(), now())
                """, instanceId, instanceKey, recipient, role, recipientCareGroupId, journey, m1);
        jdbcTemplate.update("""
                insert into checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, task_key, key_version,
                    title_snapshot, display_order, is_required, target_subject, due_at,
                    status, lock_version, created_at, updated_at)
                values (?, ?, ?, 'v1', ?, 1, true, 'MOTHER', ?, 'PENDING', 0, now(), now())
                """, taskId, instanceId, taskKey, title, Timestamp.from(TODAY_AT));
        checklistInstances.add(instanceId);
        checklistTasks.add(taskId);
        return new ChecklistIds(instanceId, taskId);
    }

    private ChecklistIds insertAuthorizedBabyChecklist(UUID recipient) {
        authorizedBaby = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                values (?, ?, ?, 'BABY', 'Bé An', 'ACTIVE', now(), now())
                """, authorizedBaby, m1, m1);
        authorizedBabyGroup = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(m1)
                .groupName("Today baby group")
                .status(CareGroupStatus.ACTIVE)
                .linkedJourneyId(journey)
                .linkedBabyProfileId(authorizedBaby)
                .build()).getId();
        String fullPermission = new FamilyPermission(false, false, false, false, true, true).toJson();
        insertMember(authorizedBabyGroup, recipient, InviteStatus.ACCEPTED, fullPermission);

        UUID instanceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String instanceKey = ChecklistDistributionKeyFactory.userCreatedInstanceKey(
                recipient, "FAMILY", authorizedBabyGroup, "BABY", authorizedBaby, null, null);
        String taskKey = ChecklistDistributionKeyFactory.userCreatedChildKey(instanceId, taskId);
        jdbcTemplate.update("""
                insert into checklist_instances (
                    checklist_instance_id, distribution_key, key_version, recipient_user_id,
                    recipient_role, care_group_id, care_context_type, care_context_id,
                    context_owner_user_id, origin, status, lock_version, created_at, updated_at)
                values (?, ?, 'v1', ?, 'FAMILY', ?, 'BABY', ?, ?, 'USER_CREATED',
                        'PENDING', 0, now(), now())
                """, instanceId, instanceKey, recipient, authorizedBabyGroup, authorizedBaby, m1);
        jdbcTemplate.update("""
                insert into checklist_task_instances (
                    checklist_task_instance_id, checklist_instance_id, task_key, key_version,
                    title_snapshot, display_order, is_required, target_subject, due_at,
                    status, lock_version, created_at, updated_at)
                values (?, ?, ?, 'v1', 'Baby checklist', 1, true, 'BABY', ?,
                        'PENDING', 0, now(), now())
                """, taskId, instanceId, taskKey, Timestamp.from(TODAY_AT));
        checklistInstances.add(instanceId);
        checklistTasks.add(taskId);
        return new ChecklistIds(instanceId, taskId);
    }

    private UUID insertCareTask(UUID assignee, String title) {
        UUID taskId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, care_group_id, creator_user_id,
                    assignee_user_id, title, scheduled_at, status, origin, target_subject,
                    journey_id, created_at, updated_at)
                values (?, 'MANUAL_TASK', ?, ?, ?, ?, ?, ?, 'OPEN', 'USER_CREATED',
                        'MOTHER', ?, now(), now())
                """, taskId, m1, primaryGroup, m1, assignee, title,
                Timestamp.from(OVERDUE_AT), journey);
        careTaskRows.add(taskId);
        return taskId;
    }

    private UUID insertReminder(UUID owner, String title) {
        UUID reminderId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, title, scheduled_at, item_type,
                    status, origin, target_subject, journey_id, created_at, updated_at)
                values (?, 'SCHEDULED_REMINDER', ?, ?, ?, 'APPOINTMENT', 'PENDING',
                        'USER_CREATED', 'MOTHER', ?, now(), now())
                """, reminderId, owner, title, Timestamp.from(UPCOMING_AT), journey);
        reminderRows.add(reminderId);
        return reminderId;
    }

    private UUID insertBabyReminder(UUID owner, UUID babyId, String title) {
        UUID reminderId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_tasks (
                    task_id, task_type, owner_user_id, title, scheduled_at, item_type,
                    status, origin, target_subject, baby_id, created_at, updated_at)
                values (?, 'SCHEDULED_REMINDER', ?, ?, ?, 'APPOINTMENT', 'PENDING',
                        'USER_CREATED', 'BABY', ?, now(), now())
                """, reminderId, owner, title, Timestamp.from(UPCOMING_AT), babyId);
        reminderRows.add(reminderId);
        return reminderId;
    }

    private void insertMember(UUID groupId, UUID userId, InviteStatus status, String permissionJson) {
        memberRepository.saveAndFlush(CareGroupMember.builder()
                .careGroupId(groupId)
                .userId(userId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(status)
                .permissionJson(permissionJson)
                .build());
    }

    private DeniedResponse invokeDenied(Actor actor, TaskKind kind, UUID taskId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks/{kind}/{taskId}/actions", kind, taskId)
                        .with(csrf())
                        .with(user(actor.userId().toString()).roles(actor.role()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "COMPLETE",
                                "clientRequestId", UUID.randomUUID()))))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Set<String> keys = new TreeSet<>();
        body.fieldNames().forEachRemaining(keys::add);
        ObjectNode canonicalBody = (ObjectNode) body.deepCopy();
        canonicalBody.remove(List.of("timestamp", "path"));
        return new DeniedResponse(result.getResponse().getStatus(), keys, canonicalBody);
    }

    private static void assertCanonicalNotFound(DeniedResponse response) {
        assertThat(response.httpStatus()).isEqualTo(404);
        assertThat(response.bodyKeys()).containsExactlyInAnyOrder(
                "success", "status", "error", "message", "path", "details", "metadata", "timestamp");
        assertThat(response.canonicalBody().path("success").asBoolean()).isFalse();
        assertThat(response.canonicalBody().path("status").asInt()).isEqualTo(404);
        assertThat(response.canonicalBody().path("error").asText()).isEqualTo("TASK_NOT_FOUND");
        assertThat(response.canonicalBody().path("message").asText()).isEqualTo("Task not found");
        assertThat(response.canonicalBody().path("details").isNull()).isTrue();
        JsonNode metadata = response.canonicalBody().path("metadata");
        assertThat(metadata.isNull() || metadata.isEmpty()).isTrue();
    }

    private void assertAcceptedFamilyControl(UUID actor, TaskIds expected) throws Exception {
        assertExactUnion(today(actor, "FAMILY"), expected, primaryGroup, actor);
    }

    private void establishNegativeFamilyStates() {
        jdbcTemplate.update("""
                update care_group_members
                   set invitation_status='REVOKED', updated_at=now()
                 where care_group_id=? and user_id=?
                """, primaryGroup, fRevoked);
        String noPermission = new FamilyPermission(
                false, false, false, false, false, false).toJson();
        jdbcTemplate.update("""
                update care_group_members
                   set permission_json=?::jsonb, updated_at=now()
                 where care_group_id=? and user_id=?
                """, noPermission, primaryGroup, fNoPermission);
    }

    private Map<TaskKind, UUID> protectedIds(Actor actor) {
        TaskIds ids = actor.userId().equals(fRevoked)
                ? revokedTasks
                : actor.userId().equals(fNoPermission) ? noPermissionTasks : m1Tasks;
        return Map.of(
                TaskKind.CHECKLIST, ids.checklistTaskId(),
                TaskKind.CARE_TASK, ids.careTaskId(),
                TaskKind.REMINDER, ids.reminderOccurrenceId());
    }

    private DatabaseState databaseState() {
        JsonNode parents = objectMapper.valueToTree(jdbcTemplate.queryForList("""
                select * from checklist_instances
                 where care_group_id=?
                   and recipient_user_id in (?, ?, ?, ?)
                 order by checklist_instance_id
                """, primaryGroup, m1, f1Ok, fRevoked, fNoPermission));
        JsonNode tasks = objectMapper.valueToTree(jdbcTemplate.queryForList("""
                select task.* from checklist_task_instances task
                join checklist_instances parent
                  on parent.checklist_instance_id=task.checklist_instance_id
                 where parent.care_group_id=?
                   and parent.recipient_user_id in (?, ?, ?, ?)
                 order by task.checklist_task_instance_id
                """, primaryGroup, m1, f1Ok, fRevoked, fNoPermission));
        JsonNode careTasks = objectMapper.valueToTree(jdbcTemplate.queryForList("""
                select * from care_tasks
                 where task_id=? or (care_group_id=? and assignee_user_id in (?, ?, ?, ?))
                 order by task_id
                """, sharedReminderDefinition, primaryGroup, m1, f1Ok, fRevoked, fNoPermission));
        JsonNode aliases = objectMapper.valueToTree(jdbcTemplate.queryForList("""
                select * from reminder_occurrence_aliases
                 where reminder_definition_id=?
                 order by occurrence_id
                """, sharedReminderDefinition));
        return new DatabaseState(parents, tasks, careTasks, aliases);
    }

    private Set<UUID> auditIds() {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "select audit_event_id from audit_events order by audit_event_id", UUID.class));
    }

    private Set<UUID> commandIds() {
        return new LinkedHashSet<>(jdbcTemplate.queryForList("""
                select checklist_action_command_id
                  from checklist_action_commands
                 order by checklist_action_command_id
                """, UUID.class));
    }

    private JsonNode today(UUID actor, String role) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tasks/today")
                        .param("date", EFFECTIVE_DATE.toString())
                        .header("X-User-Timezone", ZONE)
                        .with(user(actor.toString()).roles(role)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertExactUnion(
            JsonNode response,
            TaskIds expected,
            UUID checklistCareGroupId,
            UUID actorUserId) {
        List<JsonNode> tasks = flatten(response);
        assertThat(tasks).hasSizeGreaterThanOrEqualTo(3);
        assertThat(tasks).extracting(task -> task.path("taskKind").asText())
                .contains("CHECKLIST", "CARE_TASK", "REMINDER");
        assertThat(kindToId(response)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "CHECKLIST", expected.checklistTaskId().toString(),
                "CARE_TASK", expected.careTaskId().toString(),
                "REMINDER", expected.reminderOccurrenceId().toString()));
        assertThat(tasks).extracting(task -> task.path("taskKind").asText()
                        + ":" + task.path("taskId").asText())
                .doesNotHaveDuplicates();

        Set<String> expectedTaskIds = Set.of(
                expected.checklistTaskId().toString(),
                expected.careTaskId().toString(),
                expected.reminderOccurrenceId().toString());
        assertThat(tasks.stream()
                .filter(task -> !expectedTaskIds.contains(task.path("taskId").asText()))
                .toList())
                .as("every additional Today row is an eligible admin checklist materialization")
                .allSatisfy(task -> {
                    assertThat(task.path("taskKind").asText()).isEqualTo("CHECKLIST");
                    assertThat(task.path("origin").asText()).isEqualTo("SYSTEM_TEMPLATE");
                    assertThat(task.path("instanceId").isTextual()).isTrue();
                    assertThat(task.path("templateVersionId").isTextual()).isTrue();
                    assertThat(UUID.fromString(task.path("instanceId").asText())).isNotNull();
                    assertThat(UUID.fromString(task.path("templateVersionId").asText())).isNotNull();
                    assertLegitimateSystemTemplateTask(task, actorUserId, checklistCareGroupId);
                });

        JsonNode checklist = task(tasks, expected.checklistTaskId());
        JsonNode careTask = task(tasks, expected.careTaskId());
        JsonNode reminder = task(tasks, expected.reminderOccurrenceId());
        assertExactMetadata(checklist, "USER_CREATED", "MOTHER", "PENDING",
                checklistCareGroupId, "JOURNEY", journey, expected.checklistInstanceId(), null,
                Set.of("COMPLETE"));
        assertExactMetadata(careTask, "USER_CREATED", "MOTHER", "PENDING",
                primaryGroup, "JOURNEY", journey, null, null, Set.of("COMPLETE"));
        assertExactMetadata(reminder, "USER_CREATED", "MOTHER", "PENDING",
                primaryGroup, "JOURNEY", journey, null, null,
                Set.of("COMPLETE", "SKIP"));

        Map<String, String> buckets = List.of(checklist, careTask, reminder).stream().collect(Collectors.toMap(
                task -> task.path("taskKind").asText(),
                task -> task.path("timeBucket").asText()));
        assertThat(buckets).containsExactlyInAnyOrderEntriesOf(Map.of(
                "CHECKLIST", "TODAY",
                "CARE_TASK", "OVERDUE",
                "REMINDER", "UPCOMING"));
        assertThat(response.path("counts").path("overdue").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("counts").path("today").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("counts").path("upcoming").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("counts").path("unscheduled").asInt())
                .isEqualTo(response.path("sections").path("unscheduled").size());
        assertThat(response.path("sections").path("overdue").size()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("sections").path("today").size()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("sections").path("upcoming").size()).isGreaterThanOrEqualTo(1);
    }

    private static JsonNode task(List<JsonNode> tasks, UUID taskId) {
        return tasks.stream()
                .filter(task -> taskId.toString().equals(task.path("taskId").asText()))
                .findFirst()
                .orElseThrow();
    }

    private void assertLegitimateSystemTemplateTask(
            JsonNode task,
            UUID actorUserId,
            UUID expectedCareGroupId) {
        UUID taskId = UUID.fromString(task.path("taskId").asText());
        UUID instanceId = UUID.fromString(task.path("instanceId").asText());
        UUID templateVersionId = UUID.fromString(task.path("templateVersionId").asText());
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select child.title_snapshot, child.status, child.target_subject, child.due_at,
                       parent.recipient_user_id, parent.care_group_id,
                       parent.care_context_type, parent.care_context_id, parent.origin,
                       template.recipient_scope
                  from checklist_task_instances child
                  join checklist_instances parent
                    on parent.checklist_instance_id=child.checklist_instance_id
                  join care_item_templates template
                    on template.template_version_id=parent.template_version_id
                   and template.entry_type='TEMPLATE_ROOT'
                 where child.checklist_task_instance_id=?
                   and parent.checklist_instance_id=?
                   and parent.template_version_id=?
                   and child.template_version_id=?
                   and parent.origin='SYSTEM_TEMPLATE'
                   and template.content_status='APPROVED'
                   and template.distribution_enabled=true
                   and template.template_type='MANDATORY'
                """, taskId, instanceId, templateVersionId, templateVersionId);
        assertThat(row.get("recipient_user_id")).isEqualTo(actorUserId);
        assertThat(row.get("care_group_id")).isEqualTo(expectedCareGroupId);
        assertThat(row.get("care_context_type")).isEqualTo("JOURNEY");
        assertThat(row.get("care_context_id")).isEqualTo(journey);
        assertThat(row.get("origin")).isEqualTo("SYSTEM_TEMPLATE");
        assertThat(row.get("recipient_scope")).isIn(
                expectedCareGroupId == null ? "MOTHER" : "FAMILY", "BOTH");
        assertThat(task.path("title").asText()).isEqualTo(row.get("title_snapshot"));
        assertThat(task.path("status").asText()).isEqualTo(row.get("status"));
        assertThat(task.path("targetSubject").asText()).isEqualTo(row.get("target_subject"));
    }

    private TemplateIds seedApprovedMandatoryMotherTemplate() {
        adminTemplateId = UUID.randomUUID();
        adminTemplateVersionId = UUID.randomUUID();
        adminTemplateItemId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_item_templates (
                    template_id, entry_type, title, description, stage, is_active,
                    version, template_status, content_status, template_lineage_id,
                    template_version_id, migration_review_required,
                    distribution_enabled, template_type, recipient_scope,
                    eligibility_anchor_type, eligibility_range_unit,
                    eligibility_start_inclusive, eligibility_end_inclusive,
                    created_at, updated_at)
                values (?, 'TEMPLATE_ROOT', 'Today mandatory admin template', 'deterministic mixed-source fixture',
                    'PREGNANCY', true, 1, 'ACTIVE', 'DRAFT', ?, ?, false,
                    false, 'MANDATORY', 'MOTHER', 'LMP', 'WEEK', 0, 20,
                    now(), now())
                """, adminTemplateId, adminTemplateId, adminTemplateVersionId);
        jdbcTemplate.update("""
                insert into care_item_templates (
                    template_id, parent_template_id, entry_type, title, display_order,
                    stage, is_active, version, template_status, content_status,
                    target_subject, is_required, due_anchor_type, due_offset_start,
                    due_offset_end, due_offset_unit, created_at, updated_at)
                values (?, ?, 'CHECKLIST_ENTRY', 'Today mandatory admin task', 1,
                    'PREGNANCY', true, 1, 'ACTIVE', 'APPROVED',
                    'MOTHER', true, 'LMP', 70, 70, 'DAY', now(), now())
                """, adminTemplateItemId, adminTemplateId);
        jdbcTemplate.update("""
                update care_item_templates
                   set content_status='APPROVED', distribution_enabled=true,
                       approved_at=now(), approved_by=?
                 where template_id=?
                """, m1, adminTemplateId);
        return new TemplateIds(adminTemplateVersionId);
    }

    private static JsonNode requiredSystemTemplateTask(JsonNode response, UUID templateVersionId) {
        List<JsonNode> matches = flatten(response).stream()
                .filter(task -> templateVersionId.toString()
                        .equals(task.path("templateVersionId").asText()))
                .toList();
        assertThat(matches).singleElement().satisfies(task -> {
            assertThat(task.path("taskKind").asText()).isEqualTo("CHECKLIST");
            assertThat(task.path("origin").asText()).isEqualTo("SYSTEM_TEMPLATE");
        });
        return matches.getFirst();
    }

    private void trackGeneratedChecklistTask(JsonNode task) {
        UUID taskId = UUID.fromString(task.path("taskId").asText());
        UUID instanceId = UUID.fromString(task.path("instanceId").asText());
        if (!checklistTasks.contains(taskId)) checklistTasks.add(taskId);
        if (!checklistInstances.contains(instanceId)) checklistInstances.add(instanceId);
    }

    private static void assertExactMetadata(
            JsonNode task,
            String origin,
            String target,
            String status,
            UUID careGroupId,
            String contextType,
            UUID contextId,
            UUID instanceId,
            UUID templateVersionId,
            Set<String> allowedActions) {
        assertThat(task.path("origin").asText()).isEqualTo(origin);
        assertThat(task.path("targetSubject").asText()).isEqualTo(target);
        assertThat(task.path("status").asText()).isEqualTo(status);
        if (careGroupId == null) {
            assertThat(task.path("careGroupId").isNull()).isTrue();
        } else {
            assertThat(task.path("careGroupId").asText()).isEqualTo(careGroupId.toString());
        }
        assertThat(task.path("careContextType").asText()).isEqualTo(contextType);
        assertThat(task.path("careContextId").asText()).isEqualTo(contextId.toString());
        assertNullableUuid(task.path("instanceId"), instanceId);
        assertNullableUuid(task.path("templateVersionId"), templateVersionId);
        Set<String> actualActions = new LinkedHashSet<>();
        task.path("allowedActions").forEach(action -> actualActions.add(action.asText()));
        assertThat(actualActions).containsExactlyInAnyOrderElementsOf(allowedActions);
    }

    private static void assertNullableUuid(JsonNode node, UUID expected) {
        if (expected == null) {
            assertThat(node.isNull()).isTrue();
        } else {
            assertThat(node.asText()).isEqualTo(expected.toString());
        }
    }

    private static List<JsonNode> flatten(JsonNode response) {
        List<JsonNode> tasks = new ArrayList<>();
        response.path("sections").fields().forEachRemaining(section -> section.getValue().forEach(tasks::add));
        return tasks;
    }

    private static Map<String, String> kindToId(JsonNode response) {
        return flatten(response).stream()
                .filter(task -> "USER_CREATED".equals(task.path("origin").asText()))
                .collect(Collectors.toMap(
                task -> task.path("taskKind").asText(),
                task -> task.path("taskId").asText()));
    }


    private void insertUser(UUID id, String name, String role) {
        String phone = String.format("09%08d", Math.floorMod(id.hashCode(), 100_000_000));
        CanonicalUserFixture.insertUser(jdbcTemplate, id, name, phone, role);
    }

    private List<UUID> users() {
        List<UUID> result = new ArrayList<>();
        for (UUID id : new UUID[] {m1, m2, f1Ok, f2, fRevoked, fNoPermission}) {
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private record Actor(UUID userId, String role) {
    }

    private record ChecklistIds(UUID instanceId, UUID taskId) {
    }

    private record TaskIds(
            UUID checklistInstanceId,
            UUID checklistTaskId,
            UUID careTaskId,
            UUID reminderOccurrenceId) {
    }

    private record TemplateIds(UUID versionId) {
    }

    private record DeniedResponse(int httpStatus, Set<String> bodyKeys, JsonNode canonicalBody) {
    }

    private record DatabaseState(
            JsonNode checklistParents,
            JsonNode checklistTasks,
            JsonNode careTasksAndReminder,
            JsonNode reminderAliases) {
    }
}
