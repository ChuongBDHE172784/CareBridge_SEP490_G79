package com.carebridge.backend.testsupport;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL images used by tests that execute the canonical Flyway chain. */
public final class PostgresTestImages {

    private static final DockerImageName PGVECTOR_16 = pgvector("pg16");
    private static final DockerImageName PGVECTOR_18 = pgvector("pg18");

    private PostgresTestImages() {
    }

    public static PostgreSQLContainer pg16() {
        return new PostgreSQLContainer(PGVECTOR_16);
    }

    public static PostgreSQLContainer pg18() {
        return new PostgreSQLContainer(PGVECTOR_18);
    }

    private static DockerImageName pgvector(String tag) {
        return DockerImageName.parse("pgvector/pgvector:" + tag)
                .asCompatibleSubstituteFor("postgres");
    }
}
