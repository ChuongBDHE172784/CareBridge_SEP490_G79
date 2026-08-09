package com.carebridge.backend.health.entity;

public enum MetricType {
    /** Legacy wire value retained for backward-compatible rejection; no longer exposed or accepted. */
    WEIGHT,
    BMI,
    HYDRATION,
    MOOD,
    EPDS_SCORE,
    BLOOD_PRESSURE,
    BLOOD_PRESSURE_SYSTOLIC,
    BLOOD_PRESSURE_DIASTOLIC,
    BLOOD_GLUCOSE,
    FETAL_MOVEMENT_SESSION,
    FETAL_MOVEMENT_COUNT,
    SLEEP_DURATION,
    STEPS_COUNT,
    MATERNAL_HEART_RATE,
    STRESS,
    SPO2,
    TEMPERATURE,
    OTHER
}
