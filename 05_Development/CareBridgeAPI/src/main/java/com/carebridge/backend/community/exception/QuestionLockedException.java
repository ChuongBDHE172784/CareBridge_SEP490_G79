package com.carebridge.backend.community.exception;

public class QuestionLockedException extends RuntimeException {

    public QuestionLockedException(String questionId) {
        super("[COM-012] Cannot delete a locked question: " + questionId);
    }
}
