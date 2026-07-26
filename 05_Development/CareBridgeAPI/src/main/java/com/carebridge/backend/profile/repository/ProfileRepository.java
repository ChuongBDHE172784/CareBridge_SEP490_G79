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

    @Query("select p from UserProfile p where p.profileId = :userId")
    Optional<UserProfile> findByUserId(@Param("userId") UUID userId);
}
