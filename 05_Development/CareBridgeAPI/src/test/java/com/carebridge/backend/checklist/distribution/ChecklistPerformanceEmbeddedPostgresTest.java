package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Opt-in CHK-041 reference-volume harness against real PostgreSQL 18.1.
 *
 * <p>Run with {@code -Dchecklist.performance.enabled=true}. The ordinary PR suite skips this
 * production-volume gate because it exercises a sustained concurrent Today workload.
 */
@EnabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named = "checklist.performance.enabled", matches = "true")
@TestMethodOrder(OrderAnnotation.class)
@TestPropertySource(properties = {
        "logging.level.org.hibernate.SQL=INFO",
        "logging.level.org.hibernate.type.descriptor.sql=INFO",
        "logging.level.org.springframework.orm=INFO",
        "logging.level.com.carebridge.backend=INFO"
})
class ChecklistPerformanceEmbeddedPostgresTest extends AbstractEmbeddedPostgresIntegrationTest {

    private static final int GROUP_COUNT = 20;
    private static final int TASK_COUNT = 500;
    private static final int REQUEST_COUNT = 250;
    private static final int OFFERED_RPS = 50;
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 7, 29);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CareGroupRepository careGroupRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @Timeout(180)
    void todaySustainsFiftyRpsWithFiveHundredTasksAcrossTwentyGroups() throws Exception {
        UUID actor = seedTodayReferenceVolume();
        for (int warmup = 0; warmup < 5; warmup++) {
            TimedResponse response = invokeToday(actor, System.nanoTime());
            assertThat(response.status()).isEqualTo(200);
            assertReferenceVolume(response.body());
        }

        var scheduler = Executors.newScheduledThreadPool(OFFERED_RPS);
        List<java.util.concurrent.ScheduledFuture<TimedResponse>> futures = new ArrayList<>();
        long harnessStart = System.nanoTime();
        for (int request = 0; request < REQUEST_COUNT; request++) {
            long delayNanos = TimeUnit.SECONDS.toNanos(request) / OFFERED_RPS;
            long scheduledAt = harnessStart + delayNanos;
            futures.add(scheduler.schedule((Callable<TimedResponse>) () -> invokeToday(actor, scheduledAt),
                    delayNanos, TimeUnit.NANOSECONDS));
        }

        List<TimedResponse> responses = new ArrayList<>(REQUEST_COUNT);
        try {
            for (var future : futures) {
                responses.add(future.get(120, TimeUnit.SECONDS));
            }
        } finally {
            scheduler.shutdownNow();
        }
        long harnessEnd = System.nanoTime();

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(200);
            assertReferenceVolume(response.body());
        });
        List<Long> latencies = responses.stream()
                .map(TimedResponse::latencyNanos)
                .sorted(Comparator.naturalOrder())
                .toList();
        List<Long> serviceLatencies = responses.stream()
                .map(TimedResponse::serviceNanos)
                .sorted(Comparator.naturalOrder())
                .toList();
        List<Long> dispatchLags = responses.stream()
                .map(response -> response.startedNanos() - response.scheduledNanos())
                .sorted(Comparator.naturalOrder())
                .toList();
        long p95 = percentile(latencies, 0.95);
        long p99 = percentile(latencies, 0.99);
        long serviceP95 = percentile(serviceLatencies, 0.95);
        long serviceP99 = percentile(serviceLatencies, 0.99);
        long dispatchP99 = percentile(dispatchLags, 0.99);
        long firstStart = responses.stream().mapToLong(TimedResponse::startedNanos).min().orElseThrow();
        long lastStart = responses.stream().mapToLong(TimedResponse::startedNanos).max().orElseThrow();
        double actualStartRps = (REQUEST_COUNT - 1) /
                (Duration.ofNanos(lastStart - firstStart).toNanos() / 1_000_000_000.0d);
        double completedThroughput = REQUEST_COUNT /
                (Duration.ofNanos(harnessEnd - harnessStart).toNanos() / 1_000_000_000.0d);

        System.out.printf(
                "CHK-041 Today: tasks=%d groups=%d requests=%d offeredRps=%d actualStartRps=%.2f "
                        + "completedRps=%.2f endToEndP95Ms=%.2f endToEndP99Ms=%.2f "
                        + "serviceP95Ms=%.2f serviceP99Ms=%.2f dispatchP99Ms=%.2f%n",
                TASK_COUNT, GROUP_COUNT, REQUEST_COUNT, OFFERED_RPS, actualStartRps,
                completedThroughput, nanosToMillis(p95), nanosToMillis(p99),
                nanosToMillis(serviceP95), nanosToMillis(serviceP99), nanosToMillis(dispatchP99));

        assertThat(actualStartRps)
                .as("scheduler start rate must stay within 2%% of the 50 RPS offer")
                .isGreaterThanOrEqualTo(OFFERED_RPS * 0.98d);
        assertThat(p95).as("Today p95 at CHK-041 reference volume")
                .isLessThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(500));
        assertThat(p99).as("Today p99 at CHK-041 reference volume")
                .isLessThanOrEqualTo(TimeUnit.SECONDS.toNanos(1));
    }

    private UUID seedTodayReferenceVolume() {
        UUID actor = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, actor, "CHK-041 Mother",
                "09" + Math.floorMod(actor.getLeastSignificantBits(), 100_000_000L), "MOTHER");
        UUID subject = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', u.display_name, 'ACTIVE', now(), now()
                  from users u where u.user_id=?
                """, subject, actor);
        UUID journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(actor)
                .careSubjectId(subject)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(EFFECTIVE_DATE.minusDays(60))
                .lastMenstrualDate(EFFECTIVE_DATE.minusDays(60))
                .estimatedDueDate(EFFECTIVE_DATE.plusDays(220))
                .build()).getId();
        jdbcTemplate.update("update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey, subject);

        for (int groupIndex = 0; groupIndex < GROUP_COUNT; groupIndex++) {
            CareGroup group = careGroupRepository.saveAndFlush(CareGroup.builder()
                    .ownerUserId(actor)
                    .groupName("CHK-041 Group " + groupIndex)
                    .status(CareGroupStatus.ACTIVE)
                    .linkedJourneyId(journey)
                    .build());
            UUID parent = UUID.randomUUID();
            jdbcTemplate.update("""
                    insert into checklist_instances
                        (checklist_instance_id, distribution_key, key_version,
                         recipient_user_id, recipient_role, care_group_id,
                         care_context_type, care_context_id, context_owner_user_id,
                         origin, status, created_at, updated_at)
                    values (?, ?, 'v1', ?, 'MOTHER', ?, 'JOURNEY', ?, ?,
                            'USER_CREATED', 'PENDING', now(), now())
                    """, parent, sha256("parent:" + parent), actor, group.getId(), journey, actor);
            int firstTask = groupIndex * (TASK_COUNT / GROUP_COUNT);
            for (int offset = 0; offset < TASK_COUNT / GROUP_COUNT; offset++) {
                int displayOrder = firstTask + offset;
                UUID task = UUID.randomUUID();
                jdbcTemplate.update("""
                        insert into checklist_task_instances
                            (checklist_task_instance_id, checklist_instance_id, task_key, key_version,
                             title_snapshot, display_order, is_required, target_subject, due_at,
                             status, created_at, updated_at)
                        values (?, ?, ?, 'v1', ?, ?, false, 'MOTHER',
                                cast(? as date)::timestamp at time zone 'Asia/Ho_Chi_Minh',
                                'PENDING', now(), now())
                        """, task, parent, sha256("task:" + task), "CHK-041 task " + displayOrder,
                        displayOrder, EFFECTIVE_DATE);
            }
        }
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from checklist_instances where recipient_user_id=?",
                Integer.class, actor)).isEqualTo(GROUP_COUNT);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from checklist_task_instances task
                join checklist_instances parent
                  on parent.checklist_instance_id=task.checklist_instance_id
                where parent.recipient_user_id=?
                """, Integer.class, actor)).isEqualTo(TASK_COUNT);
        return actor;
    }

    private TimedResponse invokeToday(UUID actor, long scheduledAtNanos) throws Exception {
        long started = System.nanoTime();
        var response = mockMvc.perform(get("/api/v1/tasks/today")
                        .param("date", EFFECTIVE_DATE.toString())
                        .header("X-User-Timezone", "Asia/Ho_Chi_Minh")
                        .with(user(actor.toString()).roles("MOTHER")))
                .andReturn().getResponse();
        long completed = System.nanoTime();
        return new TimedResponse(scheduledAtNanos, started, completed - scheduledAtNanos,
                completed - started, response.getStatus(), response.getContentAsString());
    }

    private void assertReferenceVolume(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<JsonNode> tasks = new ArrayList<>();
            root.path("sections").fields().forEachRemaining(entry -> entry.getValue().forEach(tasks::add));
            assertThat(tasks).as("Today must return the seeded reference volume").hasSize(TASK_COUNT);
            var groupIds = tasks.stream()
                    .map(task -> task.path("careGroupId").asText())
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(groupIds).doesNotContain("").hasSize(GROUP_COUNT);
        } catch (Exception exception) {
            throw new AssertionError("Today response was not valid JSON", exception);
        }
    }

    private static long percentile(List<Long> sorted, double quantile) {
        int index = Math.max(0, (int) Math.ceil(sorted.size() * quantile) - 1);
        return sorted.get(index);
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0d;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private record TimedResponse(long scheduledNanos, long startedNanos, long latencyNanos,
                                 long serviceNanos, int status, String body) {
    }
}
