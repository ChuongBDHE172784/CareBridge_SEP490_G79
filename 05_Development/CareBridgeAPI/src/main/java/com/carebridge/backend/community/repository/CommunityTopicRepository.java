package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityTopic;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityTopicRepository extends JpaRepository<CommunityTopic, UUID> {

    List<CommunityTopic> findAllByOrderBySortOrderAsc();

    List<CommunityTopic> findAllByIsHiddenFalseOrderBySortOrderAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
