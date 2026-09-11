package com.carebridge.backend.expertavailability.availabilitystatus;

public enum AvailabilityStatus {
    AVAILABLE,
    BUSY,
    UNAVAILABLE,
    /**
     * A mother has taken this slot. Distinct from BUSY, which the expert sets on
     * themselves: the column is a plain varchar, so rows carrying this value were
     * already reaching the reader and failing it — one of them made the whole
     * availability screen answer 500 and show no slots at all.
     *
     * <p>Everything that consults the status branches on AVAILABLE or not, so a
     * booked slot is correctly excluded from what mothers can book and, in
     * replaceAvailability, preserved rather than deleted when the expert rewrites
     * a day.
     */
    BOOKED
}
