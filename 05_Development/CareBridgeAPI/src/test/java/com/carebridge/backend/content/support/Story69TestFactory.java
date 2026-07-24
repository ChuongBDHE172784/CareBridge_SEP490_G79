package com.carebridge.backend.content.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Isolated Story 6.9 test data and contract lookup helpers.
 *
 * <p>Every data factory call returns a fresh value. Contract lookup deliberately converts a
 * missing planned type or method into an assertion-friendly empty Optional so the RED gate is a
 * meaningful test failure rather than a Java compilation failure.</p>
 */
public final class Story69TestFactory {

    private Story69TestFactory() {
    }

    public static UUID freshId() {
        return UUID.randomUUID();
    }

    public static Optional<Class<?>> loadClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<Method> method(Class<?> type, String name, int parameterCount) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .filter(candidate -> candidate.getParameterCount() == parameterCount)
                .findFirst();
    }

    public static String productionSource(String moduleRelativePath) {
        Path path = Path.of("src", "main", "java").resolve(moduleRelativePath);
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new AssertionError("Cannot read production contract source: " + path, exception);
        }
    }
}
