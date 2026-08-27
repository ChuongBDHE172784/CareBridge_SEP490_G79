package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Real PostgreSQL proof that disabling rollout never restores the import writer. */
class ChecklistImportPostgresIntegrationTest extends AbstractEmbeddedPostgresIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void defaultFlagOffStillReturnsGoneAndLeavesLegacyTableUnchanged() throws Exception {
        long before = jdbcTemplate.queryForObject(
                "select count(*) from preparation_checklist_items", Long.class);
        UUID actor = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/user-checklist-items/import").with(csrf())
                        .with(user(actor.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"journeyId\":\"%s\",\"templateItemIds\":[\"%s\"]}"
                                .formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("CHECKLIST_LEGACY_ROUTE_RETIRED"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from preparation_checklist_items", Long.class)).isEqualTo(before);
    }
}
