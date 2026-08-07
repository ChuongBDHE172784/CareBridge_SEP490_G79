package com.carebridge.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
                "CONTENT_ADMIN", "SYSTEM_ADMIN");
    }

    @Test
    void partnerRoleIsRejectedByTheCanonicalRoleConstraint() {
        String constraint = jdbcTemplate.queryForObject("""
                SELECT pg_get_constraintdef(oid)
                  FROM pg_constraint
                 WHERE conrelid = 'public.users'::regclass
                   AND conname = 'users_role_check'
                """, String.class);

        // The Partner programme was retired; the database must be the backstop, not
        // just the Java enum. Asserting the literal absence here is why 'PARTNER'
        // still appears in this file — it proves the value is gone, not retained.
        assertThat(constraint).doesNotContain("PARTNER");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO public.users (user_id, email, role, enabled)
                VALUES (gen_random_uuid(), 'retired-partner@carebridge.dev', 'PARTNER', true)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.users WHERE role = 'PARTNER'", Long.class))
                .isZero();
    }
}
