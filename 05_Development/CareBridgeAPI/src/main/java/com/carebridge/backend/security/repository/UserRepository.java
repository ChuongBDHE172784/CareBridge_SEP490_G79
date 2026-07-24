package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, java.util.UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") java.util.UUID id);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailOrPhone(String email, String phone);

    // UC-111: dashboard aggregation — user count grouped by role
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countGroupByRole();

    // UC-111: dashboard aggregation — active predicate (TDS §5.2)
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.locked = false "
            + "AND (u.suspendedUntil IS NULL OR u.suspendedUntil <= :now)")
    long countActive(@Param("now") Instant now);

    // UC-113: impact report — mothers served
    long countByRole(Role role);

    // UC-114 Manage User Accounts: admin-facing search/filter over the existing users
    // table. ADR-IAM-001: scoped to the single-entity `User` — never joins a
    // roles/user_roles table. Each nullable filter is cast to `string`/kept typed so
    // Postgres can resolve the bind-parameter type when the value is null (avoids the
    // "could not determine data type of parameter" error seen elsewhere in the
    // codebase for null-filter JPQL against a real Postgres driver).
    @Query("""
            select u from User u
            where (cast(:email as string) is null or lower(u.email) like lower(concat('%', cast(:email as string), '%')))
              and (cast(:phone as string) is null or u.phone like concat('%', cast(:phone as string), '%'))
              and (cast(:name as string) is null or lower(u.name) like lower(concat('%', cast(:name as string), '%')))
              and (:role is null or u.role = :role)
              and (:enabled is null or u.enabled = :enabled)
              and (:locked is null or u.locked = :locked)
            """)
    Page<User> search(
            @Param("email") String email, @Param("phone") String phone,
            @Param("name") String name, @Param("role") Role role,
            @Param("enabled") Boolean enabled, @Param("locked") Boolean locked,
            Pageable pageable);
}
