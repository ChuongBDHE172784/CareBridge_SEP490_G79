package com.carebridge.backend.expertverification.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The renderer serves someone else's HTML under our own origin, so these are security tests, not
 * cosmetic ones (MF-05 Spec 05 §8.4.1).
 */
class RegistrySourcePageRendererTest {

    private final RegistrySourcePageRenderer renderer =
            new RegistrySourcePageRenderer("https://thongtin.medinet.org.vn/");

    @Test
    void removesEveryScriptFromTheSourcePage() {
        String rendered = renderer.render(
                "<html><head><script>alert(1)</script></head>"
                        + "<body><p>ok</p><script src=\"/x.js\"></script></body></html>");

        assertThat(rendered).doesNotContain("<script");
        assertThat(rendered).doesNotContain("alert(1)");
        assertThat(rendered).contains("ok");
    }

    @Test
    void stripsInlineEventHandlers() {
        String rendered = renderer.render("<html><body><div onclick=\"steal()\">x</div></body></html>");

        assertThat(rendered).doesNotContain("onclick");
    }

    @Test
    void removesEmbeddedFramesAndObjects() {
        String rendered = renderer.render(
                "<html><body><iframe src=\"//evil\"></iframe><object data=\"x\"></object></body></html>");

        assertThat(rendered).doesNotContain("<iframe").doesNotContain("<object");
    }

    /** Without the base tag the portal's relative CSS resolves against CareBridge and the page breaks. */
    @Test
    void addsBaseTagSoTheSourceStylesheetStillResolves() {
        String rendered = renderer.render("<html><head></head><body>x</body></html>");

        assertThat(rendered).contains("<base href=\"https://thongtin.medinet.org.vn/\"");
    }

    /** Asset requests would otherwise leak the internal admin URL, which carries the credential id. */
    @Test
    void addsNoReferrerSoInternalUrlsDoNotLeakToTheSource() {
        String rendered = renderer.render("<html><head></head><body>x</body></html>");

        assertThat(rendered).contains("name=\"referrer\"").contains("content=\"no-referrer\"");
    }

    @Test
    void makesFormsInertBecauseTheViewStateHasAlreadyExpired() {
        String rendered = renderer.render(
                "<html><body><form method=\"post\" action=\"/search\"><input name=\"q\"></form></body></html>");

        assertThat(rendered).doesNotContain("action=\"/search\"");
        assertThat(rendered).contains("onsubmit=\"return false\"");
    }

    @Test
    void declaresASandboxingContentSecurityPolicy() {
        assertThat(RegistrySourcePageRenderer.CONTENT_SECURITY_POLICY)
                .contains("sandbox")
                .contains("default-src 'none'");
    }
}
