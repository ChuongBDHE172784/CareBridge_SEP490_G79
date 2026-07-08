package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.OtpVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    long deleteByExpiresAtBefore(Instant expiresAt);
}
