package com.carebridge.backend.common.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ProductionReferenceDataLoaderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Test
    void splitsProductionReferenceDataIntoExpectedStatements() {
        var resource = new org.springframework.core.io.ClassPathResource(
                "db/data_seed/production_reference_data.sql");
        assertThat(resource.exists()).isTrue();

        var loader = new ProductionReferenceDataLoader(jdbcTemplate);
        loader.seedProductionReferenceData();

        verify(jdbcTemplate, times(6)).execute(anyString());
    }

    @Test
    void splitSqlStatementsHandlesCommentsAndSemicolons() {
        String testSql = """
                -- Comment line 1
                INSERT INTO public.table_a (col) VALUES ('val;1');
                /* Block comment ; */
                INSERT INTO public.table_b (col) VALUES ('val2');
                """;
        List<String> statements = ProductionReferenceDataLoader.splitSqlStatements(testSql);
        assertThat(statements).hasSize(2);
    }

    @Test
    void runSkipsPostgresOnlyReferenceDataForOtherDatabases() throws Exception {
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

        new ProductionReferenceDataLoader(jdbcTemplate).run(null);

        verify(jdbcTemplate, times(0)).execute(anyString());
        verify(connection).close();
    }

    @Test
    void beanIsLoadedByDefaultAcrossProfiles() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .run(context -> {
                    assertThat(context.getBeansOfType(ProductionReferenceDataLoader.class)).hasSize(1);
                });
    }

    @Test
    void beanCanBeDisabledExplicitly() {
        new ApplicationContextRunner()
                .withPropertyValues("carebridge.reference-data.enabled=false")
                .withUserConfiguration(TestConfig.class)
                .run(context -> {
                    assertThat(context.getBeansOfType(ProductionReferenceDataLoader.class)).isEmpty();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProductionReferenceDataLoader.class)
    static class TestConfig {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return org.mockito.Mockito.mock(JdbcTemplate.class);
        }
    }
}
