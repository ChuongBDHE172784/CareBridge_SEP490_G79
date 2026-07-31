package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.mapper.AuditLogMapper;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.impl.AuditServiceImpl;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Real PostgreSQL fault-matrix evidence for CHK-038 business/audit atomicity. */
@EnabledOnOs(OS.WINDOWS)
@Import(ChecklistBusinessAuditAtomicityPostgresTest.AuditFaultTestConfiguration.class)
class ChecklistBusinessAuditAtomicityPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private CareGroupRepository careGroupRepository;
    @Autowired private ChecklistDistributionService distributionService;
    @Autowired private UnifiedTaskActionFacade actionFacade;
    @Autowired private ICareGroupService careGroupService;
    @Autowired private DtoSerializationFault dtoSerializationFault;
    @Autowired private AuditPersistenceFault auditPersistenceFault;

    @MockitoSpyBean private AuditEligibilityPolicy auditEligibilityPolicy;

    private UUID contentAdmin;
    private UUID systemAdmin;
    private UUID mother;
    private UUID subject;
    private UUID journey;
    private UUID replacementSubject;
    private UUID replacementJourney;
    private UUID careGroup;
    private UUID templateVersionId;
    private UUID taskId;
    private ChecklistDistributionCommand initialCommand;
    private Observation baseline;

    @BeforeEach
    void seedPendingAggregate() throws Exception {
        reset(auditEligibilityPolicy);
        dtoSerializationFault.reset();
        auditPersistenceFault.reset();
        seedCanonicalJourneyContext();
        TemplateIds template = createAndApproveTemplate();
        templateVersionId = template.versionId();
        ChecklistLifecycleEligibilityValue pregnancyWeeks = ChecklistLifecycleEligibilityValue.builder()
                .stage(ContentStage.PREGNANCY.name())
                .anchorType(ChecklistAnchorType.LMP)
                .rangeUnit(ChecklistRangeUnit.WEEK)
                .startInclusive(0)
                .endInclusive(12)
                .active(true)
                .build();
        initialCommand = new ChecklistDistributionCommand(
                template.lineageId(), template.versionId(), null, mother,
                ChecklistCareContextType.JOURNEY, journey, mother, ContentStage.PREGNANCY, pregnancyWeeks,
                new ChecklistLifecycleDates(
                        jdbcTemplate.queryForObject(
                                "select last_menstrual_date from mother_journeys where journey_id=?",
                                LocalDate.class, journey),
                        jdbcTemplate.queryForObject(
                                "select estimated_due_date from mother_journeys where journey_id=?",
                                LocalDate.class, journey), null, null),
                jdbcTemplate.queryForObject("select current_date", LocalDate.class), ZoneId.of("UTC"),
                List.of(new ChecklistDistributionRecipient(
                        mother, ChecklistRecipientRole.MOTHER, true, true, true)),
                List.of(new ChecklistDistributionItem(
                        template.itemId(), "CHK-038 pending task", 1, true,
                        ChecklistTargetSubject.MOTHER, ChecklistAnchorType.LMP, 0)),
                UUID.randomUUID());

        ChecklistDistributionResult seeded = distributionService.distribute(initialCommand);
        assertThat(seeded.createdInstances()).isEqualTo(1);
        assertThat(seeded.createdTasks()).isEqualTo(1);
        taskId = jdbcTemplate.queryForObject(
                "select checklist_task_instance_id from checklist_task_instances where template_version_id=?",
                UUID.class, templateVersionId);
        baseline = observeInIndependentTransaction();
        assertThat(baseline.state().parentCount()).isEqualTo(1);
        assertThat(baseline.state().taskCount()).isEqualTo(1);
        assertThat(baseline.state().aggregate().parentStatus()).isEqualTo("PENDING");
        assertThat(baseline.state().aggregate().taskStatus()).isEqualTo("PENDING");
    }

    @AfterEach
    void cleanHermeticFixture() {
        reset(auditEligibilityPolicy);
        dtoSerializationFault.reset();
        auditPersistenceFault.reset();
        DataSource provisionerDataSource = POSTGRES.getPostgresDatabase();
        JdbcTemplate provisioner = new JdbcTemplate(provisionerDataSource);
        new TransactionTemplate(new DataSourceTransactionManager(provisionerDataSource))
                .executeWithoutResult(status -> {
                    provisioner.execute("set local session_replication_role = replica");
                    if (templateVersionId != null) {
                        provisioner.update("delete from checklist_action_commands where task_id in "
                                + "(select checklist_task_instance_id from checklist_task_instances "
                                + "where template_version_id=?)", templateVersionId);
                        provisioner.update("delete from audit_events where template_version_id=?", templateVersionId);
                        provisioner.update("delete from checklist_task_instances where template_version_id=?",
                                templateVersionId);
                        provisioner.update("delete from checklist_instances where template_version_id=?",
                                templateVersionId);
                        provisioner.update("delete from care_item_templates where parent_template_id in "
                                + "(select template_id from care_item_templates where template_version_id=?)",
                                templateVersionId);
                        provisioner.update("delete from care_item_templates where template_version_id=?",
                                templateVersionId);
                    }
                    if (contentAdmin != null && systemAdmin != null && mother != null) {
                        provisioner.update("delete from audit_events where actor_user_id in (?, ?, ?) "
                                        + "or subject_user_id in (?, ?, ?)",
                                contentAdmin, systemAdmin, mother, contentAdmin, systemAdmin, mother);
                    }
                    if (careGroup != null) {
                        provisioner.update("delete from care_groups where care_group_id=?", careGroup);
                    }
                    if (journey != null) {
                        provisioner.update("delete from mother_journeys where journey_id=?", journey);
                    }
                    if (replacementJourney != null) {
                        provisioner.update("delete from mother_journeys where journey_id=?", replacementJourney);
                    }
                    if (subject != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", subject);
                    }
                    if (replacementSubject != null) {
                        provisioner.update("delete from care_subjects where care_subject_id=?", replacementSubject);
                    }
                    for (UUID userId : Stream.of(contentAdmin, systemAdmin, mother)
                            .filter(java.util.Objects::nonNull).toList()) {
                        provisioner.update("delete from users where user_id=?", userId);
                    }
                });
    }

    @ParameterizedTest(name = "{0} rolls back {1}")
    @MethodSource("faultMatrix")
    void requiredAuditFailureRollsBackBusinessStateAndEveryPriorAudit(
            Fault fault,
            Mutation mutation) {
        AtomicLong mutationTransactionId = new AtomicLong();
        armFault(fault, mutation, mutationTransactionId);

        assertThatThrownBy(() -> mutate(mutation))
                .isInstanceOf(RuntimeException.class);
        assertThat(mutationTransactionId.get())
                .as("the injected failure must execute inside the business transaction")
                .isPositive();

        Observation afterFailure = observeInIndependentTransaction();
        assertThat(afterFailure.transactionId())
                .as("the observer must not reuse the rolled-back mutation transaction")
                .isNotEqualTo(mutationTransactionId.get());
        assertThat(afterFailure.state())
                .as("no business row or audit row may survive the required-audit failure")
                .isEqualTo(baseline.state());
        assertThat(afterFailure.state().aggregate().parentStatus()).isEqualTo("PENDING");
        assertThat(afterFailure.state().aggregate().taskStatus()).isEqualTo("PENDING");
        assertThat(afterFailure.state().aggregate().parentCancelledAt()).isNull();
        assertThat(afterFailure.state().aggregate().parentCompletedAt()).isNull();
        assertThat(afterFailure.state().aggregate().taskCancelledAt()).isNull();
        assertThat(afterFailure.state().aggregate().taskCompletedAt()).isNull();
        assertThat(afterFailure.state().aggregate().taskSkippedAt()).isNull();
        assertThat(afterFailure.state().actionCommandCount()).isZero();
    }

    private static Stream<Arguments> faultMatrix() {
        return Stream.of(Fault.values())
                .flatMap(fault -> Stream.of(Mutation.values())
                        .map(mutation -> Arguments.of(fault, mutation)));
    }

    @ParameterizedTest(name = "relink {0} rolls back canonical group and audit")
    @EnumSource(Fault.class)
    void requiredRelinkAuditFailureRollsBackGroupProjectionAndAudit(Fault fault) {
        seedReplacementJourney();
        RelinkObservation baselineRelink = observeRelinkInIndependentTransaction();
        AtomicLong mutationTransactionId = new AtomicLong();
        armRelinkFault(fault, mutationTransactionId);

        assertThatThrownBy(() -> careGroupService.relinkJourney(careGroup, replacementJourney, mother))
                .isInstanceOf(RuntimeException.class);
        assertThat(mutationTransactionId.get()).isPositive();

        RelinkObservation afterFailure = observeRelinkInIndependentTransaction();
        assertThat(afterFailure.transactionId()).isNotEqualTo(mutationTransactionId.get());
        assertThat(afterFailure.transactionId()).isNotEqualTo(baselineRelink.transactionId());
        assertThat(afterFailure.state()).isEqualTo(baselineRelink.state());
        assertThat(afterFailure.state().linkedJourneyId()).isEqualTo(journey);
        assertThat(afterFailure.state().auditCount()).isZero();
    }

    private void armRelinkFault(Fault fault, AtomicLong mutationTransactionId) {
        switch (fault) {
            case POLICY_INELIGIBLE -> doAnswer(invocation -> {
                AuditAction action = invocation.getArgument(0);
                if (action == AuditAction.CARE_GROUP_CONTEXT_RELINKED) {
                    captureMutationTransaction(mutationTransactionId);
                    return false;
                }
                return invocation.callRealMethod();
            }).when(auditEligibilityPolicy).shouldAudit(any(AuditAction.class));
            case DTO_SERIALIZATION -> dtoSerializationFault.armRelink(
                    2, () -> captureMutationTransaction(mutationTransactionId));
            case AUDIT_PERSISTENCE -> auditPersistenceFault.arm(
                    AuditAction.CARE_GROUP_CONTEXT_RELINKED,
                    1,
                    () -> captureMutationTransaction(mutationTransactionId));
        }
    }

    private void seedReplacementJourney() {
        jdbcTemplate.update("update mother_journeys set status='COMPLETED', updated_at=now() where journey_id=?",
                journey);
        replacementSubject = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, person_id, user_id, 'MOTHER', display_name, 'ACTIVE', now(), now()
                  from users where user_id=?
                """, replacementSubject, mother);
        LocalDate startDate = LocalDate.now().minusWeeks(12);
        replacementJourney = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(mother)
                .careSubjectId(replacementSubject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(startDate)
                .lastMenstrualDate(startDate)
                .estimatedDueDate(startDate.plusWeeks(40))
                .build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                replacementJourney, replacementSubject);
        entityManager.clear();
    }

    private String groupRowJson() {
        return jdbcTemplate.queryForObject(
                "select to_jsonb(row)::text from care_groups row where care_group_id=?",
                String.class,
                careGroup);
    }

    private long relinkAuditCount() {
        return jdbcTemplate.queryForObject("""
                select count(*) from audit_events
                 where event_category='CARE_GROUP_CONTEXT_RELINKED' and resource_id=?
                """, Long.class, careGroup);
    }

    private RelinkObservation observeRelinkInIndependentTransaction() {
        TransactionTemplate observer = new TransactionTemplate(transactionManager);
        observer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return observer.execute(status -> {
            entityManager.clear();
            long transactionId = jdbcTemplate.queryForObject("select txid_current()", Long.class);
            RelinkState state = new RelinkState(
                    groupRowJson(),
                    jdbcTemplate.queryForObject(
                            "select linked_journey_id from care_groups where care_group_id=?",
                            UUID.class,
                            careGroup),
                    relinkAuditCount());
            return new RelinkObservation(transactionId, state);
        });
    }

    private void armFault(Fault fault, Mutation mutation, AtomicLong mutationTransactionId) {
        AuditAction targetAction = mutation == Mutation.DISTRIBUTE_OR_CANCEL
                ? AuditAction.CHECKLIST_CANCELLED
                : AuditAction.CHECKLIST_COMPLETED;
        int targetedAuditCall = mutation == Mutation.DISTRIBUTE_OR_CANCEL ? 2 : 1;
        AtomicInteger matchingCalls = new AtomicInteger();

        switch (fault) {
            case POLICY_INELIGIBLE -> doAnswer(invocation -> {
                AuditAction action = invocation.getArgument(0);
                if (action == targetAction && matchingCalls.incrementAndGet() == targetedAuditCall) {
                    captureMutationTransaction(mutationTransactionId);
                    return false;
                }
                return invocation.callRealMethod();
            }).when(auditEligibilityPolicy).shouldAudit(any(AuditAction.class));
            case DTO_SERIALIZATION -> dtoSerializationFault.arm(
                    mutation == Mutation.DISTRIBUTE_OR_CANCEL ? 3 : 1,
                    () -> captureMutationTransaction(mutationTransactionId));
            case AUDIT_PERSISTENCE -> auditPersistenceFault.arm(
                    targetAction,
                    targetedAuditCall,
                    () -> captureMutationTransaction(mutationTransactionId));
        }
    }

    private void captureMutationTransaction(AtomicLong mutationTransactionId) {
        mutationTransactionId.compareAndSet(0L,
                jdbcTemplate.queryForObject("select txid_current()", Long.class));
    }

    private void mutate(Mutation mutation) {
        if (mutation == Mutation.DISTRIBUTE_OR_CANCEL) {
            distributionService.distribute(lifecycleCorrectedCommand());
            return;
        }
        actionFacade.apply(mother, TaskKind.CHECKLIST, taskId,
                new TaskActionRequest(TaskAction.COMPLETE, UUID.randomUUID(), null));
    }

    private ChecklistDistributionCommand lifecycleCorrectedCommand() {
        ChecklistLifecycleDates dates = initialCommand.lifecycleDates();
        ChecklistLifecycleDates correctedDates = new ChecklistLifecycleDates(
                dates.lastMenstrualDate().plusDays(1),
                dates.estimatedDueDate().plusDays(1),
                dates.deliveryDate(),
                dates.birthDate());
        return new ChecklistDistributionCommand(
                initialCommand.templateLineageId(),
                initialCommand.templateVersionId(),
                initialCommand.careGroupId(),
                initialCommand.careGroupOwnerUserId(),
                initialCommand.contextType(),
                initialCommand.contextId(),
                initialCommand.contextOwnerUserId(),
                initialCommand.stage(),
                initialCommand.substage(),
                correctedDates,
                initialCommand.effectiveDate(),
                initialCommand.timezone(),
                initialCommand.recipients(),
                initialCommand.items(),
                UUID.randomUUID());
    }

    private Observation observeInIndependentTransaction() {
        TransactionTemplate observer = new TransactionTemplate(transactionManager);
        observer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return observer.execute(status -> {
            entityManager.clear();
            long transactionId = jdbcTemplate.queryForObject("select txid_current()", Long.class);
            UUID parentId = jdbcTemplate.queryForObject(
                    "select checklist_instance_id from checklist_instances where template_version_id=?",
                    UUID.class, templateVersionId);
            AggregateState aggregate = jdbcTemplate.queryForObject("""
                    select parent.status, parent.lock_version, parent.cancelled_at, parent.completed_at,
                           task.status, task.lock_version, task.cancelled_at, task.completed_at, task.skipped_at
                      from checklist_instances parent
                      join checklist_task_instances task
                        on task.checklist_instance_id=parent.checklist_instance_id
                     where parent.checklist_instance_id=? and task.checklist_task_instance_id=?
                    """, (resultSet, rowNumber) -> new AggregateState(
                    resultSet.getString(1),
                    resultSet.getLong(2),
                    resultSet.getObject(3),
                    resultSet.getObject(4),
                    resultSet.getString(5),
                    resultSet.getLong(6),
                    resultSet.getObject(7),
                    resultSet.getObject(8),
                    resultSet.getObject(9)), parentId, taskId);
            DatabaseState databaseState = new DatabaseState(
                    aggregate,
                    count("select count(*) from checklist_instances where template_version_id=?",
                            templateVersionId),
                    count("select count(*) from checklist_task_instances where template_version_id=?",
                            templateVersionId),
                    count("select count(*) from checklist_action_commands where task_id=?", taskId),
                    count("select count(*) from audit_events where template_version_id=?", templateVersionId));
            return new Observation(transactionId, databaseState);
        });
    }

    private long count(String sql, UUID id) {
        return jdbcTemplate.queryForObject(sql, Long.class, id);
    }

    private void seedCanonicalJourneyContext() {
        LocalDate effectiveDate = jdbcTemplate.queryForObject("select current_date", LocalDate.class);
        LocalDate lastMenstrualDate = effectiveDate.minusWeeks(8);
        contentAdmin = UUID.randomUUID();
        systemAdmin = UUID.randomUUID();
        mother = UUID.randomUUID();
        insertUser(contentAdmin, "CHK-038 Content Admin", "CONTENT_ADMIN");
        insertUser(systemAdmin, "CHK-038 System Admin", "SYSTEM_ADMIN");
        insertUser(mother, "CHK-038 Mother", "MOTHER");

        subject = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, person_id, user_id, 'MOTHER', display_name, 'ACTIVE', now(), now()
                  from users where user_id=?
                """, subject, mother);
        journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(mother)
                .careSubjectId(subject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(lastMenstrualDate)
                .lastMenstrualDate(lastMenstrualDate)
                .estimatedDueDate(lastMenstrualDate.plusWeeks(40))
                .build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?", journey, subject);
        careGroup = careGroupRepository.saveAndFlush(CareGroup.builder()
                .ownerUserId(mother)
                .groupName("CHK-038 Care Group")
                .status(CareGroupStatus.ACTIVE)
                .linkedJourneyId(journey)
                .build()).getId();
    }

    private TemplateIds createAndApproveTemplate() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "CHK-038 atomicity checklist",
                "description", "PostgreSQL audit rollback fixture",
                "recipientRoles", List.of("MOTHER"),
                "stage", "PREGNANCY",
                "substage", Map.of(
                        "code", "PREGNANCY_LMP_WEEK_0_12",
                        "anchor", "LMP",
                        "startInclusive", 0,
                        "endInclusive", 12,
                        "unit", "WEEK"),
                "items", List.of(Map.of(
                        "itemText", "CHK-038 pending task",
                        "order", 1,
                        "isRequired", true,
                        "targetSubject", "MOTHER"))));

        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/checklist-templates")
                        .with(csrf()).with(user(contentAdmin.toString()).roles("CONTENT_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        UUID lineageId = UUID.fromString(created.path("data").path("lineageId").asText());
        UUID versionId = UUID.fromString(created.path("data").path("versionId").asText());

        mockMvc.perform(post("/api/v1/admin/checklist-templates/{lineage}/versions/{version}/approve",
                        lineageId, versionId)
                        .with(csrf()).with(user(systemAdmin.toString()).roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk());
        UUID itemId = jdbcTemplate.queryForObject("""
                select template_id from care_item_templates
                 where parent_template_id=(
                     select template_id from care_item_templates where template_version_id=?)
                """, UUID.class, versionId);
        return new TemplateIds(lineageId, versionId, itemId);
    }

    private void insertUser(UUID id, String name, String role) {
        CanonicalUserFixture.insertUser(
                jdbcTemplate, id, name,
                "09" + Math.floorMod(id.getLeastSignificantBits(), 100_000_000L), role);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuditFaultTestConfiguration {

        @Bean
        DtoSerializationFault dtoSerializationFault() {
            return new DtoSerializationFault();
        }

        @Bean
        AuditPersistenceFault auditPersistenceFault() {
            return new AuditPersistenceFault();
        }

        @Bean
        @Primary
        ChecklistAuditWriter chk038ChecklistAuditWriter(
                AuditLogRepository repository,
                AuditEligibilityPolicy policy,
                ObjectMapper objectMapper,
                DtoSerializationFault dtoFault,
                AuditPersistenceFault persistenceFault) {
            return new ChecklistAuditWriter(
                    persistenceFault.decorate(repository),
                    policy,
                    new FaultInjectingAuditObjectMapper(objectMapper, dtoFault));
        }

        @Bean
        @Primary
        AuditService chk038RequiredAuditService(
                AuditLogRepository repository,
                AuditLogMapper mapper,
                AuditEligibilityPolicy policy,
                ObjectMapper objectMapper,
                DtoSerializationFault dtoFault,
                AuditPersistenceFault persistenceFault) {
            return new AuditServiceImpl(
                    persistenceFault.decorate(repository),
                    mapper,
                    policy,
                    new FaultInjectingAuditObjectMapper(objectMapper, dtoFault));
        }
    }

    private static final class FaultInjectingAuditObjectMapper extends ObjectMapper {
        private final ObjectMapper delegate;
        private final DtoSerializationFault fault;

        private FaultInjectingAuditObjectMapper(ObjectMapper delegate, DtoSerializationFault fault) {
            this.delegate = delegate;
            this.fault = fault;
        }

        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            fault.failIfArmed(value);
            return delegate.writeValueAsString(value);
        }
    }

    static final class DtoSerializationFault {
        private final AtomicInteger matchingCalls = new AtomicInteger();
        private volatile int targetedCall;
        private volatile Runnable onFailure;
        private volatile boolean relinkPayload;

        void arm(int targetedCall, Runnable onFailure) {
            this.matchingCalls.set(0);
            this.targetedCall = targetedCall;
            this.onFailure = onFailure;
            this.relinkPayload = false;
        }

        void armRelink(int targetedCall, Runnable onFailure) {
            this.matchingCalls.set(0);
            this.targetedCall = targetedCall;
            this.onFailure = onFailure;
            this.relinkPayload = true;
        }

        void reset() {
            this.matchingCalls.set(0);
            this.targetedCall = 0;
            this.onFailure = null;
            this.relinkPayload = false;
        }

        void failIfArmed(Object value) throws JsonProcessingException {
            if (!(value instanceof Map<?, ?> payload)
                    || !matchesPayload(payload)
                    || targetedCall <= 0
                    || matchingCalls.incrementAndGet() != targetedCall) {
                return;
            }
            Runnable callback = onFailure;
            targetedCall = 0;
            onFailure = null;
            callback.run();
            throw new JsonProcessingException("synthetic CHK-038 audit DTO serialization failure") {
            };
        }

        private boolean matchesPayload(Map<?, ?> payload) {
            if (relinkPayload) {
                return payload.size() == 2
                        && payload.containsKey("careContextType")
                        && payload.containsKey("careContextId");
            }
            return payload.size() == 1 && payload.containsKey("status");
        }
    }

    static final class AuditPersistenceFault {
        private final AtomicInteger matchingCalls = new AtomicInteger();
        private volatile AuditAction targetAction;
        private volatile int targetedCall;
        private volatile Runnable onFailure;

        AuditLogRepository decorate(AuditLogRepository delegate) {
            return (AuditLogRepository) Proxy.newProxyInstance(
                    AuditLogRepository.class.getClassLoader(),
                    new Class<?>[]{AuditLogRepository.class},
                    (proxy, method, arguments) -> {
                        if ("save".equals(method.getName())
                                && arguments != null
                                && arguments.length == 1
                                && arguments[0] instanceof AuditLog audit) {
                            failIfArmed(audit);
                        }
                        try {
                            return method.invoke(delegate, arguments);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
        }

        void arm(AuditAction targetAction, int targetedCall, Runnable onFailure) {
            this.matchingCalls.set(0);
            this.targetAction = targetAction;
            this.targetedCall = targetedCall;
            this.onFailure = onFailure;
        }

        void reset() {
            this.matchingCalls.set(0);
            this.targetAction = null;
            this.targetedCall = 0;
            this.onFailure = null;
        }

        private void failIfArmed(AuditLog audit) {
            if (audit.getAction() != targetAction
                    || targetedCall <= 0
                    || matchingCalls.incrementAndGet() != targetedCall) {
                return;
            }
            Runnable callback = onFailure;
            targetAction = null;
            targetedCall = 0;
            onFailure = null;
            callback.run();
            throw new IllegalStateException("synthetic CHK-038 audit persistence failure");
        }
    }

    private enum Fault {
        POLICY_INELIGIBLE,
        DTO_SERIALIZATION,
        AUDIT_PERSISTENCE
    }

    private enum Mutation {
        DISTRIBUTE_OR_CANCEL,
        COMPLETE_ACTION
    }

    private record AggregateState(
            String parentStatus,
            long parentLockVersion,
            Object parentCancelledAt,
            Object parentCompletedAt,
            String taskStatus,
            long taskLockVersion,
            Object taskCancelledAt,
            Object taskCompletedAt,
            Object taskSkippedAt) {
    }

    private record DatabaseState(
            AggregateState aggregate,
            long parentCount,
            long taskCount,
            long actionCommandCount,
            long auditCount) {
    }

    private record RelinkObservation(long transactionId, RelinkState state) {
    }

    private record RelinkState(
            String groupRowJson,
            UUID linkedJourneyId,
            long auditCount) {
    }

    private record TemplateIds(UUID lineageId, UUID versionId, UUID itemId) {
    }

    private record Observation(long transactionId, DatabaseState state) {
    }
}
