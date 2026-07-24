package com.carebridge.backend.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "carebridgeDotenv";

    private final Path dotenvPath;

    public DotenvEnvironmentPostProcessor() {
        this(Path.of(".env"));
    }

    DotenvEnvironmentPostProcessor(Path dotenvPath) {
        this.dotenvPath = dotenvPath;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("carebridge.dotenv.enabled", Boolean.class, true)) {
            return;
        }
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        Map<String, Object> values = parseDotenv(dotenvPath);
        if (values.isEmpty()) {
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        MapPropertySource dotenvPropertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (propertySources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, dotenvPropertySource);
        } else {
            propertySources.addLast(dotenvPropertySource);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    static Map<String, Object> parseDotenv(Path dotenvPath) {
        List<String> lines;
        try {
            lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return Map.of();
        }

        Map<String, Object> values = new LinkedHashMap<>();
        for (String line : lines) {
            parseLine(line, values);
        }
        return values;
    }

    private static void parseLine(String line, Map<String, Object> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }

        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = trimmed.substring(0, separatorIndex).trim();
        if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return;
        }

        String value = trimmed.substring(separatorIndex + 1).trim();
        values.put(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }

        char quote = value.charAt(0);
        if ((quote != '"' && quote != '\'') || value.charAt(value.length() - 1) != quote) {
            return value;
        }

        String unquoted = value.substring(1, value.length() - 1);
        if (quote == '"') {
            return unquoted
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return unquoted.replace("\\'", "'");
    }
}
