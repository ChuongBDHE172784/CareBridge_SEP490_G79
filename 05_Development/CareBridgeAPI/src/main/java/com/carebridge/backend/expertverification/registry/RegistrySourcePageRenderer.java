package com.carebridge.backend.expertverification.registry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prepares the registry's own result page to be served back from a CareBridge URL
 * (MF-05 Spec 05 §8.4.1).
 *
 * <p>This is third-party HTML rendered under our origin, so it is defanged before it goes out:
 * scripts removed, forms made inert, and a {@code <base>} added so the portal's stylesheet still
 * loads and the page looks like the original. The caller must also send the CSP header — see
 * {@link #CONTENT_SECURITY_POLICY}. Without both halves an injected script would run with the
 * admin's session.
 */
@Component
public class RegistrySourcePageRenderer {

    public static final String CONTENT_SECURITY_POLICY =
            "sandbox; default-src 'none'; "
                    + "style-src https://thongtin.medinet.org.vn 'unsafe-inline'; "
                    + "img-src https://thongtin.medinet.org.vn data:; "
                    + "font-src https://thongtin.medinet.org.vn";

    private final String assetBaseUrl;

    public RegistrySourcePageRenderer(
            @Value("${carebridge.registry.hcm-medinet.asset-base-url:https://thongtin.medinet.org.vn/}") String assetBaseUrl) {
        this.assetBaseUrl = assetBaseUrl;
    }

    public String render(String rawHtml) {
        Document document = Jsoup.parse(rawHtml, assetBaseUrl);

        document.select("script, noscript").remove();
        document.select("iframe, object, embed").remove();

        // Every control on the page is driven by a view state that has already expired, so leaving
        // the forms live would only produce confusing errors for the admin.
        for (Element form : document.select("form")) {
            form.removeAttr("action");
            form.removeAttr("method");
            form.attr("onsubmit", "return false");
        }
        document.select("[onclick], [onchange], [onload], [onsubmit]").forEach(element -> {
            element.removeAttr("onclick");
            element.removeAttr("onchange");
            element.removeAttr("onload");
        });

        Element head = document.head();
        // Relative asset paths would otherwise resolve against the CareBridge domain and the page
        // would render unstyled.
        head.prependElement("meta")
                .attr("name", "referrer")
                .attr("content", "no-referrer");
        head.prependElement("base").attr("href", assetBaseUrl);

        return document.html();
    }
}
