package com.carebridge.backend.triage.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Java context enums must match the canonical contract exactly.
 *
 * <p>Python asserts the same file in {@code tests/test_context_contract_parity.py}. Declaring
 * the values in code on both sides is what makes them ordinary enums; this test is what stops
 * the two declarations drifting — the failure mode that previously broke rule parity.
 */
class ContextContractParityTest {

    private static final String CONTRACT_RESOURCE = "triage/context_contract_v1.json";
    private static JsonNode contract;

    @BeforeAll
    static void loadContract() throws Exception {
        try (InputStream stream = new ClassPathResource(CONTRACT_RESOURCE).getInputStream()) {
            contract = new ObjectMapper().readTree(stream);
        }
    }

    private static List<String> values(String section) {
        List<String> values = new ArrayList<>();
        contract.get(section).get("values").forEach(node -> values.add(node.asText()));
        return values;
    }

    @Test
    @DisplayName("TargetEntity matches the canonical contract")
    void targetEntityMatchesContract() {
        assertThat(java.util.Arrays.stream(TargetEntity.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(values("targetEntity"));
        assertThat(TargetEntity.UNKNOWN.isResolved()).isFalse();
        assertThat(TargetEntity.CONFLICTED.isResolved()).isFalse();
        assertThat(TargetEntity.MOTHER.isResolved()).isTrue();
    }

    @Test
    @DisplayName("CareStage matches the canonical contract")
    void careStageMatchesContract() {
        assertThat(java.util.Arrays.stream(CareStage.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(values("careStage"));
    }

    @Test
    @DisplayName("IntentType matches the canonical contract")
    void intentTypeMatchesContract() {
        assertThat(java.util.Arrays.stream(IntentType.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(values("intentType"));
    }

    @Test
    @DisplayName("ContextResolutionStatus and ResolutionSource match the canonical contract")
    void statusAndSourceMatchContract() {
        assertThat(java.util.Arrays.stream(ContextResolutionStatus.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(values("contextResolutionStatus"));
        assertThat(java.util.Arrays.stream(ResolutionSource.values()).map(Enum::name).toList())
                .containsExactlyElementsOf(values("resolutionSource"));
    }

    @Test
    @DisplayName("Entity→stage mapping matches the contract on both sides")
    void entityStageMappingMatchesContract() {
        JsonNode byEntity = contract.get("careStage").get("byEntity");

        for (TargetEntity entity : List.of(TargetEntity.MOTHER, TargetEntity.BABY)) {
            List<String> expected = new ArrayList<>();
            byEntity.get(entity.name()).forEach(node -> expected.add(node.asText()));
            assertThat(CareStage.forEntity(entity).stream().map(Enum::name).toList())
                    .as(entity.name())
                    .containsExactlyElementsOf(expected);
        }

        assertThat(CareStage.forEntity(TargetEntity.UNKNOWN))
                .as("an unresolved entity must have no valid stage")
                .isEmpty();
        assertThat(CareStage.forEntity(TargetEntity.CONFLICTED)).isEmpty();
    }

    @Test
    @DisplayName("A maternal stage is never valid for a baby, and vice versa")
    void stagesDoNotCrossEntities() {
        assertThat(CareStage.isValidFor(TargetEntity.BABY, CareStage.PREGNANCY)).isFalse();
        assertThat(CareStage.isValidFor(TargetEntity.BABY, CareStage.POSTPARTUM_MOTHER)).isFalse();
        assertThat(CareStage.isValidFor(TargetEntity.MOTHER, CareStage.INFANT_0_12M)).isFalse();
        assertThat(CareStage.isValidFor(TargetEntity.MOTHER, CareStage.POSTPARTUM_MOTHER)).isTrue();
    }

    @Test
    @DisplayName("Legacy POSTPARTUM maps only for MOTHER, never for BABY or an unresolved target")
    void legacyPostpartumIsNotAutoMappedToBaby() {
        assertThat(CareStage.mapLegacy("POSTPARTUM", TargetEntity.MOTHER))
                .isEqualTo(CareStage.POSTPARTUM_MOTHER);
        assertThat(CareStage.mapLegacy("POSTPARTUM", TargetEntity.BABY))
                .as("a postpartum session may be about the newborn — resolve the target first")
                .isNull();
        assertThat(CareStage.mapLegacy("POSTPARTUM", TargetEntity.UNKNOWN)).isNull();
        assertThat(CareStage.mapLegacy("POSTPARTUM", TargetEntity.CONFLICTED)).isNull();
    }

    @Test
    @DisplayName("Only symptom triage and follow-up answers may produce a colour")
    void onlyTriageIntentsMayProduceAnOutcome() {
        List<String> allowed = new ArrayList<>();
        contract.get("intentType").get("mayProduceTriageOutcome")
                .forEach(node -> allowed.add(node.asText()));

        for (IntentType intent : IntentType.values()) {
            assertThat(intent.mayProduceTriageOutcome())
                    .as(intent.name())
                    .isEqualTo(allowed.contains(intent.name()));
        }
        assertThat(IntentType.GENERAL_HEALTH_INFORMATION.mayProduceTriageOutcome()).isFalse();
        assertThat(IntentType.SOURCE_LOOKUP.mayProduceTriageOutcome()).isFalse();
    }

    @Test
    @DisplayName("Unresolved context blocks symptom questions, per the contract")
    void unresolvedContextBlocksSymptomQuestions() {
        List<String> blocking = new ArrayList<>();
        contract.get("contextResolutionStatus").get("blocksSymptomQuestions")
                .forEach(node -> blocking.add(node.asText()));

        for (ContextResolutionStatus status : ContextResolutionStatus.values()) {
            assertThat(status.blocksSymptomQuestions())
                    .as(status.name())
                    .isEqualTo(blocking.contains(status.name()));
        }
        assertThat(ContextResolutionStatus.RESOLVED.blocksSymptomQuestions()).isFalse();
    }

    @Test
    @DisplayName("Resolution precedence matches the contract order")
    void resolutionPrecedenceMatchesContract() {
        List<String> precedence = new ArrayList<>();
        contract.get("resolutionSource").get("precedence")
                .forEach(node -> precedence.add(node.asText()));

        for (int index = 1; index < precedence.size(); index++) {
            ResolutionSource higher = ResolutionSource.valueOf(precedence.get(index - 1));
            ResolutionSource lower = ResolutionSource.valueOf(precedence.get(index));
            assertThat(higher.outranks(lower)).as("%s over %s", higher, lower).isTrue();
        }

        assertThat(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE
                .outranks(ResolutionSource.EXPLICIT_SELECTED_PROFILE))
                .as("a stored profile must not override what the user just said")
                .isTrue();
    }
}
