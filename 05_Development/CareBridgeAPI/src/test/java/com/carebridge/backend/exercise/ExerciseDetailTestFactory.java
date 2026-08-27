package com.carebridge.backend.exercise;

import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class ExerciseDetailTestFactory {

    public static final UUID UUID_PUBLISHED_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID UUID_PUBLISHED_2 = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID UUID_DRAFT = UUID.fromString("00000000-0000-0000-0002-000000000001");
    public static final UUID UUID_ARCHIVED = UUID.fromString("00000000-0000-0000-0003-000000000001");
    public static final UUID UUID_NOT_EXIST = UUID.fromString("00000000-0000-0000-0000-999999999999");
    public static final UUID ADMIN_CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-200000000001");

    private ExerciseDetailTestFactory() {
    }

    public static PregnancyExercise makePublishedExerciseWithFullDetail() {
        PregnancyExercise e = new PregnancyExercise();
        e.setExerciseId(UUID_PUBLISHED_1);
        e.setCreatedBy(ADMIN_CREATOR_ID);
        e.setTitle("Prenatal Yoga - First Trimester");
        e.setDescription("Gentle yoga poses suitable for early pregnancy");
        e.setTrimesterScope(TrimesterScope.FIRST);
        e.setDifficultyLevel(DifficultyLevel.EASY);
        e.setDurationMinutes((short) 20);
        e.setInstructionContent(
                "Step 1: Start in a comfortable seated position on a yoga mat.\n"
                + "Step 2: Inhale deeply and raise both arms above your head.\n"
                + "Step 3: Exhale and lower your arms slowly.");
        e.setMediaUrl("https://cdn.carebridge.com/exercises/prenatal-yoga-t1.mp4");
        e.setSafetyWarning("Stop immediately if you feel dizzy or experience pain.");
        e.setSupportsPostureAnalysis(true);
        e.setStatus(ExerciseStatus.PUBLISHED);
        e.setVersionNo(1);
        e.setCreatedAt(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        e.setUpdatedAt(OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        return e;
    }

    public static PregnancyExercise makePublishedExerciseWithNullSafetyWarning() {
        PregnancyExercise e = makePublishedExerciseWithFullDetail();
        e.setExerciseId(UUID_PUBLISHED_2);
        e.setSafetyWarning(null);
        return e;
    }

    public static PregnancyExercise makeDraftExercise() {
        PregnancyExercise e = makePublishedExerciseWithFullDetail();
        e.setExerciseId(UUID_DRAFT);
        e.setTitle("Draft Exercise - Not Accessible");
        e.setStatus(ExerciseStatus.DRAFT);
        return e;
    }

    public static PregnancyExercise makeArchivedExercise() {
        PregnancyExercise e = makePublishedExerciseWithFullDetail();
        e.setExerciseId(UUID_ARCHIVED);
        e.setTitle("Archived Exercise - Not Accessible");
        e.setStatus(ExerciseStatus.ARCHIVED);
        return e;
    }
}
