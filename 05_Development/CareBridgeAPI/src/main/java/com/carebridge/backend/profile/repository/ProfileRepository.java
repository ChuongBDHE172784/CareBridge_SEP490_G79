package com.carebridge.backend.profile.repository;

import com.carebridge.backend.profile.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query(value = "SELECT p.* FROM persons p JOIN users u ON u.person_id=p.person_id WHERE u.user_id=:userId", nativeQuery = true)
    Optional<UserProfile> findByUserId(@Param("userId") UUID userId);
}
