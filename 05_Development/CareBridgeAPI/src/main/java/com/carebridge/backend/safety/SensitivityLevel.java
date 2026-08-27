package com.carebridge.backend.safety;

public enum SensitivityLevel {
    LOW, MEDIUM, HIGH;

    public double getThreshold() {
        return switch (this) {
            case LOW -> 15.0;
            case MEDIUM -> 12.0;
            case HIGH -> 9.0;
        };
    }
}
