package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.QuestionNotificationMute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionNotificationMuteRepository extends JpaRepository<QuestionNotificationMute, UUID> {

    boolean existsByUserIdAndQuestionId(UUID userId, UUID questionId);

    void deleteByUserIdAndQuestionId(UUID userId, UUID questionId);
}
