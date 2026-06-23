package com.carebridge.backend.content.repository;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<ContentItem, UUID> {

    Optional<ContentItem> findByTitleIgnoreCaseAndStageAndType(
            String title, ContentStage stage, ContentType type);
}
