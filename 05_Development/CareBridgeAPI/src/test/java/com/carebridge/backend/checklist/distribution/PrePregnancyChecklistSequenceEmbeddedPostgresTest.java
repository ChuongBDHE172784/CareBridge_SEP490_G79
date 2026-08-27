package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.sequence.ChecklistSequenceState;
import com.carebridge.backend.checklist.sequence.ChecklistSequenceAdvanceRequest;
import com.carebridge.backend.checklist.sequence.ChecklistSequenceAdvanceService;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskServiceImpl;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL regression for catalog requiredness through sequence qualification. */
@EnabledOnOs(OS.WINDOWS)
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class PrePregnancyChecklistSequenceEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private MotherJourneyRepository journeys;
    @Autowired private JpaChecklistReconciliationSource reconciliationSource;
    @Autowired private UnifiedTaskActionFacade actions;
    @Autowired private UnifiedTodayTaskServiceImpl today;
    @Autowired private ChecklistSequenceAdvanceService advanceService;

    @Test
    void requiredCatalogLeafMaterializesRequiredAndCompletedSetBecomesReadyToAdvance() {
        UUID mother = UUID.randomUUID();
        UUID journey = seedMotherJourney(mother);
        TemplateIds first = seedApprovedSequenceTemplate(mother, 1);
        seedApprovedSequenceTemplate(mother, 2);

        ChecklistDistributionCommand candidate = reconciliationSource.loadCandidatesForActor(
                        mother, LocalDate.of(2026, 8, 13), ZoneId.of("Asia/Ho_Chi_Minh"), UUID.randomUUID())
                .stream()
                .filter(value -> first.version().equals(value.templateVersionId()))
                .findFirst()
                .orElseThrow();
        assertThat(candidate.checklistContractVersion()).isEqualTo((short) 2);
        assertThat(candidate.items()).singleElement().satisfies(item -> {
            assertThat(item.required()).isTrue();
            assertThat(item.targetSubject()).isNull();
        });

        var firstToday = today.getTodayTasks(
                mother, LocalDate.of(2026, 8, 13), "Asia/Ho_Chi_Minh", null, true);
        var firstSequence = firstToday.sequence();
        List<TodayTaskItemResponse> firstTasks = templateTasks(firstToday, first.version());
        assertThat(firstSequence.sequenceState()).isEqualTo(ChecklistSequenceState.ACTIVE);
        assertThat(firstSequence.currentPosition()).isEqualTo(1);
        assertThat(firstSequence.currentInstanceId()).isNotNull();
        assertThat(firstTasks).singleElement().satisfies(task -> {
            assertThat(task.instanceId()).isEqualTo(firstSequence.currentInstanceId());
            assertThat(task.templateVersionId()).isEqualTo(first.version());
        });
        UUID taskId = firstTasks.getFirst().taskId();
        UUID instanceId = firstSequence.currentInstanceId();
        assertThat(jdbc.queryForObject("""
                select historical_at is null from checklist_instances
                 where checklist_instance_id=?
                """, Boolean.class, instanceId)).isTrue();
        assertThat(jdbc.queryForMap("""
                select checklist_contract_version, period_key, schedule_zone_id,
                       materialization_mode, was_actionable
                  from checklist_instances
                 where checklist_instance_id=?
                """, instanceId))
                .containsEntry("checklist_contract_version", 2)
                .containsEntry("period_key", ChecklistPeriodIdentity.V2_NON_CADENCE_PERIOD_KEY)
                .containsEntry("schedule_zone_id", ChecklistPeriodIdentity.V2_NON_CADENCE_ZONE_ID)
                .containsEntry("materialization_mode",
                        ChecklistPeriodIdentity.V2_NON_CADENCE_MODE.name())
                .containsEntry("was_actionable", true);
        assertThat(jdbc.queryForObject("""
                select is_required from checklist_task_instances
                 where checklist_task_instance_id=?
                """, Boolean.class, taskId)).isTrue();

        var repeatedToday = today.getTodayTasks(
                mother, LocalDate.of(2026, 8, 13), "Asia/Ho_Chi_Minh", null, true);
        assertThat(repeatedToday.sequence().currentInstanceId()).isEqualTo(instanceId);
        assertThat(templateTasks(repeatedToday, first.version()))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.taskId()).isEqualTo(taskId);
                    assertThat(task.instanceId()).isEqualTo(instanceId);
                });
        assertThat(jdbc.queryForObject("""
                select count(*) from checklist_instances
                 where recipient_user_id=? and template_version_id=?
                """, Integer.class, mother, first.version())).isOne();
        assertThat(jdbc.queryForObject("""
                select count(*)
                  from checklist_task_instances task
                  join checklist_instances parent
                    on parent.checklist_instance_id=task.checklist_instance_id
                 where parent.recipient_user_id=? and parent.template_version_id=?
                """, Integer.class, mother, first.version())).isOne();

        var readOnlyToday = today.getTodayTasks(
                mother, LocalDate.of(2026, 8, 13), "Asia/Ho_Chi_Minh", null, false);
        assertThat(readOnlyToday.sequence().currentInstanceId()).isEqualTo(instanceId);
        assertThat(templateTasks(readOnlyToday, first.version()))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.taskId()).isEqualTo(taskId);
                    assertThat(task.instanceId()).isEqualTo(instanceId);
                });

        actions.apply(mother, TaskKind.CHECKLIST, taskId,
                new TaskActionRequest(TaskAction.COMPLETE, UUID.randomUUID(), null));

        var todayResponse = today.getTodayTasks(
                mother, LocalDate.of(2026, 8, 13), "Asia/Ho_Chi_Minh", null, true);
        var sequence = todayResponse.sequence();
        assertThat(sequence.sequenceState()).isEqualTo(ChecklistSequenceState.READY_TO_ADVANCE);
        assertThat(sequence.advanceAvailable()).isTrue();
        assertThat(sequence.currentPosition()).isEqualTo(1);
        assertThat(sequence.nextSet().position()).isEqualTo(2);

        var advanced = advanceService.advance(mother,
                new ChecklistSequenceAdvanceRequest(sequence.currentInstanceId(), UUID.randomUUID()));
        assertThat(advanced.predecessorInstanceId()).isEqualTo(sequence.currentInstanceId());
        assertThat(advanced.successorInstanceId()).isNotNull();
        assertThat(advanced.sequence().currentPosition()).isEqualTo(2);
        assertThat(advanced.sequence().currentInstanceId()).isEqualTo(advanced.successorInstanceId());
        assertThat(jdbc.queryForMap("""
                select checklist_contract_version, period_key, schedule_zone_id,
                       materialization_mode, was_actionable
                  from checklist_instances
                where checklist_instance_id=?
                """, advanced.successorInstanceId()))
                .containsEntry("checklist_contract_version", 2)
                .containsEntry("period_key", "O:USER_CREATED")
                .containsEntry("schedule_zone_id", "UTC")
                .containsEntry("materialization_mode", "INTERACTIVE")
                .containsEntry("was_actionable", true);
        assertThat(jdbc.queryForMap("""
                select is_required, checklist_contract_version, target_subject
                  from checklist_task_instances
                where checklist_instance_id=?
                """, advanced.successorInstanceId()))
                .containsEntry("is_required", true)
                .containsEntry("checklist_contract_version", 2)
                .containsEntry("target_subject", null);
    }

    private UUID seedMotherJourney(UUID mother) {
        CanonicalUserFixture.insertUser(jdbc, mother, "Sequence Mother",
                String.format("08%08d", Math.floorMod(mother.hashCode(), 100_000_000)), "MOTHER");
        UUID subject = UUID.randomUUID();
        jdbc.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, person_id, user_id, 'MOTHER', display_name, 'ACTIVE', now(), now()
                  from users where user_id=?
                """, subject, mother);
        MotherJourney journey = journeys.saveAndFlush(MotherJourney.builder()
                .ownerUserId(mother)
                .careSubjectId(subject)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 8, 1))
                .build());
        jdbc.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey.getId(), subject);
        return journey.getId();
    }

    private TemplateIds seedApprovedSequenceTemplate(UUID approver, int position) {
        UUID root = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        transactions.executeWithoutResult(status -> {
            jdbc.update("""
                    insert into care_item_templates (
                        template_id, entry_type, title, stage, is_active, version,
                        template_status, content_status, template_lineage_id,
                        template_version_id, migration_review_required, distribution_enabled,
                        template_type, recipient_scope, eligibility_anchor_type,
                        eligibility_range_unit, eligibility_start_inclusive,
                        eligibility_end_inclusive, display_order, schedule_type,
                        materialization_policy, checklist_contract_version,
                        created_at, updated_at)
                    values (?, 'TEMPLATE_ROOT', ?, 'PRE_PREGNANCY', true, 1,
                        'ACTIVE', 'DRAFT', ?, ?, false, false, 'MANDATORY', 'MOTHER',
                        'NONE', 'DAY', 0, 0, ?, 'SET', 'SEQUENCE_STEP', 2, now(), now())
                    """, root, "Sequence set " + position, root, version, position);
            jdbc.update("""
                    insert into care_item_templates (
                        template_id, parent_template_id, entry_type, title, display_order,
                        stage, is_active, version, template_status, content_status,
                        target_subject, is_required, checklist_contract_version,
                        created_at, updated_at)
                    values (?, ?, 'CHECKLIST_ENTRY', ?, 1, 'PRE_PREGNANCY', true, 1,
                        'ACTIVE', 'APPROVED', null, true, 2, now(), now())
                    """, item, root, "Required item " + position);
            jdbc.update("""
                    update care_item_templates
                       set content_status='APPROVED', distribution_enabled=true,
                           approved_at=now(), approved_by=?
                     where template_id=?
                    """, approver, root);
        });
        return new TemplateIds(root, version, item);
    }

    private static List<TodayTaskItemResponse> templateTasks(
            TodayTasksResponse response,
            UUID templateVersionId) {
        return Stream.of(
                        response.sections().overdue(),
                        response.sections().today(),
                        response.sections().upcoming(),
                        response.sections().unscheduled())
                .flatMap(List::stream)
                .filter(task -> templateVersionId.equals(task.templateVersionId()))
                .toList();
    }

    private record TemplateIds(UUID root, UUID version, UUID item) {
    }
}
