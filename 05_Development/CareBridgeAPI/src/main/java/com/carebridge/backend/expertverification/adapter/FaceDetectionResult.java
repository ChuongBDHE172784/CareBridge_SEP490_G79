package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceDetectionStatus;
import java.util.List;

public record FaceDetectionResult(
    FaceDetectionStatus status,
    int faceCount,
    double highestProbability,
    List<FaceBoundingBox> faces,
    String providerErrorCode
) {
    public static FaceDetectionResult detected(List<FaceBoundingBox> faces) {
        double maxProb = faces.stream()
            .mapToDouble(FaceBoundingBox::probability)
            .max()
            .orElse(0.0);
        return new FaceDetectionResult(
            FaceDetectionStatus.DETECTED,
            faces.size(),
            maxProb,
            faces,
            null
        );
    }

    public static FaceDetectionResult noFace() {
        return new FaceDetectionResult(
            FaceDetectionStatus.NO_FACE,
            0,
            0.0,
            List.of(),
            "NO_FACE"
        );
    }

    public static FaceDetectionResult multipleFaces(int count) {
        return new FaceDetectionResult(
            FaceDetectionStatus.MULTIPLE_FACES,
            count,
            0.0,
            List.of(),
            "MULTIPLE_FACES"
        );
    }

    public static FaceDetectionResult lowQuality() {
        return new FaceDetectionResult(
            FaceDetectionStatus.LOW_QUALITY,
            0,
            0.0,
            List.of(),
            "LOW_QUALITY"
        );
    }

    public static FaceDetectionResult providerError(String errorCode) {
        return new FaceDetectionResult(
            FaceDetectionStatus.PROVIDER_ERROR,
            0,
            0.0,
            List.of(),
            errorCode
        );
    }

    public boolean hasExactlyOneFace() {
        return faceCount == 1 && status == FaceDetectionStatus.DETECTED;
    }
}