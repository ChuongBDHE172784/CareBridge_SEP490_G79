package com.carebridge.backend.content.policy;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.owasp.html.AttributePolicy;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Sanitizes HTML produced by the content admin rich text editor before it is persisted to
 * {@code content_items.body}. See ContentRichTextEditor_TDS.md ADR-RTE-005 for the allowlist
 * rationale — body is rendered unescaped (dangerouslySetInnerHTML on web, flutter_html on
 * mobile), so this is the only place stored XSS is prevented.
 */
@Component
public class HtmlContentSanitizer {

    private static final String CLOUDINARY_IMAGE_PREFIX = "https://res.cloudinary.com/";
    private static final Pattern DIMENSION = Pattern.compile("\\d{1,4}");

    // ADR-RTE-009: text-align added alongside color/font-size/font-family — same low-risk pattern,
    // OWASP's CssSchema validates values per-property so this doesn't open any other CSS surface.
    private static final CssSchema TEXT_STYLE_SCHEMA =
            CssSchema.withProperties(List.of("color", "font-size", "font-family", "text-align"));

    // ADR-RTE-005 §11.4: images may only reference the platform's own Cloudinary delivery
    // host — content is embedded permanently in body and rendered to end users (mothers/
    // family), so arbitrary external hosts would leak reader IP/User-Agent via image requests.
    private static final AttributePolicy CLOUDINARY_ONLY_SRC = (elementName, attributeName, value) -> {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.regionMatches(true, 0, CLOUDINARY_IMAGE_PREFIX, 0, CLOUDINARY_IMAGE_PREFIX.length())
                ? trimmed
                : null;
    };

    // ADR-RTE-008: fixed-enum attribute policies for image resize/align — deliberately NOT CSS
    // (float/margin/width), so the allowlist stays a closed set of known-safe values instead of
    // opening arbitrary style properties. Frontend CSS (RichTextEditor.css / ContentDetailPage.tsx)
    // interprets these data-* attributes; the sanitizer only guarantees the value is one of a few.
    private static final AttributePolicy WIDTH_PCT_ENUM = (elementName, attributeName, value) ->
            value != null && Set.of("25", "50", "75", "100").contains(value) ? value : null;

    private static final AttributePolicy ALIGN_ENUM = (elementName, attributeName, value) ->
            value != null && Set.of("left", "center", "right").contains(value) ? value : null;

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowCommonInlineFormattingElements() // b, i, s, u, strong, em, br, span, ...
            .allowCommonBlockElements() // p, div, h1-h6, ul, ol, li, blockquote
            .allowStyling(TEXT_STYLE_SCHEMA) // color / font-size / font-family only
            .allowUrlProtocols("https") // required for the "src" URL attribute below to pass at all
            .allowElements("img")
            .allowAttributes("alt").onElements("img")
            .allowAttributes("width", "height").matching(DIMENSION).onElements("img")
            .allowAttributes("src").matching(CLOUDINARY_ONLY_SRC).onElements("img")
            .allowAttributes("data-width-pct").matching(WIDTH_PCT_ENUM).onElements("img") // ADR-RTE-008
            .allowAttributes("data-align").matching(ALIGN_ENUM).onElements("img") // ADR-RTE-008
            .toFactory();

    public String sanitize(String rawHtml) {
        if (rawHtml == null) {
            return null;
        }
        return POLICY.sanitize(rawHtml);
    }
}
