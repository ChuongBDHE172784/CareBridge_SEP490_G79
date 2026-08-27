package com.carebridge.backend.expertverification.adapter;

public record FaceBoundingBox(
    double x,
    double y,
    double width,
    double height,
    double probability,
    boolean normalized
) {
    // CompreFace returns coordinates as normalized values [0-1] or pixels depending on version
    // We handle both by checking if values > 1 (pixel coordinates) or <= 1 (normalized)

    public FaceBoundingBox(double x, double y, double width, double height, double probability) {
        this(x, y, width, height, probability, false);
    }
}