package com.carebridge.backend.expertverification.registry;

import java.util.List;

/**
 * Outcome of one call to a {@link RegistrySource}.
 *
 * @param status  whether the source answered at all — distinct from whether it found anything
 * @param rows    parsed result rows, empty when the source answered with no match
 * @param pageHtml full result page, kept in memory only, served back to admins by
 *                 {@code RegistryHtmlCache} and never persisted (§9)
 */
public record RegistryQueryResult(Status status, List<RegistryRow> rows, String pageHtml) {

    public enum Status {
        /** The source answered with a well-formed result page. {@code rows} may still be empty. */
        OK,
        /**
         * The source could not be reached, timed out, or returned markup we no longer recognise.
         * Never reported to admins as "not found" — see §6.3.
         */
        SOURCE_ERROR,
        /** The integration is switched off by configuration. */
        DISABLED
    }

    public static RegistryQueryResult ok(List<RegistryRow> rows, String pageHtml) {
        return new RegistryQueryResult(Status.OK, List.copyOf(rows), pageHtml);
    }

    public static RegistryQueryResult sourceError() {
        return new RegistryQueryResult(Status.SOURCE_ERROR, List.of(), null);
    }

    public static RegistryQueryResult disabled() {
        return new RegistryQueryResult(Status.DISABLED, List.of(), null);
    }
}
