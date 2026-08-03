package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.sequence.ChecklistSequenceState;
import com.carebridge.backend.checklist.today.dto.TodaySequenceNextSet;
import com.carebridge.backend.checklist.today.dto.TodaySequenceProjection;
import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TodayTasksSequenceProjectionSerializationTest {

    @Test
    void sequenceProjectionIsAdditiveAndUnwrappedAtTodayTopLevel() throws Exception {
        TodaySequenceProjection sequence = new TodaySequenceProjection(
                ChecklistSequenceState.READY_TO_ADVANCE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Set 1",
                1,
                3,
                1,
                true,
                new TodaySequenceNextSet("Set 2", 2),
                false,
                null);
        TodayTasksResponse response = new TodayTasksResponse(
                Instant.parse("2026-08-03T00:00:00Z"), "Asia/Ho_Chi_Minh", 7,
                new TodayTaskSections(List.of(), List.of(), List.of(), List.of()),
                new TodayTaskCounts(0, 0, 0, 0), UUID.randomUUID(), sequence);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(response);

        assertThat(json).contains("\"sequenceState\":\"READY_TO_ADVANCE\"");
        assertThat(json).contains("\"nextSet\"");
        assertThat(json).doesNotContain("\"sequence\":");
    }
}
