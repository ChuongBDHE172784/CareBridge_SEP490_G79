package com.carebridge.backend.account.repository;

import com.carebridge.backend.account.entity.AccountDeletionRequest;
import com.carebridge.backend.account.entity.DeletionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, UUID> {

    Optional<AccountDeletionRequest> findByUserIdAndStatus(UUID userId, DeletionStatus status);

    List<AccountDeletionRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, DeletionStatus status);
}
