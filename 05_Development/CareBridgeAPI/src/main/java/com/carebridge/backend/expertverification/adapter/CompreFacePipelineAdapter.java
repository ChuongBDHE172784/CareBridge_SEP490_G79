package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.FaceDetectionStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        selfieBytes = autoRotateExif(selfieBytes);
        idCardFrontBytes = autoRotateExif(idCardFrontBytes);

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

        // Step 2: Detect faces in ID card front (with auto-rotate retry)
        FaceDetectionResult idCardDetection = faceDetectionAdapter.detect(idCardFrontBytes, idCardMimeType);
        byte[] effectiveIdCardBytes = idCardFrontBytes;
        String effectiveIdCardMimeType = idCardMimeType;

        if (!idCardDetection.hasExactlyOneFace()) {
            // ID card might be rotated — try 90°, 180°, 270° rotations
            int[] rotations = {90, 180, 270};
            for (int degrees : rotations) {
                byte[] rotated = rotateImage(idCardFrontBytes, idCardMimeType, degrees);
                if (rotated != null) {
                    FaceDetectionResult rotatedDetection = faceDetectionAdapter.detect(rotated, "image/jpeg");
                    if (rotatedDetection.hasExactlyOneFace()) {
                        idCardDetection = rotatedDetection;
                        effectiveIdCardBytes = rotated;
                        effectiveIdCardMimeType = "image/jpeg";
                        break;
                    }
                }
            }
        }

        if (!idCardDetection.hasExactlyOneFace()) {
            return new PipelineResult(
                    new FaceVerificationResult(
                            mapDetectionStatus(idCardDetection.status()),
                            null,
                            getThreshold(),
                            "ID_CARD_DETECTION_FAILED: " + idCardDetection.providerErrorCode()),
                    null, null, selfieDetection.status(), idCardDetection.status(),
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

        // Step 4: Crop ID card face (using potentially rotated image)
        FaceBoundingBox idCardBox = idCardDetection.faces().get(0);
        Optional<byte[]> croppedIdCard = faceCropService.cropFace(effectiveIdCardBytes, idCardBox, effectiveIdCardMimeType);
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
                croppedIdCard.get(), effectiveIdCardMimeType);

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

    private byte[] autoRotateExif(byte[] imageBytes) {
        try {
            com.drew.metadata.Metadata metadata = com.drew.imaging.ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
            com.drew.metadata.exif.ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);
            if (directory == null || !directory.containsTag(com.drew.metadata.exif.ExifIFD0Directory.TAG_ORIENTATION)) {
                return imageBytes;
            }
            int orientation = directory.getInt(com.drew.metadata.exif.ExifIFD0Directory.TAG_ORIENTATION);
            int degrees = 0;
            switch (orientation) {
                case 3: degrees = 180; break;
                case 6: degrees = 90; break;
                case 8: degrees = 270; break;
            }
            if (degrees == 0) return imageBytes;
            byte[] rotated = rotateImage(imageBytes, "image/jpeg", degrees);
            return rotated != null ? rotated : imageBytes;
        } catch (Exception ex) {
            return imageBytes;
        }
    }

    /**
     * Rotates an image by the specified degrees (90, 180, 270).
     * Returns JPEG bytes of the rotated image, or null on failure.
     */
    private byte[] rotateImage(byte[] imageBytes, String mimeType, int degrees) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) return null;

            int w = original.getWidth();
            int h = original.getHeight();
            boolean swap = (degrees == 90 || degrees == 270);
            int newW = swap ? h : w;
            int newH = swap ? w : h;

            BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double radians = Math.toRadians(degrees);
            g2d.translate(newW / 2.0, newH / 2.0);
            g2d.rotate(radians);
            g2d.translate(-w / 2.0, -h / 2.0);
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(rotated, "jpg", out);
            return out.toByteArray();
        } catch (Exception ex) {
            return null;
        }
    }
}