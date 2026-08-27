package com.carebridge.backend.journey.entity;

/** Server-owned source used to establish a pregnancy's canonical dating. */
public enum GestationalDatingBasis {
    LMP,
    EDD,
    /** Compatibility classification for an exact 280-day legacy pair. */
    LMP_DERIVED_FROM_EDD
}
