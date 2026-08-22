package com.carebridge.backend.expertverification.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Queries the HCM Department of Health public practice-licence portal.
 *
 * <p>The portal is an ASP.NET WebForms page inside DotNetNuke, so a lookup needs two requests: a
 * GET to collect cookies plus the {@code __VIEWSTATE}/{@code __EVENTVALIDATION} tokens, then a
 * POST that replays them together with the licence number. Field names were captured from a real
 * browser session on 2026-08-16 (MF-05 Spec 05 §6.1).
 *
 * <p>All of this stays server-side. Nothing here — URL, field names, view state — is ever sent to
 * a browser (§4.4).
 */
@Component
@Slf4j
public class HcmMedinetRegistrySource implements RegistrySource {

    public static final String SOURCE_CODE = "HCM_MEDINET";

    /**
     * The DotNetNuke module id changes whenever the portal's layout is edited, so it is discovered
     * per request rather than hardcoded (§3.6).
     */
    private static final Pattern MODULE_ID_PATTERN = Pattern.compile("dnn\\$ctr(\\d+)\\$TimKiemCCHNY");

    private static final String SEARCH_FIELD = "txtSoChungChiSearch";
    private static final String NAME_FIELD = "txtHoTenSearch";
    private static final String SEARCH_BUTTON = "btnSearchCCHN";
    private static final String RESULT_GRID_SUFFIX = "grvCCHNY";
    private static final int EXPECTED_COLUMNS = 7;
    private static final long RETRY_BACKOFF_MS = 3_000L;

    private final boolean enabled;
    private final String baseUrl;
    private final int timeoutMs;
    private final long minIntervalMs;
    private final String userAgent;

    /** Shared across the whole instance: the source is public infrastructure, not a per-user quota. */
    private final Object throttleLock = new Object();

    private long lastRequestAt;

