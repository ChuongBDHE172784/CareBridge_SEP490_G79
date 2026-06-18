package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.OtpVerification;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    long deleteByExpiresAtBefore(Instant expiresAt);
}
