package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpertCredentialRepository extends JpaRepository<ExpertCredential, UUID> {

    List<ExpertCredential> findByExpertProfileId(UUID expertProfileId);
}
