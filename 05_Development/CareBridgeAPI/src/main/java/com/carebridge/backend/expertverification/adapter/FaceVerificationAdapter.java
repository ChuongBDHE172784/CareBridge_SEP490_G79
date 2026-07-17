package com.carebridge.backend.expertverification.adapter;

public interface FaceVerificationAdapter {

    FaceVerificationResult verify(
            byte[] selfie, String selfieMimeType,
            byte[] identityFront, String identityFrontMimeType);
}
