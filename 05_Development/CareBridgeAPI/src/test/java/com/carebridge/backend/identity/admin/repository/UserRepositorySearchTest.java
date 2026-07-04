package com.carebridge.backend.identity.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.security.repository.UserRepository;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * UC114-TC-013 — search repository query never references roles/user_roles tables
 * (ADR-IAM-001). Pure reflection check on the @Query annotation string — no DB needed.
 */
class UserRepositorySearchTest {

    @Test
    void searchQuery_isScopedToUserEntityOnly() throws NoSuchMethodException {
        Method searchMethod = UserRepository.class.getMethod(
                "search", String.class, String.class, String.class,
                com.carebridge.backend.security.rbac.Role.class, Boolean.class, Boolean.class,
                org.springframework.data.domain.Pageable.class);
        Query query = searchMethod.getAnnotation(Query.class);
        assertThat(query).isNotNull();
        String jpql = query.value().toLowerCase();

        assertThat(jpql).contains("from user u");
        assertThat(jpql).doesNotContain("user_roles");
        assertThat(jpql).doesNotContain("join");
        assertThat(jpql).doesNotContain("roles r");
    }
}
