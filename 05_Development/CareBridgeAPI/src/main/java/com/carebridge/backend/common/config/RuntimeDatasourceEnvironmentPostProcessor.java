package com.carebridge.backend.common.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.Profiles;

/** Resolves runtime database credentials as one atomic datasource tuple. */
public class RuntimeDatasourceEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "carebridgeRuntimeDatasource";
    static final String CONFIGURATION_MISSING = "DATASOURCE_CONFIGURATION_MISSING";
    static final String DIRECT_CONFIGURATION_INCOMPLETE =
            "DATASOURCE_DIRECT_CONFIGURATION_INCOMPLETE";
    static final String CAREBRIDGE_CONFIGURATION_INCOMPLETE =
            "DATASOURCE_CAREBRIDGE_CONFIGURATION_INCOMPLETE";
    static final String SUPABASE_CONFIGURATION_INCOMPLETE =
            "DATASOURCE_SUPABASE_CONFIGURATION_INCOMPLETE";

    private static final DatasourceKeys DIRECT_KEYS = new DatasourceKeys(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password");
    private static final DatasourceKeys CAREBRIDGE_KEYS = new DatasourceKeys(
            "CAREBRIDGE_DB_URL",
            "CAREBRIDGE_DB_USERNAME",
            "CAREBRIDGE_DB_PASSWORD");
    private static final DatasourceKeys SUPABASE_KEYS = new DatasourceKeys(
            "SUPABASE_DB_URL",
            "SUPABASE_DB_USERNAME",
            "SUPABASE_DB_PASSWORD");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("hermetic"))) {
            return;
        }

        PropertySource<?> directSource = firstDefiningSource(environment, DIRECT_KEYS);
        TupleState directState = directState(directSource);
        if (directState == TupleState.PARTIAL) {
            fail(DIRECT_CONFIGURATION_INCOMPLETE);
        }

        DatasourceTuple carebridge = runtimeTuple(environment, CAREBRIDGE_KEYS);
        DatasourceTuple supabase = runtimeTuple(environment, SUPABASE_KEYS);
        if (carebridge.state() == TupleState.PARTIAL) {
            fail(CAREBRIDGE_CONFIGURATION_INCOMPLETE);
        }
        if (supabase.state() == TupleState.PARTIAL) {
            fail(SUPABASE_CONFIGURATION_INCOMPLETE);
        }

        if (environment.acceptsProfiles(Profiles.of("local"))) {
            DatasourceTuple selected = carebridge.state() == TupleState.COMPLETE
                    ? carebridge
                    : supabase.state() == TupleState.COMPLETE ? supabase : null;
            if (selected == null) {
                return;
            }
            if (directState == TupleState.COMPLETE
                    && hasAtLeastAsMuchPrecedence(environment, directSource, selected.source())) {
                return;
            }
            publish(environment, selected, directSource);
            return;
        }

        if (directState == TupleState.COMPLETE) {
            return;
        }

        if (carebridge.state() == TupleState.COMPLETE) {
            publish(environment, carebridge, null);
            return;
        }
        if (supabase.state() == TupleState.COMPLETE) {
            publish(environment, supabase, null);
            return;
        }
        fail(CONFIGURATION_MISSING);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private static TupleState directState(PropertySource<?> source) {
        if (source == null) {
            return TupleState.ABSENT;
        }
        int configured = configuredCount(source, DIRECT_KEYS);
        if (configured < 3) {
            return TupleState.PARTIAL;
        }
        String url = property(source, DIRECT_KEYS.url());
        String username = property(source, DIRECT_KEYS.username());
        String password = property(source, DIRECT_KEYS.password());
        return isBlank(url) || isBlank(username) || isBlank(password)
                ? TupleState.PARTIAL
                : TupleState.COMPLETE;
    }

    private static DatasourceTuple runtimeTuple(
            ConfigurableEnvironment environment, DatasourceKeys keys) {
        PropertySource<?> source = firstDefiningSource(environment, keys);
        if (source == null) {
            return DatasourceTuple.absent();
        }

        int configured = configuredCount(source, keys);
        String url = property(source, keys.url());
        String username = property(source, keys.username());
        String password = property(source, keys.password());
        if (configured < 3 || isBlank(url) || isBlank(username) || isBlank(password)) {
            return DatasourceTuple.partial();
        }
        return new DatasourceTuple(TupleState.COMPLETE, url, username, password, source);
    }

    private static PropertySource<?> firstDefiningSource(
            ConfigurableEnvironment environment, DatasourceKeys keys) {
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (PROPERTY_SOURCE_NAME.equals(source.getName())
                    || "configurationProperties".equals(source.getName())) {
                continue;
            }
            if (configuredCount(source, keys) > 0) {
                return source;
            }
        }
        return null;
    }

    private static int configuredCount(
            PropertySource<?> source, DatasourceKeys keys) {
        int configured = 0;
        if (source.containsProperty(keys.url())) {
            configured++;
        }
        if (source.containsProperty(keys.username())) {
            configured++;
        }
        if (source.containsProperty(keys.password())) {
            configured++;
        }
        return configured;
    }

    private static String property(PropertySource<?> source, String key) {
        Object value = source.getProperty(key);
        return value == null ? null : value.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasAtLeastAsMuchPrecedence(
            ConfigurableEnvironment environment,
            PropertySource<?> candidate,
            PropertySource<?> reference) {
        if (candidate == null) {
            return false;
        }
        if (reference == null || candidate.getName().equals(reference.getName())) {
            return true;
        }
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (candidate.getName().equals(source.getName())) {
                return true;
            }
            if (reference.getName().equals(source.getName())) {
                return false;
            }
        }
        return false;
    }

    private static void publish(
            ConfigurableEnvironment environment,
            DatasourceTuple tuple,
            PropertySource<?> beforeSource) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(DIRECT_KEYS.url(), tuple.url());
        values.put(DIRECT_KEYS.username(), tuple.username());
        values.put(DIRECT_KEYS.password(), tuple.password());

        MutablePropertySources propertySources = environment.getPropertySources();
        MapPropertySource resolved = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.replace(PROPERTY_SOURCE_NAME, resolved);
        } else if (beforeSource != null && propertySources.contains(beforeSource.getName())) {
            propertySources.addBefore(beforeSource.getName(), resolved);
        } else {
            propertySources.addLast(resolved);
        }
    }

    private static void fail(String code) {
        throw new IllegalStateException(code);
    }

    private enum TupleState {
        ABSENT,
        PARTIAL,
        COMPLETE
    }

    private record DatasourceKeys(String url, String username, String password) {}

    private record DatasourceTuple(
            TupleState state,
            String url,
            String username,
            String password,
            PropertySource<?> source) {

        private static DatasourceTuple absent() {
            return new DatasourceTuple(TupleState.ABSENT, null, null, null, null);
        }

        private static DatasourceTuple partial() {
            return new DatasourceTuple(TupleState.PARTIAL, null, null, null, null);
        }
    }
}
