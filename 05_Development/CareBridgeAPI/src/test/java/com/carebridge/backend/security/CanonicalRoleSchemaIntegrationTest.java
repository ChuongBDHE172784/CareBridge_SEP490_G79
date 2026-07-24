package com.carebridge.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class CanonicalRoleSchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationStartsWithUsersRoleAsTheOnlyNullableCanonicalRolePersistence() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.roles')::text", String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.user_roles')::text", String.class)).isNull();

        var roleColumn = jdbcTemplate.queryForMap("""
                SELECT is_nullable, data_type
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = 'users'
                   AND column_name = 'role'
                """);
        String constraint = jdbcTemplate.queryForObject("""
                SELECT pg_get_constraintdef(oid)
                  FROM pg_constraint
                 WHERE conrelid = 'public.users'::regclass
                   AND conname = 'users_role_check'
                """, String.class);

        assertThat(roleColumn.get("is_nullable")).isEqualTo("YES");
        assertThat(roleColumn.get("data_type")).isEqualTo("character varying");
        assertThat(constraint).contains(
                "MOTHER", "FAMILY", "EXPERT", "MODERATOR",
                "CONTENT_ADMIN", "SYSTEM_ADMIN", "PARTNER");
    }
}