    public HcmMedinetRegistrySource(
            @Value("${carebridge.registry.hcm-medinet.enabled:false}") boolean enabled,
            @Value("${carebridge.registry.hcm-medinet.base-url:https://thongtin.medinet.org.vn/Chứng-chỉ-hành-nghề}") String baseUrl,
            @Value("${carebridge.registry.hcm-medinet.timeout-ms:15000}") int timeoutMs,
            @Value("${carebridge.registry.hcm-medinet.min-interval-ms:2000}") long minIntervalMs,
            @Value("${carebridge.registry.hcm-medinet.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36}") String userAgent) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.minIntervalMs = minIntervalMs;
        this.userAgent = userAgent;
    }

    @Override
    public String sourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public String displayName() {
        return "Cổng tra cứu Sở Y tế TP.HCM";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public RegistryQueryResult lookup(String licenseNumber) {
        if (!enabled) {
            return RegistryQueryResult.disabled();
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            return RegistryQueryResult.ok(List.of(), null);
        }

        RegistryQueryResult first = throttledAttempt(licenseNumber);
        if (first.status() != RegistryQueryResult.Status.SOURCE_ERROR) {
            return first;
        }

        // One retry only: this runs while an admin waits, and the source is public infrastructure.
        try {
            Thread.sleep(RETRY_BACKOFF_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return RegistryQueryResult.sourceError();
        }
        return throttledAttempt(licenseNumber);
    }

    private RegistryQueryResult throttledAttempt(String licenseNumber) {
        awaitThrottleSlot();
        try {
            return attempt(licenseNumber);
        } catch (RuntimeException | java.io.IOException failure) {
            log.warn("Registry lookup failed source={} reason={}", SOURCE_CODE,
                    failure.getClass().getSimpleName());
            return RegistryQueryResult.sourceError();
        }
    }

    /** Keeps at least {@code minIntervalMs} between outbound calls from this instance. */
    private void awaitThrottleSlot() {
        synchronized (throttleLock) {
            long waitFor = lastRequestAt + minIntervalMs - System.currentTimeMillis();
            if (waitFor > 0) {
                try {
                    throttleLock.wait(waitFor);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestAt = System.currentTimeMillis();
        }
    }

    private RegistryQueryResult attempt(String licenseNumber) throws java.io.IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        Connection.Response init = Jsoup.connect(baseUrl)
                .method(Connection.Method.GET)
                .userAgent(userAgent)
                .timeout(remainingMs(deadline))
                .execute();
        Document page = init.parse();

        String moduleId = discoverModuleId(page.html()).orElse(null);
        if (moduleId == null) {
            log.warn("REGISTRY_CONTRACT_CHANGED source={} reason=MODULE_ID_NOT_FOUND", SOURCE_CODE);
            return RegistryQueryResult.sourceError();
        }

        String prefix = "dnn$ctr" + moduleId + "$TimKiemCCHNY$";
        Connection.Response result = Jsoup.connect(baseUrl)
                .method(Connection.Method.POST)
                .userAgent(userAgent)
                .cookies(init.cookies())
                .timeout(remainingMs(deadline))
                // The search button carries no name attribute; the postback is driven by __EVENTTARGET.
                .data("__EVENTTARGET", prefix + SEARCH_BUTTON)
                .data("__EVENTARGUMENT", "")
                .data("__VIEWSTATE", hiddenValue(page, "__VIEWSTATE"))
                .data("__VIEWSTATEGENERATOR", hiddenValue(page, "__VIEWSTATEGENERATOR"))
                .data("__VIEWSTATEENCRYPTED", hiddenValue(page, "__VIEWSTATEENCRYPTED"))
                .data("__EVENTVALIDATION", hiddenValue(page, "__EVENTVALIDATION"))
                .data("ScriptManager_TSM", hiddenValue(page, "ScriptManager_TSM"))
                .data("StylesheetManager_TSSM", hiddenValue(page, "StylesheetManager_TSSM"))
                .data("ScrollTop", "")
                .data("__dnnVariable", "")
                .data(prefix + SEARCH_FIELD, licenseNumber)
                .data(prefix + NAME_FIELD, "")
                .execute();

        Document resultPage = result.parse();
        return parseResultPage(resultPage, licenseNumber);
    }

    /**
     * Rejects a response that is not the page produced by our own search.
     *
     * <p>WebForms re-renders the search box with the submitted value, so an echo mismatch means the
     * POST field name no longer matches the portal and the server quietly served its default page.
     * Reporting that as "not found" would be the worst possible failure: it looks like a clean
     * answer while being no answer at all (§6.3).
     */
    RegistryQueryResult parseResultPage(Document resultPage, String submittedLicenseNumber) {
        Element searchBox = resultPage.selectFirst("input[name$=" + SEARCH_FIELD + "]");
        String echoed = searchBox == null ? null : searchBox.attr("value");
        if (echoed == null || !echoed.trim().equalsIgnoreCase(submittedLicenseNumber.trim())) {
            log.warn("REGISTRY_CONTRACT_CHANGED source={} reason=SEARCH_ECHO_MISMATCH echoed={}",
                    SOURCE_CODE, echoed);
            return RegistryQueryResult.sourceError();
        }

        Element grid = resultPage.selectFirst("table[id$=" + RESULT_GRID_SUFFIX + "]");
        if (grid == null) {
            log.warn("REGISTRY_CONTRACT_CHANGED source={} reason=RESULT_GRID_MISSING", SOURCE_CODE);
            return RegistryQueryResult.sourceError();
        }

        List<RegistryRow> rows = new ArrayList<>();
        for (Element row : grid.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < EXPECTED_COLUMNS) {
                continue; // header row, or a layout row such as the pager
            }
            rows.add(new RegistryRow(
                    cells.get(1).text().trim(),
                    cells.get(2).text().trim(),
                    cells.get(3).text().trim(),
                    cells.get(4).text().trim(),
                    cells.get(5).text().trim(),
                    cells.get(6).text().trim(),
                    row.outerHtml()));
        }
        return RegistryQueryResult.ok(rows, resultPage.html());
    }

    /** Parsed per request — the number in {@code dnn$ctr419$…} is not stable (§3.6). */
    static Optional<String> discoverModuleId(String html) {
        if (html == null) {
            return Optional.empty();
        }
        Matcher matcher = MODULE_ID_PATTERN.matcher(html);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static String hiddenValue(Document page, String name) {
        Element field = page.selectFirst("input[name=" + name + "]");
        return field == null ? "" : field.attr("value");
    }

    private static int remainingMs(long deadline) {
        long remaining = deadline - System.currentTimeMillis();
        // Jsoup treats 0 as "no timeout", which would let a stalled socket hang the admin request.
        return (int) Math.max(1_000L, remaining);
    }
}
