package com.carebridge.backend.testsupport;

import java.util.Locale;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Runs embedded-PostgreSQL tests only where the project ships matching binaries. */
public final class WindowsEmbeddedPostgresCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Windows embedded PostgreSQL binaries are available");
    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("Embedded PostgreSQL tests are Windows-only");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return operatingSystem.startsWith("windows") ? ENABLED : DISABLED;
    }
}
