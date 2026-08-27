package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.exercise.dto.SessionStateResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.impl.ExerciseSessionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExercisePauseResumeServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Mock
    private ExerciseSessionRepository sessionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseSafetyCheckRepository safetyCheckRepository;

    @Mock
    private PostureFeedbackEventRepository postureFeedbackEventRepository;

    @Spy
    private ExerciseSessionMapper sessionMapper;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExerciseSessionServiceImpl service;

    private ExerciseSession session(SessionStatus status, UUID owner) {
        return ExerciseSession.builder()
                .exerciseSessionId(SESSION_ID)
                .userId(owner)
                .sessionStatus(status)
                .pausedSeconds(0)
                .warningCount(0)
                .startedAt(OffsetDateTime.now().minusMinutes(5))
                .updatedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    @DisplayName("PAUSE-TC-001: IN_PROGRESS → PAUSED with warningCount incremented")
    void pause_inProgress_pausesAndIncrementsWarning() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, USER_ID)));
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionStateResponse response = service.pauseSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.PAUSED.name());
        assertThat(response.getWarningCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("PAUSE-TC-002: already PAUSED → InvalidSessionStateException EXSESS-005")
    void pause_alreadyPaused_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.PAUSED, USER_ID)));

        assertThatThrownBy(() -> service.pauseSession(SESSION_ID, USER_ID))
                .isInstanceOf(InvalidSessionStateException.class)
                .extracting("code").isEqualTo("EXSESS-005");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("PAUSE-TC-003: wrong user → SessionOwnershipException EXSESS-010")
    void pause_wrongUser_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, OTHER_USER_ID)));

        assertThatThrownBy(() -> service.pauseSession(SESSION_ID, USER_ID))
                .isInstanceOf(SessionOwnershipException.class)
                .extracting("code").isEqualTo("EXSESS-010");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("RESUME-TC-001: PAUSED → IN_PROGRESS with pausedSeconds accumulated")
    void resume_paused_resumesAndAccumulatesPausedSeconds() {
        ExerciseSession paused = session(SessionStatus.PAUSED, USER_ID);
        paused.setPausedSeconds(10);
        paused.setUpdatedAt(OffsetDateTime.now().minusSeconds(30));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(paused));
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionStateResponse response = service.resumeSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS.name());
        // 10 prior + ~30 elapsed during pause.
        assertThat(response.getPausedSeconds()).isGreaterThanOrEqualTo(35);
    }

    @Test
    @DisplayName("RESUME-TC-002: not PAUSED → InvalidSessionStateException EXSESS-006")
    void resume_notPaused_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, USER_ID)));

        assertThatThrownBy(() -> service.resumeSession(SESSION_ID, USER_ID))
                .isInstanceOf(InvalidSessionStateException.class)
                .extracting("code").isEqualTo("EXSESS-006");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("RESUME-TC-003: wrong user → SessionOwnershipException")
    void resume_wrongUser_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.PAUSED, OTHER_USER_ID)));

        assertThatThrownBy(() -> service.resumeSession(SESSION_ID, USER_ID))
                .isInstanceOf(SessionOwnershipException.class);

        verify(sessionRepository, never()).save(any());
    }
}
