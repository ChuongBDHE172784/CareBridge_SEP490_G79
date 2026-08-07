package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentSource;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import java.time.Instant;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Real PostgreSQL/Flyway lifecycle and migration-preflight evidence for INT-004/005. */
class LifecycleContentPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ContentService contentService;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ChecklistTemplateRepository templateRepository;
    @Autowired private ChecklistItemRepository checklistItemRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private UUID motherId;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        wipeStoryFixtures();
        motherId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, motherId, "Story 69 Lifecycle Mother", uniquePhone(), "MOTHER");
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', u.display_name,
                       'ACTIVE', now(), now()
                  from users u
                 where u.user_id = ?
                """, careSubjectId, motherId);
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(motherId)
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .build());
        journeyId = journey.getId();
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journeyId, careSubjectId);
    }

    @AfterEach
    void cleanUp() {
        wipeStoryFixtures();
    }

    @Test
    void uc82_69_int_004_eachReadReResolvesSameJourneyAndFiltersApprovedCanonicalStage() {
        seedContent("Approved PRE", ContentStage.PRE_PREGNANCY, ContentStatus.APPROVED);
        seedContent("Denied PRE", ContentStage.PRE_PREGNANCY, ContentStatus.DRAFT);
        seedContent("Approved PREG", ContentStage.PREGNANCY, ContentStatus.APPROVED);
        seedContent("Denied PREG", ContentStage.PREGNANCY, ContentStatus.ARCHIVED);

        var before = contentService.getLifecycleContents(
                motherId, ContentType.ARTICLE, null, PageRequest.of(0, 20));
        assertThat(before.stage()).isEqualTo(ContentStage.PRE_PREGNANCY);
        assertThat(before.payload().getContent())
                .extracting(item -> item.getTitle())
                .containsExactly("Approved PRE");

        int changed = jdbcTemplate.update(
                "update mother_journeys set journey_type='PREGNANCY', version=version+1, updated_at=now() "
                        + "where journey_id=? and status='ACTIVE'",
                journeyId);
        assertThat(changed).isOne();

        var after = contentService.getLifecycleContents(
                motherId, ContentType.ARTICLE, null, PageRequest.of(0, 20));
        assertThat(after.stage()).isEqualTo(ContentStage.PREGNANCY);
        assertThat(after.payload().getContent())
                .extracting(item -> item.getTitle())
                .containsExactly("Approved PREG");
        assertThat(jdbcTemplate.queryForObject(
                        "select journey_id from mother_journeys where owner_user_id=? and status='ACTIVE'",
                        UUID.class, motherId))
                .isEqualTo(journeyId);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from mother_journeys where owner_user_id=? and status='ACTIVE'",
                        Long.class, motherId))
                .isEqualTo(1L);
    }

    @Test
    // R69-027 retained PostgreSQL evidence start
    void uc82_69_int_005_realPostgresPersistsFiveStatusesAndDeterministicCardinality() {
        for (ChecklistTemplateStatus status : ChecklistTemplateStatus.values()) {
            seedTemplate("Story 69 " + status,
                    status == ChecklistTemplateStatus.APPROVED
                            ? ContentStage.PREGNANCY : ContentStage.PRE_PREGNANCY,
                    status);
        }

        Set<String> persistedStatuses = jdbcTemplate.queryForList(
                        "select distinct content_status from care_item_templates "
                                + "where entry_type='TEMPLATE_ROOT' order by content_status",
                        String.class)
                .stream().collect(Collectors.toSet());
        Set<String> allowedStatuses = Arrays.stream(ChecklistTemplateStatus.values())
                .map(Enum::name).collect(Collectors.toSet());
        assertThat(persistedStatuses).isEqualTo(allowedStatuses);
        assertThat(EnumSet.allOf(ChecklistTemplateStatus.class)).hasSize(5);

        var statusColumn = jdbcTemplate.queryForMap(
                "select data_type, character_maximum_length from information_schema.columns "
                        + "where table_schema='public' and table_name='care_item_templates' "
                        + "and column_name='content_status'");
        assertThat(statusColumn.get("data_type")).isEqualTo("character varying");
        assertThat(((Number) statusColumn.get("character_maximum_length")).intValue()).isEqualTo(20);
        // R69 kept content_status an open varchar so a sixth status needs no migration.
        // The original proxy for that — "no CHECK mentions content_status at all" — stopped
        // holding when V20260731070000 added three approval/distribution gates that
        // reference the column without restricting which statuses may be stored. The
        // guarantee itself is still asserted above (all five statuses persisted); what is
        // re-checked here is the narrower, still-true property it stood for.
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from pg_constraint where conrelid='public.care_item_templates'::regclass "
                                + "and contype='c' and pg_get_constraintdef(oid) ilike '%content_status in (%'",
                        Long.class))
                .isZero();

        List<String> cardinality = jdbcTemplate.query(
                "select stage || '|' || content_status || '|' || count(*) "
                        + "from care_item_templates where entry_type='TEMPLATE_ROOT' "
                        + "group by stage, content_status order by stage, content_status",
                (row, index) -> row.getString(1));

        assertThat(cardinality).hasSize(5);
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from flyway_schema_history where success=false", Long.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from flyway_schema_history "
                                + "where version='20260727010000' and success=true", Long.class))
                .isEqualTo(1L);
        assertThat(Files.exists(Path.of(
                        "src/main/resources/db/migration/"
                                + "V20260723140000__optimize_approved_checklist_stage_lookup.sql")))
                .isFalse();

        System.out.println("STORY69-PG allowed_statuses=" + String.join(",", persistedStatuses.stream().sorted().toList()));
        System.out.println("STORY69-PG cardinality=" + String.join(",", cardinality));
        System.out.println("STORY69-PG migration_decision=no-new-migration");
    }
    // R69-027 retained PostgreSQL evidence end

    @Test
    void r69_027_retainedPostgresEvidenceEmitsAndReliesOnNoPlannerPayload() throws Exception {
        String source = Files.readString(Path.of(
                "src/test/java/com/carebridge/backend/content/integration/"
                        + "LifecycleContentPostgresIntegrationTest.java"));
        String startMarker = "// R69-027 retained PostgreSQL evidence start";
        String endMarker = "// R69-027 retained PostgreSQL evidence end";
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());

        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        assertThat(source.substring(start, end).toLowerCase())
                .doesNotContain(
                        "explain", "_plan=", "actual time", "planning time", "execution time",
                        "buffers", "cost=", "rows=", "width=");
    }

    @Test
    void uc82_69_tc_002_genericChecklistHttpUsesExactlyTwoBoundedPostgresQueries()
            throws Exception {
        ChecklistTemplate approvedA = seedDraft("Approved A", ContentStage.PREGNANCY);
        ChecklistTemplate approvedB = seedDraft("Approved B", ContentStage.PREGNANCY);
        for (ChecklistTemplateStatus status : ChecklistTemplateStatus.values()) {
            if (status != ChecklistTemplateStatus.APPROVED) {
                seedTemplate("Denied " + status, ContentStage.PREGNANCY, status);
            }
        }
        ChecklistTemplate approvedOtherStage = seedDraft("Approved POST", ContentStage.POSTPARTUM);
        seedChecklistItem(approvedA, "A first", 1);
        seedChecklistItem(approvedA, "A highest-order", Integer.MAX_VALUE);
        seedChecklistItem(approvedB, "B only", 3);
        seedChecklistItem(approvedOtherStage, "Wrong stage", 1);
        // Approval comes last: entries cannot be attached to an approved root.
        promote(approvedA, ChecklistTemplateStatus.APPROVED);
        promote(approvedB, ChecklistTemplateStatus.APPROVED);
        promote(approvedOtherStage, ChecklistTemplateStatus.APPROVED);
        entityManager.clear();

        Logger sqlLogger = (Logger) LoggerFactory.getLogger("org.hibernate.SQL");
        Level previousLevel = sqlLogger.getLevel();
        ListAppender<ILoggingEvent> sqlAppender = new ListAppender<>();
        sqlAppender.start();
        sqlLogger.setLevel(Level.DEBUG);
        sqlLogger.addAppender(sqlAppender);
        String requestThread = Thread.currentThread().getName();
        String body;
        try (var backgroundExecutor = Executors.newSingleThreadExecutor()) {
            backgroundExecutor.submit(() -> templateRepository.count()).get();
            body = mockMvc.perform(get("/api/v1/content/checklists")
                            .param("stage", "PREGNANCY")
                            .with(user(motherId.toString()).roles("MOTHER")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> requestSql = sqlAppender.list.stream()
                    .filter(event -> requestThread.equals(event.getThreadName()))
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(sqlAppender.list)
                    .anySatisfy(event -> assertThat(event.getThreadName())
                            .isNotEqualTo(requestThread));
            assertThat(requestSql)
                    .as("one APPROVED template query plus one approved-parent item query")
                    .hasSize(2);
            assertThat(requestSql.stream()
                    .map(statement -> statement.toLowerCase().replaceAll("\\s+", " "))
                    .filter(statement -> statement.contains("from care_item_templates")))
                    .hasSize(2);
        } finally {
            sqlLogger.detachAppender(sqlAppender);
            sqlLogger.setLevel(previousLevel);
            sqlAppender.stop();
        }

        JsonNode rows = objectMapper.readTree(body).path("data");
        assertThat(rows.isArray()).isTrue();
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.path("stage").asText()).isEqualTo("PREGNANCY");
            assertThat(row.has("status")).isFalse();
            assertThat(row.path("id").asText())
                    .isIn(approvedA.getId().toString(), approvedB.getId().toString());
        });
        JsonNode approvedARow = StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> row.path("id").asText().equals(approvedA.getId().toString()))
                .findFirst().orElseThrow();
        assertThat(approvedARow.path("items")).hasSize(2);
        assertThat(approvedARow.path("items").get(0).path("order").asInt()).isEqualTo(1);
        assertThat(approvedARow.path("items").get(1).path("order").asInt())
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void uc82_69_tc_003_genericChecklistWithoutStageReturnsApprovedAcrossStagesOnly()
            throws Exception {
        ChecklistTemplate approvedPre = seedDraft("Approved PRE generic", ContentStage.PRE_PREGNANCY);
        ChecklistTemplate approvedPost = seedDraft("Approved POST generic", ContentStage.POSTPARTUM);
        seedChecklistItem(approvedPre, "PRE approved item", 1);
        seedChecklistItem(approvedPost, "POST approved item", 2);
        // Approval comes last: entries cannot be attached to an approved root.
        promote(approvedPre, ChecklistTemplateStatus.APPROVED);
        promote(approvedPost, ChecklistTemplateStatus.APPROVED);
        for (ChecklistTemplateStatus status : ChecklistTemplateStatus.values()) {
            if (status != ChecklistTemplateStatus.APPROVED) {
                ChecklistTemplate denied = seedDraft("Denied generic " + status, ContentStage.PREGNANCY);
                seedChecklistItem(denied, "Denied item " + status, 1);
                promote(denied, status);
            }
        }
        entityManager.clear();

        String body = mockMvc.perform(get("/api/v1/content/checklists")
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rows = objectMapper.readTree(body).path("data");
        assertThat(rows).hasSize(2);
        assertThat(StreamSupport.stream(rows.spliterator(), false)
                .map(row -> row.path("name").asText()).toList())
                .containsExactlyInAnyOrder("Approved PRE generic", "Approved POST generic");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.has("status")).isFalse();
            assertThat(row.path("items")).hasSize(1);
            assertThat(row.toString()).doesNotContain("Denied generic", "Denied item");
        });
    }

    @Test
    void uc82_69_tc_006_trulyMissingLifecycleDetailReturnsExactNeutralErrorSchema()
            throws Exception {
        UUID missingId = UUID.fromString("69000000-0000-0000-0000-000000000699");
        String path = "/api/v1/content/lifecycle/" + missingId;

        String body = mockMvc.perform(get(path)
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        JsonNode error = objectMapper.readTree(body);
        assertThat(error.path("success").asBoolean()).isFalse();
        assertThat(error.path("status").asInt()).isEqualTo(404);
        assertThat(error.path("error").asText()).isEqualTo("CNT-003");
        assertThat(error.path("message").asText())
                .isEqualTo("Content not found or not available");
        assertThat(error.path("path").asText()).isEqualTo(path);
        assertThat(error.path("details").isNull()).isTrue();
        assertThat(error.path("timestamp").asText()).isNotBlank();
        assertThat(body).doesNotContain("authorUserId", "review", "PREGNANCY");
    }

    @Test
    void uc82_69_tc_008_genericBrowseHonorsExplicitCrossStageInsteadOfMotherLifecycle()
            throws Exception {
        seedContent("Canonical PRE article", ContentStage.PRE_PREGNANCY, ContentStatus.APPROVED);
        seedContent("Explicit POST article", ContentStage.POSTPARTUM, ContentStatus.APPROVED);
        seedContent("Denied POST draft", ContentStage.POSTPARTUM, ContentStatus.DRAFT);
        entityManager.clear();

        String body = mockMvc.perform(get("/api/v1/content")
                        .param("type", "ARTICLE")
                        .param("stage", "POSTPARTUM")
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rows = objectMapper.readTree(body).path("data");
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.path("title").asText()).isEqualTo("Explicit POST article");
            assertThat(row.path("stage").asText()).isEqualTo("POSTPARTUM");
        });
        assertThat(body).doesNotContain("Canonical PRE article", "Denied POST draft", "lifecycle");
    }

    @Test
    void uc82_69_tc_014_adminChecklistUsesPersistedStatusAndItemCount() throws Exception {
        ChecklistTemplate pending = seedTemplate(
                "Persisted pending checklist", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.PENDING_REVIEW);
        pending.setDescription("Persisted review description");
        pending.setVersionNo(7);
        templateRepository.saveAndFlush(pending);
        seedChecklistItem(pending, "Persisted item one", 1);
        seedChecklistItem(pending, "Persisted item two", 2);
        seedTemplate("Other approved checklist", ContentStage.PREGNANCY,
                ChecklistTemplateStatus.APPROVED);
        entityManager.clear();

        String body = mockMvc.perform(get("/api/v1/admin/content/checklists")
                        .param("stage", "PREGNANCY")
                        .param("status", "PENDING_REVIEW")
                        .with(user(motherId.toString()).roles("CONTENT_ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rows = objectMapper.readTree(body).path("data");
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.path("id").asText()).isEqualTo(pending.getId().toString());
            assertThat(row.path("status").asText()).isEqualTo("PENDING_REVIEW");
            assertThat(row.path("itemCount").asLong()).isEqualTo(2L);
            assertThat(row.path("versionNo").asInt()).isEqualTo(7);
        });
        assertThat(body).doesNotContain("Persisted item one", "Persisted item two", "\"items\"");
    }

    @Test
    void uc82_69_tc_018_019_lifecycleTypesTopicsPagingOrderAndRealEmptyEnvelope()
            throws Exception {
        assertThat(jdbcTemplate.update(
                "update mother_journeys set journey_type='PREGNANCY', updated_at=now() "
                        + "where journey_id=?", journeyId)).isOne();
        UUID topicA = UUID.fromString("69000000-0000-0000-0000-000000000701");
        UUID topicB = UUID.fromString("69000000-0000-0000-0000-000000000702");
        Instant base = Instant.parse("2026-07-23T00:00:00Z");
        seedContent("Article newest", ContentType.ARTICLE, ContentStage.PREGNANCY,
                ContentStatus.APPROVED, topicA, base.plusSeconds(30));
        seedContent("Article older", ContentType.ARTICLE, ContentStage.PREGNANCY,
                ContentStatus.APPROVED, topicA, base.plusSeconds(10));
        seedContent("FAQ topic A", ContentType.FAQ, ContentStage.PREGNANCY,
                ContentStatus.APPROVED, topicA, base.plusSeconds(20));
        seedContent("Checklist topic A", ContentType.CHECKLIST, ContentStage.PREGNANCY,
                ContentStatus.APPROVED, topicA, base.plusSeconds(15));
        seedContent("FAQ topic B", ContentType.FAQ, ContentStage.PREGNANCY,
                ContentStatus.APPROVED, topicB, base.plusSeconds(25));
        seedContent("Denied PREG FAQ", ContentType.FAQ, ContentStage.PREGNANCY,
                ContentStatus.DRAFT, topicA, null);
        seedContent("Wrong-stage FAQ", ContentType.FAQ, ContentStage.POSTPARTUM,
                ContentStatus.APPROVED, topicA, base.plusSeconds(40));
        entityManager.clear();

        for (ContentType type : List.of(
                ContentType.ARTICLE, ContentType.FAQ, ContentType.CHECKLIST)) {
            var result = contentService.getLifecycleContents(
                    motherId, type, topicA,
                    PageRequest.of(0, 10, Sort.by("publishedAt").descending()));
            assertThat(result.stage()).isEqualTo(ContentStage.PREGNANCY);
            assertThat(result.payload()).allSatisfy(row -> {
                assertThat(row.getType()).isEqualTo(type);
                assertThat(row.getStage()).isEqualTo(ContentStage.PREGNANCY);
                assertThat(row.getTopicId()).isEqualTo(topicA);
            });
        }

        var articlePage0 = contentService.getLifecycleContents(
                motherId, ContentType.ARTICLE, topicA,
                PageRequest.of(0, 1, Sort.by("publishedAt").descending()));
        var articlePage1 = contentService.getLifecycleContents(
                motherId, ContentType.ARTICLE, topicA,
                PageRequest.of(1, 1, Sort.by("publishedAt").descending()));
        assertThat(articlePage0.payload().getTotalElements()).isEqualTo(2);
        assertThat(articlePage0.payload().getTotalPages()).isEqualTo(2);
        assertThat(articlePage0.payload().getContent())
                .extracting(item -> item.getTitle()).containsExactly("Article newest");
        assertThat(articlePage1.payload().getContent())
                .extracting(item -> item.getTitle()).containsExactly("Article older");

        UUID unknownTopic = UUID.fromString("69000000-0000-0000-0000-000000000799");
        String emptyBody = mockMvc.perform(get("/api/v1/content/lifecycle")
                        .param("type", "FAQ")
                        .param("topicId", unknownTopic.toString())
                        .param("page", "0")
                        .param("size", "1")
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode emptyEnvelope = objectMapper.readTree(emptyBody).path("data");
        assertThat(emptyEnvelope.path("stage").asText()).isEqualTo("PREGNANCY");
        assertThat(emptyEnvelope.path("payload").path("data")).isEmpty();
        assertThat(emptyEnvelope.path("payload").path("totalElements").asLong()).isZero();
        assertThat(emptyEnvelope.path("payload").path("totalPages").asInt()).isZero();
    }

    @Test
    void r69_006_equalPublishedTimestampsUseIdDescendingAcrossAdjacentPages() {
        assertThat(jdbcTemplate.update(
                "update mother_journeys set journey_type='PREGNANCY', updated_at=now() "
                        + "where journey_id=?", journeyId)).isOne();
        UUID lowerId = UUID.fromString("69000000-0000-0000-0000-000000000901");
        UUID higherId = UUID.fromString("69000000-0000-0000-0000-000000000902");
        UUID topicId = UUID.fromString("69000000-0000-0000-0000-000000000903");
        Instant samePublishedAt = Instant.parse("2026-07-23T12:00:00Z");
        insertContentWithId(lowerId, "Equal timestamp lower id", topicId, samePublishedAt);
        insertContentWithId(higherId, "Equal timestamp higher id", topicId, samePublishedAt);
        entityManager.clear();

        for (int attempt = 0; attempt < 3; attempt++) {
            var page0 = contentService.getLifecycleContents(
                    motherId, ContentType.ARTICLE, topicId, PageRequest.of(0, 1));
            var page1 = contentService.getLifecycleContents(
                    motherId, ContentType.ARTICLE, topicId, PageRequest.of(1, 1));

            assertThat(page0.payload().getContent())
                    .extracting(row -> row.getId())
                    .containsExactly(higherId);
            assertThat(page1.payload().getContent())
                    .extracting(row -> row.getId())
                    .containsExactly(lowerId);
        }
    }

    @Test
    void uc82_69_tc_020_realApprovedSameStageDetailPreservesPublicDtoAndRedactsInternals()
            throws Exception {
        UUID topicId = UUID.fromString("69000000-0000-0000-0000-000000000801");
        UUID privateAuthorId = UUID.fromString("69000000-0000-0000-0000-000000000802");
        ContentItem item = ContentItem.builder()
                .type(ContentType.ARTICLE)
                .title("Public lifecycle detail")
                .body("Reviewed public body")
                .stage(ContentStage.PRE_PREGNANCY)
                .topicId(topicId)
                .status(ContentStatus.APPROVED)
                .versionNo(9)
                .authorUserId(privateAuthorId)
                .sourceLabel("Public source label")
                .sources(List.of(new ContentSource(
                        "Public source title", "https://example.invalid/public", "Public publisher")))
                .publishedAt(Instant.parse("2026-07-22T00:00:00Z"))
                .build();
        item = contentRepository.saveAndFlush(item);
        UUID itemId = item.getId();
        entityManager.clear();

        String body = mockMvc.perform(get("/api/v1/content/lifecycle/" + itemId)
                        .with(user(motherId.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode envelope = objectMapper.readTree(body).path("data");
        JsonNode detail = envelope.path("payload");
        assertThat(envelope.path("stage").asText()).isEqualTo("PRE_PREGNANCY");
        assertThat(detail.path("id").asText()).isEqualTo(itemId.toString());
        assertThat(detail.path("type").asText()).isEqualTo("ARTICLE");
        assertThat(detail.path("title").asText()).isEqualTo("Public lifecycle detail");
        assertThat(detail.path("body").asText()).isEqualTo("Reviewed public body");
        assertThat(detail.path("stage").asText()).isEqualTo("PRE_PREGNANCY");
        assertThat(detail.path("topicId").asText()).isEqualTo(topicId.toString());
        assertThat(detail.path("version").asInt()).isEqualTo(9);
        assertThat(detail.path("status").asText()).isEqualTo("APPROVED");
        assertThat(detail.path("sourceLabel").asText()).isEqualTo("Public source label");
        assertThat(detail.path("publishedAt").asText()).isNotBlank();
        assertThat(detail.path("updatedAt").asText()).isNotBlank();
        assertThat(detail.path("createdAt").asText()).isNotBlank();
        assertThat(detail.path("sources")).singleElement().satisfies(source -> {
            assertThat(source.path("title").asText()).isEqualTo("Public source title");
            assertThat(source.path("publisher").asText()).isEqualTo("Public publisher");
        });
        assertThat(detail.path("contentStale").asBoolean()).isFalse();
        assertThat(body).doesNotContain(
                privateAuthorId.toString(), "authorUserId", "authorId", "reviewReason");
    }

    private ChecklistTemplate seedTemplate(
            String name, ContentStage stage, ChecklistTemplateStatus status) {
        return promote(seedDraft(name, stage), status);
    }

    /**
     * Inline template metadata is the sole template authority since the checklist support
     * tables were retired: a TEMPLATE_ROOT must carry a lineage/version pair, a recipient
     * scope and eligibility bounds that match its stage, or the schema rejects the row.
     *
     * <p>Always created as a draft, because the schema freezes a template's entries the
     * moment it reaches APPROVED or ARCHIVED — and refuses to demote it again. Anything
     * that needs items must seed them here and call {@link #promote} afterwards.
     */
    private ChecklistTemplate seedDraft(String name, ContentStage stage) {
        boolean prePregnancy = stage == ContentStage.PRE_PREGNANCY;
        return templateRepository.saveAndFlush(ChecklistTemplate.builder()
                .name(name)
                .stage(stage)
                .templateType(ChecklistTemplateType.OPTIONAL)
                .status(ChecklistTemplateStatus.DRAFT)
                .versionNo(1)
                .templateLineageId(UUID.randomUUID())
                .templateVersionId(UUID.randomUUID())
                .recipientScope(ChecklistRecipientScope.MOTHER)
                .eligibilityAnchorType(prePregnancy
                        ? ChecklistAnchorType.NONE
                        : stage == ContentStage.PREGNANCY
                                ? ChecklistAnchorType.LMP
                                : ChecklistAnchorType.DELIVERY_DATE)
                .eligibilityRangeUnit(prePregnancy
                        ? ChecklistRangeUnit.DAY : ChecklistRangeUnit.WEEK)
                .eligibilityStartInclusive(0)
                .eligibilityEndInclusive(prePregnancy ? 0 : 42)
                .build());
    }

    private ChecklistTemplate promote(ChecklistTemplate template, ChecklistTemplateStatus status) {
        if (status == ChecklistTemplateStatus.DRAFT) {
            return template;
        }
        template.setStatus(status);
        if (status == ChecklistTemplateStatus.APPROVED) {
            // care_item_templates_approved_gate_ck: an APPROVED root must carry provenance.
            template.setApprovedAt(Instant.parse("2026-07-01T00:00:00Z"));
            template.setApprovedBy(UUID.randomUUID());
        }
        return templateRepository.saveAndFlush(template);
    }

    private void seedChecklistItem(ChecklistTemplate template, String text, Integer order) {
        checklistItemRepository.saveAndFlush(ChecklistItem.builder()
                .template(template)
                .itemText(text)
                .order(order)
                .isRequired(false)
                .build());
    }

    private ContentItem seedContent(String title, ContentStage stage, ContentStatus status) {
        return seedContent(title, ContentType.ARTICLE, stage, status, null,
                status == ContentStatus.APPROVED ? Instant.now() : null);
    }

    private ContentItem seedContent(
            String title,
            ContentType type,
            ContentStage stage,
            ContentStatus status,
            UUID topicId,
            Instant publishedAt) {
        return contentRepository.saveAndFlush(ContentItem.builder()
                .type(type)
                .title(title)
                .body("Story 6.9 integration body")
                .stage(stage)
                .topicId(topicId)
                .status(status)
                .versionNo(1)
                .publishedAt(publishedAt)
                .build());
    }

    private void insertContentWithId(
            UUID id, String title, UUID topicId, Instant publishedAt) {
        jdbcTemplate.update(
                "insert into content_items "
                        + "(content_item_id, body, content_type, created_at, published_at, status, "
                        + "title, topic_id, version_no, stage) "
                        + "values (?, 'Story 6.9 pagination regression', 'ARTICLE', now(), ?, "
                        + "'APPROVED', ?, ?, 1, 'PREGNANCY')",
                id, Timestamp.from(publishedAt), title, topicId);
    }

    private String uniquePhone() {
        return "07" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private void wipeStoryFixtures() {
        // preparation_checklist_items was dropped with the consolidation contract; its v2
        // successor checklist_task_instances hangs off checklist_instances, which
        // references users, so the cascade below already clears it.
        jdbcTemplate.execute(
                "truncate table care_item_templates, "
                        + "content_item_sources, content_items, "
                        + "care_subjects, mother_journeys, audit_events, users cascade");
    }

}
