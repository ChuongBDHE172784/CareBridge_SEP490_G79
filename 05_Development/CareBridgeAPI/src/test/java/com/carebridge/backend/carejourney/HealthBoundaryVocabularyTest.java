package com.carebridge.backend.carejourney;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-006: baby-care content must remain observational and must not diagnose,
 * prescribe, recommend treatment, or make unqualified health claims.
 */
class HealthBoundaryVocabularyTest {

    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(diagnos(?:e|is|tic)?|prescri(?:be|ption)?|treatment|healthy|normal)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    void babyCareSourceContainsNoClinicalConclusionVocabulary() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        List<Path> roots = List.of(
                projectRoot.resolve("src/main/java/com/carebridge/backend/baby"),
                projectRoot.resolve("src/main/java/com/carebridge/backend/carejourney"),
                projectRoot.resolve("src/main/java/com/carebridge/backend/notification"),
                projectRoot.resolve("src/main/java/com/carebridge/backend/emergency"),
                projectRoot.resolve("../CareBridgeMobileApp/lib/features/baby").normalize(),
                projectRoot.resolve("../CareBridgeMobileApp/lib/features/notification").normalize(),
                projectRoot.resolve("../CareBridgeMobileApp/lib/core/notifications").normalize(),
                projectRoot.resolve("../CareBridgeMobileApp/lib/l10n").normalize(),
                projectRoot.resolve("../CareBridgeMobileApp/lib/app/localization").normalize());
        List<String> violations = new ArrayList<>();

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java")
                                || path.toString().endsWith(".dart"))
                        .forEach(path -> collectViolations(path, violations));
            }
        }

        assertThat(violations)
                .as("SEC-006 forbidden health-boundary vocabulary")
                .isEmpty();
    }

    private void collectViolations(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (int index = 0; index < lines.size(); index++) {
                if (FORBIDDEN.matcher(lines.get(index)).find()) {
                    violations.add(path + ":" + (index + 1) + " -> " + lines.get(index).trim());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan " + path, exception);
        }
    }
}
