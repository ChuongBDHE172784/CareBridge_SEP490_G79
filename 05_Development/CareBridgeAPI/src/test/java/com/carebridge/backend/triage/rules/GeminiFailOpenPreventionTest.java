package com.carebridge.backend.triage.rules;

import com.carebridge.backend.common.config.DevPortMockConfiguration;
import com.carebridge.backend.common.config.DevPortStubConfiguration;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.triage.adapter.GeminiTriageClientAdapter;
import com.carebridge.backend.triage.exception.AiOutcomeUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guards the fail-open hole that used to exist around the LLM triage path.
 *
 * <p>Three production sites previously returned {@code RiskLevel.GREEN} when the model was
 * unavailable, absent or unparseable — an outage produced the most reassuring possible
 * answer. These tests pin the fail-closed behaviour so it cannot silently come back.
 */
class GeminiFailOpenPreventionTest {

    @Test
    @DisplayName("An unavailable model never yields a triage colour")
    void unavailableModelFailsClosed() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generate(anyString())).thenThrow(new RuntimeException("outage"));

        assertThatThrownBy(() -> new GeminiTriageClientAdapter(client).analyzeSymptoms("x"))
                .isInstanceOf(AiOutcomeUnavailableException.class);
    }

    @Test
    @DisplayName("An unparseable response never defaults to GREEN")
    void unparseableResponseFailsClosed() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generate(anyString())).thenReturn("... no recognisable verdict ...");

        assertThatThrownBy(() -> new GeminiTriageClientAdapter(client).analyzeSymptoms("x"))
                .isInstanceOf(AiOutcomeUnavailableException.class);
    }

    @Test
    @DisplayName("The dev stub cannot produce a triage outcome")
    void devStubFailsClosed() {
        assertThatThrownBy(() ->
                new DevPortStubConfiguration().geminiTriageClient().analyzeSymptoms("x"))
                .isInstanceOf(AiOutcomeUnavailableException.class);
    }

    @Test
    @DisplayName("The dev mock cannot produce a triage outcome")
    void devMockFailsClosed() {
        assertThatThrownBy(() ->
                new DevPortMockConfiguration().geminiTriageClient().analyzeSymptoms("x"))
                .isInstanceOf(AiOutcomeUnavailableException.class);
    }

    @Test
    @DisplayName("No stub, mock or catch block in production yields a triage colour")
    void noUnavailabilityPathReturnsAColour() throws Exception {
        Path mainSources = Path.of("src", "main", "java", "com", "carebridge", "backend");
        List<String> offenders;
        try (Stream<Path> paths = Files.walk(mainSources)) {
            offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(GeminiFailOpenPreventionTest::hasFailOpenColour)
                    .map(Path::toString)
                    .toList();
        }
        // Returning GREEN because the model SAID "GREEN" is legitimate V1 parsing. What must
        // never happen is a colour appearing because the model was absent, broken or stubbed.
        assertThat(offenders)
                .as("an unavailability, stub or default path that yields a triage colour is fail-open")
                .isEmpty();
    }

    private static boolean hasFailOpenColour(Path path) {
        try {
            String source = Files.readString(path);
            if (source.contains("RiskLevel level = RiskLevel.GREEN")) {
                return true;  // GREEN as the default before parsing
            }
            boolean isStubOrMockConfig = path.toString().contains("config")
                    && (source.contains("DEV-STUB") || source.contains("ConditionalOnMissingBean"));
            if (isStubOrMockConfig && source.contains("AiTriageResult(RiskLevel.")) {
                return true;  // a stub or mock bean producing a colour
            }
            return colourInsideCatchBlock(source);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Detects a triage colour produced from inside an exception handler, using real brace
     * matching. A line-offset heuristic is not good enough here: it over-reads past the
     * handler and flags legitimate parsing code further down the method.
     */
    private static boolean colourInsideCatchBlock(String source) {
        int index = source.indexOf("catch (");
        while (index >= 0) {
            int open = source.indexOf('{', index);
            if (open < 0) {
                return false;
            }
            int depth = 0;
            int cursor = open;
            while (cursor < source.length()) {
                char character = source.charAt(cursor);
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                }
                cursor++;
            }
            String block = source.substring(open, Math.min(cursor + 1, source.length()));
            if (block.contains("AiTriageResult(RiskLevel.")) {
                return true;
            }
            index = source.indexOf("catch (", index + 1);
        }
        return false;
    }
}
