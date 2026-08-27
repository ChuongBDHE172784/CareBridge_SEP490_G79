package com.carebridge.backend.expertverification.adapter;

public interface FaceVerificationAdapter {

    /**
     * Verify if selfie face matches the ID card face.
     *
     * @param selfieFaceBytes Cropped selfie face bytes
     * @param selfieMimeType Selfie MIME type
     * @param idCardFaceBytes Cropped ID card face bytes
     * @param idCardMimeType ID card MIME type
     * @return Verification result
     */
    FaceVerificationResult verify(
            byte[] selfieFaceBytes, String selfieMimeType,
            byte[] idCardFaceBytes, String idCardMimeType);
}