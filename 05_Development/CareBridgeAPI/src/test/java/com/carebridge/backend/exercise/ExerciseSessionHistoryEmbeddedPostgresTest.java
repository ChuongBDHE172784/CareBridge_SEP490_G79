package com.carebridge.backend.exercise;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ExerciseSessionHistoryEmbeddedPostgresTest
        extends AbstractEmbeddedPostgresIntegrationTest {

    private static final UUID SEEDED_OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final String SEEDED_SESSION_ID =
            "73000000-0000-0000-0000-000000000001";
    private static final String SEEDED_EXERCISE_ID =
            "60000000-0000-0000-0000-000000000003";

    @Autowired private MockMvc mockMvc;

    @Test
    void ownerReceivesHydratedSeededHistoryInTopLevelDataList() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exerciseSessionId").value(SEEDED_SESSION_ID))
                .andExpect(jsonPath("$.data[0].exerciseId").value(SEEDED_EXERCISE_ID))
                .andExpect(jsonPath("$.data[0].exerciseTitle").value("Yoga bầu 20 phút"))
                .andExpect(jsonPath("$.data[0].sessionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].actualDurationSeconds").value(1200))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void differentMotherCannotReadSeededOwnersHistory() throws Exception {
        UUID otherMother = UUID.fromString("10000000-0000-0000-0000-000000000005");

        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .with(user(otherMother.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void dateBoundsFilterTheSeededSession() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .param("from", "2026-07-26T16:59:00+07:00")
                        .param("to", "2026-07-26T17:01:00+07:00")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .param("from", "2026-07-26T17:01:00+07:00")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void specificTrimesterIncludesExercisesPublishedForAllTrimesters() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .param("trimesterScope", "FIRST")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exerciseSessionId").value(SEEDED_SESSION_ID));
    }

    @Test
    void toOnlyBoundIsInclusive() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .param("to", "2026-07-26T17:00:00+07:00")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].exerciseSessionId").value(SEEDED_SESSION_ID));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedRoleIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/sessions/history")
                        .with(user(SEEDED_OWNER_ID.toString()).roles("CAREGIVER")))
                .andExpect(status().isForbidden());
    }
}
