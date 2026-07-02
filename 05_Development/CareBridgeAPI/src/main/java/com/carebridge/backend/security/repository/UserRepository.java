package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, java.util.UUID> {

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
}
