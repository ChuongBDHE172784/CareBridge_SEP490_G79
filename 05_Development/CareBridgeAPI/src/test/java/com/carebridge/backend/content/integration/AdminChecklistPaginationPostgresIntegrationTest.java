package com.carebridge.backend.content.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** PostgreSQL regressions for deterministic admin checklist pagination and nullable stage rows. */
class AdminChecklistPaginationPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID LOWER_ID =
            UUID.fromString("69000000-0000-0000-0000-000000000911");
    private static final UUID HIGHER_ID =
            UUID.fromString("69000000-0000-0000-0000-000000000912");
    private static final UUID NULL_APPROVED_ID =
            UUID.fromString("69000000-0000-0000-0000-000000000913");
    private static final UUID NULL_DRAFT_ID =
            UUID.fromString("69000000-0000-0000-0000-000000000914");
    private static final Instant SAME_UPDATED_AT = Instant.parse("2026-07-23T12:30:00Z");

    @Autowired private ContentService contentService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("truncate table care_item_templates cascade");
        insert(LOWER_ID, "Equal timestamp lower id", "PREGNANCY", "APPROVED", SAME_UPDATED_AT);
        insert(HIGHER_ID, "Equal timestamp higher id", "PREGNANCY", "APPROVED", SAME_UPDATED_AT);
        insert(NULL_APPROVED_ID, "Legacy null approved", null, "APPROVED", null);
        insert(NULL_DRAFT_ID, "Legacy null draft", "POSTPARTUM", "DRAFT", null);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("truncate table care_item_templates cascade");
    }

    @Test
    void r69_002_filteredAndUnfilteredPagesUseUpdatedAtNullsLastThenIdDescending() {
        assertAdjacentPages(
                ChecklistTemplateStatus.APPROVED,
                new UUID[] {HIGHER_ID, LOWER_ID, NULL_APPROVED_ID});
        assertAdjacentPages(
                null,
                new UUID[] {HIGHER_ID, LOWER_ID, NULL_DRAFT_ID, NULL_APPROVED_ID});
    }

    @Test
    void r69_008_adminApiSerializesCanonicalNullStageAndTimestamp() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/checklists")
                        .with(user("69000000-0000-0000-0000-000000000001")
                                .roles("CONTENT_ADMIN"))
                        .param("status", "APPROVED")
                        .param("page", "2")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(NULL_APPROVED_ID.toString()))
                .andExpect(jsonPath("$.data[0].stage").value(
                        org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data[0].updatedAt").isNotEmpty());
    }

    private void assertAdjacentPages(ChecklistTemplateStatus status, UUID[] expectedIds) {
        for (int attempt = 0; attempt < 3; attempt++) {
            for (int page = 0; page < expectedIds.length; page++) {
                var result = contentService.getAdminChecklists(
                        null, status, PageRequest.of(page, 1));
                assertThat(result.getContent())
                        .extracting(row -> row.id())
                        .containsExactly(expectedIds[page]);
            }
        }
    }

    /**
     * Inline template metadata is the sole template authority since the checklist support
     * tables were retired, and the schema enforces its full shape on every TEMPLATE_ROOT:
     * a lineage/version pair, a recipient scope, approval provenance when APPROVED, and
     * eligibility bounds that match the stage.
     *
     * <p>The null-stage row this suite exists to cover is only legal under FAMILY scope —
     * the validator demands a stage for every mother-facing template — so scope is derived
     * from the stage rather than hard-coded, which keeps the legacy row the test cares
     * about while satisfying the constraint.
     */
    private void insert(UUID id, String name, String stage, String status, Instant updatedAt) {
        boolean familyScoped = stage == null;
        boolean approved = "APPROVED".equals(status);
        jdbcTemplate.update(
                "insert into care_item_templates "
                        + "(template_id, entry_type, created_at, title, stage, content_status, "
                        + "updated_at, version, description, template_lineage_id, "
                        + "template_version_id, recipient_scope, eligibility_anchor_type, "
                        + "eligibility_range_unit, eligibility_start_inclusive, "
                        + "eligibility_end_inclusive, approved_at, approved_by) values "
                        + "(?, 'TEMPLATE_ROOT', now(), ?, ?, ?, "
                        + "coalesce(?, timestamp with time zone '1970-01-01 00:00:00+00'), "
                        + "1, 'R69 regression', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, name, stage, status,
                updatedAt == null ? null : Timestamp.from(updatedAt),
                UUID.randomUUID(), UUID.randomUUID(),
                familyScoped ? "FAMILY" : "MOTHER",
                familyScoped ? null : "PREGNANCY".equals(stage) ? "LMP" : "DELIVERY_DATE",
                familyScoped ? null : "WEEK",
                familyScoped ? null : 0,
                familyScoped ? null : 42,
                approved ? Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")) : null,
                approved ? UUID.randomUUID() : null);
    }
}
