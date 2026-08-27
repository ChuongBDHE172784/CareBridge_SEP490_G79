package com.carebridge.backend.expertverification.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/**
 * Parser tests run against {@code registry/hcm-matched.html}, which is the real response captured
 * from the portal on 2026-08-16 — not a hand-written mock.
 */
class HcmMedinetRegistrySourceTest {

    private final HcmMedinetRegistrySource source =
            new HcmMedinetRegistrySource(true, "https://example.invalid", 15_000, 0L, "test-agent");

    private static Document fixture() throws IOException {
        Path path = Path.of("src/test/resources/registry/hcm-matched.html");
        return Jsoup.parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    void discoversModuleIdDynamically() {
        String html = "<input name=\"dnn$ctr999$TimKiemCCHNY$txtSoChungChiSearch\">";

        assertThat(HcmMedinetRegistrySource.discoverModuleId(html)).contains("999");
    }

    @Test
    void discoversTheModuleIdUsedByTheLiveFixture() throws IOException {
        assertThat(HcmMedinetRegistrySource.discoverModuleId(fixture().html())).contains("419");
    }

    @Test
    void returnsEmptyModuleIdWhenThePatternIsGone() {
        assertThat(HcmMedinetRegistrySource.discoverModuleId("<html><body>nothing</body></html>")).isEmpty();
        assertThat(HcmMedinetRegistrySource.discoverModuleId(null)).isEmpty();
    }

    @Test
    void parsesTheMatchedRowFromTheRealResponse() throws IOException {
        RegistryQueryResult result = source.parseResultPage(fixture(), "000001/HCM-CCHN");

        assertThat(result.status()).isEqualTo(RegistryQueryResult.Status.OK);
        assertThat(result.rows()).hasSize(1);

        RegistryRow row = result.rows().get(0);
        assertThat(row.fullName()).isEqualTo("Phạm Thị Bình Minh");
        assertThat(row.nationality()).isEqualTo("Việt Nam");
        assertThat(row.licenseNo()).isEqualTo("000001/HCM-CCHN");
        assertThat(row.practiceScope()).contains("chuyên khoa Nhi");
        assertThat(row.statusText()).isEqualTo("Đang hoạt động");
        assertThat(row.sourceRecordId()).isEqualTo("178224");
        assertThat(row.rowHtml()).isNotBlank();
        assertThat(result.pageHtml()).isNotBlank();
    }

    /**
     * The failure this guards against is the dangerous one: post the wrong field name and the
     * portal answers 200 with its default page. Reporting that as "not found" would look like a
     * clean answer while being none at all.
     */
    @Test
    void reportsSourceErrorWhenTheSearchBoxDoesNotEchoWhatWeSubmitted() throws IOException {
        RegistryQueryResult result = source.parseResultPage(fixture(), "999999/HN-CCHN");

        assertThat(result.status()).isEqualTo(RegistryQueryResult.Status.SOURCE_ERROR);
        assertThat(result.rows()).isEmpty();
    }

    @Test
    void reportsSourceErrorWhenTheResultGridDisappears() {
        Document withoutGrid = Jsoup.parse(
                "<html><body><input name=\"dnn$ctr419$TimKiemCCHNY$txtSoChungChiSearch\" value=\"1/HCM-CCHN\">"
                        + "</body></html>");

        RegistryQueryResult result = source.parseResultPage(withoutGrid, "1/HCM-CCHN");

        assertThat(result.status()).isEqualTo(RegistryQueryResult.Status.SOURCE_ERROR);
    }

    @Test
    void returnsDisabledWithoutTouchingTheNetworkWhenSwitchedOff() {
        HcmMedinetRegistrySource disabled =
                new HcmMedinetRegistrySource(false, "https://example.invalid", 15_000, 0L, "test-agent");

        assertThat(disabled.lookup("000001/HCM-CCHN").status())
                .isEqualTo(RegistryQueryResult.Status.DISABLED);
        assertThat(disabled.isEnabled()).isFalse();
    }
}
