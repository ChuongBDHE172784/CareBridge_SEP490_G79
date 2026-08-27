package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.DeviceToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false, dt.updatedAt = :now WHERE dt.token = :token")
    int deactivateByToken(@Param("token") String token, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false, dt.updatedAt = :now "
            + "WHERE dt.token = :token AND dt.userId = :userId")
    int deactivateByUserIdAndToken(
            @Param("userId") UUID userId,
            @Param("token") String token,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false, dt.updatedAt = :now "
            + "WHERE dt.token = :token AND dt.userId <> :userId")
    int deactivateByTokenForOtherUsers(
            @Param("userId") UUID userId,
            @Param("token") String token,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false, dt.updatedAt = :now WHERE dt.userId = :userId")
    int deactivateAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
