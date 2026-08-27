package com.carebridge.backend.exercise.repository;

import com.carebridge.backend.exercise.entity.PostureFeedbackEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostureFeedbackEventRepository
        extends JpaRepository<PostureFeedbackEvent, UUID> {

    List<PostureFeedbackEvent> findByExerciseSessionId(UUID exerciseSessionId);
}
