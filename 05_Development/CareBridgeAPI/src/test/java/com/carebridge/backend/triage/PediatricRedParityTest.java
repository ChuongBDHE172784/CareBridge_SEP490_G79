package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.engine.PediatricRiskRules;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PediatricRedParityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PediatricRiskRules rules = new PediatricRiskRules();

    @Test
    void allPediatricRedVectorsMatchSharedContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/triage/pediatric_red_parity_vectors.json")) {
            List<Map<String, Object>> vectors = objectMapper.readValue(input, new TypeReference<>() {});
            for (Map<String, Object> vector : vectors) {
                RunIntakeRequest request = new RunIntakeRequest();
                request.setStage(TriageStage.INFANT);
                request.setChildAgeMonths(((Number) vector.getOrDefault("childAgeMonths", 12)).intValue());
                if (vector.containsKey("temperatureC")) {
                    request.setTemperatureC(((Number) vector.get("temperatureC")).doubleValue());
                }
                List<String> symptoms = objectMapper.convertValue(vector.get("symptoms"), new TypeReference<>() {});
                PediatricRiskRules.RuleOutcome outcome = rules.apply(request, symptoms);
                assertThat(outcome.riskLevel()).as((String) vector.get("name")).isEqualTo("RED");
                assertThat(outcome.matchedRules()).as((String) vector.get("name"))
                        .contains((String) vector.get("expectedRule"));
            }
        }
    }
}
