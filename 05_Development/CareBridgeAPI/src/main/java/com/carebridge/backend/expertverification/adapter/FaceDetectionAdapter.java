package com.carebridge.backend.expertverification.adapter;

import com.carebridge.backend.expertverification.adapter.FaceDetectionResult;

public interface FaceDetectionAdapter {

    FaceDetectionResult detect(byte[] imageBytes, String mimeType);
}