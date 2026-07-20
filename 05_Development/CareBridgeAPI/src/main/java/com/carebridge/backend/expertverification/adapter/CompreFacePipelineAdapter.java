package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.FaceDetectionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class CompreFacePipelineAdapter {

    private final FaceDetectionAdapter faceDetectionAdapter;
    private final FaceCropService faceCropService;
    private final FaceVerificationAdapter faceVerificationAdapter;

    public CompreFacePipelineAdapter(
            FaceDetectionAdapter faceDetectionAdapter,
            FaceCropService faceCropService,
            FaceVerificationAdapter faceVerificationAdapter) {
        this.faceDetectionAdapter = faceDetectionAdapter;
        this.faceCropService = faceCropService;
        this.faceVerificationAdapter = faceVerificationAdapter;
    }

    /**
     * Result of the full Detection -> Crop -> Verification pipeline.
     * Contains the verification result, cropped face images, and detection statuses.
     */
    public record PipelineResult(
            FaceVerificationResult verificationResult,
            byte[] croppedSelfie,
            byte[] croppedIdCard,
            FaceDetectionStatus selfieDetectionStatus,
            FaceDetectionStatus idCardDetectionStatus,
            String pipelineStatus
    ) {}

    /**
     * Full Detection -> Crop -> Verification pipeline.
     * Returns the verification result along with the cropped face images.
     */
    public PipelineResult verifyWithPipeline(
            byte[] selfieBytes, String selfieMimeType,
            byte[] idCardFrontBytes, String idCardMimeType) {

        // Step 1: Detect faces in selfie
        FaceDetectionResult selfieDetection = faceDetectionAdapter.detect(selfieBytes, selfieMimeType);
        if (!selfieDetection.hasExactlyOneFace()) {
            return new PipelineResult(
                    new FaceVerificationResult(
                            mapDetectionStatus(selfieDetection.status()),
                            null,
                            getThreshold(),
                            "SELFIE_DETECTION_FAILED: " + selfieDetection.providerErrorCode()),
                    null, null, selfieDetection.status(), null,
                    selfieDetection.status().name());
        }

        // Step 2: Detect faces in ID card front
        FaceDetectionResult idCardDetection = faceDetectionAdapter.detect(idCardFrontBytes, idCardMimeType);
        if (!idCardDetection.hasExactlyOneFace()) {
            return new PipelineResult(
                    new FaceVerificationResult(
                            mapDetectionStatus(idCardDetection.status()),
                            null,
                            getThreshold(),
                            "ID_CARD_DETECTION_FAILED: " + idCardDetection.providerErrorCode()),
                    null, null, null, idCardDetection.status(),
                    idCardDetection.status().name());
        }

        // Step 3: Crop selfie face
        FaceBoundingBox selfieBox = selfieDetection.faces().get(0);
        Optional<byte[]> croppedSelfie = faceCropService.cropFace(selfieBytes, selfieBox, selfieMimeType);
        if (croppedSelfie.isEmpty()) {
            return new PipelineResult(
                    new FaceVerificationResult(
                            FaceVerificationStatus.RETRYABLE_ERROR,
                            null,
                            getThreshold(),
                            "SELFIE_CROP_FAILED"),
                    null, null, FaceDetectionStatus.DETECTED, FaceDetectionStatus.DETECTED,
                    "CROP_ERROR");
        }

        // Step 4: Crop ID card face
        FaceBoundingBox idCardBox = idCardDetection.faces().get(0);
        Optional<byte[]> croppedIdCard = faceCropService.cropFace(idCardFrontBytes, idCardBox, idCardMimeType);
        if (croppedIdCard.isEmpty()) {
            return new PipelineResult(
                    new FaceVerificationResult(
                            FaceVerificationStatus.RETRYABLE_ERROR,
                            null,
                            getThreshold(),
                            "ID_CARD_CROP_FAILED"),
                    null, null, FaceDetectionStatus.DETECTED, FaceDetectionStatus.DETECTED,
                    "CROP_ERROR");
        }

        // Step 5: Run verification on cropped faces
        FaceVerificationResult verificationResult = faceVerificationAdapter.verify(
                croppedSelfie.get(), selfieMimeType,
                croppedIdCard.get(), idCardMimeType);

        return new PipelineResult(verificationResult, croppedSelfie.get(), croppedIdCard.get(),
                FaceDetectionStatus.DETECTED, FaceDetectionStatus.DETECTED,
                verificationResult.status().name());
    }

    private BigDecimal getThreshold() {
        if (faceVerificationAdapter instanceof CompreFaceVerificationAdapter adapter) {
            return adapter.getThreshold();
        }
        return null;
    }

    private FaceVerificationStatus mapDetectionStatus(FaceDetectionStatus status) {
        return switch (status) {
            case NO_FACE -> FaceVerificationStatus.NO_FACE;
            case MULTIPLE_FACES -> FaceVerificationStatus.MULTIPLE_FACES;
            case LOW_QUALITY -> FaceVerificationStatus.RETRYABLE_ERROR;
            case PROVIDER_ERROR -> FaceVerificationStatus.RETRYABLE_ERROR;
            default -> FaceVerificationStatus.RETRYABLE_ERROR;
        };
    }
}