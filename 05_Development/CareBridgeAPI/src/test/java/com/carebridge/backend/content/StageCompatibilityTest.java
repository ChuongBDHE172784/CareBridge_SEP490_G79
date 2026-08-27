package com.carebridge.backend.content;

import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void babyCareIsCanonicalForContentButNormalizesForLegacyConsumers() throws Exception {
        assertThat(objectMapper.readValue("\"BABY_CARE\"", ContentStage.class))
                .isEqualTo(ContentStage.BABY_CARE);
        assertThat(objectMapper.readValue("\"BABY_CARE\"", PregnancyStage.class))
                .isEqualTo(PregnancyStage.POSTPARTUM);
        assertThat(objectMapper.readValue("\"BABY_CARE\"", UserStage.class))
                .isEqualTo(UserStage.POSTPARTUM);
    }

    @Test
    void canonicalStagesRemainUnchanged() throws Exception {
        assertThat(objectMapper.readValue("\"PRE_PREGNANCY\"", ContentStage.class))
                .isEqualTo(ContentStage.PRE_PREGNANCY);
        assertThat(objectMapper.readValue("\"PREGNANCY\"", PregnancyStage.class))
                .isEqualTo(PregnancyStage.PREGNANCY);
        assertThat(objectMapper.readValue("\"POSTPARTUM\"", ContentStage.class))
                .isEqualTo(ContentStage.POSTPARTUM);
    }

    @Test
    void enumsExposeTheirCanonicalValues() {
        assertThat(ContentStage.values()).containsExactly(
                ContentStage.PRE_PREGNANCY,
                ContentStage.PREGNANCY,
                ContentStage.POSTPARTUM,
                ContentStage.BABY_CARE);
        assertThat(PregnancyStage.values()).containsExactly(
                PregnancyStage.PRE_PREGNANCY,
                PregnancyStage.PREGNANCY,
                PregnancyStage.POSTPARTUM);
        assertThat(UserStage.values()).containsExactly(
                UserStage.PRE_PREGNANCY,
                UserStage.PREGNANCY,
                UserStage.POSTPARTUM);
    }
}
