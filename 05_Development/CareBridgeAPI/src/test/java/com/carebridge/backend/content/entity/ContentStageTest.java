package com.carebridge.backend.content.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContentStageTest {

    @Test
    void fromApiValueMapsOnlyTheLegacyBabyCareAliasToPostpartum() {
        assertThat(ContentStage.fromApiValue("BABY_CARE")).isEqualTo(ContentStage.POSTPARTUM);
        assertThat(ContentStage.fromApiValue("pregnancy")).isEqualTo(ContentStage.PREGNANCY);
    }

    @Test
    void fromApiValueRejectsUnknownStagesInsteadOfDefaultingToPostpartum() {
        assertThatThrownBy(() -> ContentStage.fromApiValue("UNKNOWN_STAGE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
