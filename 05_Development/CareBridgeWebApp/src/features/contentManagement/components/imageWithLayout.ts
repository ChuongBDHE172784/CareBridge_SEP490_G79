import Image from '@tiptap/extension-image';

/**
 * Extends the base Image node with `widthPct`/`align` attributes (ADR-RTE-008,
 * ContentRichTextEditor_TDS.md). Rendered as `data-width-pct`/`data-align` HTML attributes —
 * deliberately not inline style, so the backend sanitizer can allowlist a fixed enum of values
 * instead of opening arbitrary CSS (HtmlContentSanitizer.java WIDTH_PCT_ENUM/ALIGN_ENUM).
 */
export const ImageWithLayout = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      widthPct: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-width-pct'),
        renderHTML: (attributes: Record<string, unknown>) =>
          attributes.widthPct ? { 'data-width-pct': attributes.widthPct } : {},
      },
      align: {
        default: null,
        parseHTML: (element: HTMLElement) => element.getAttribute('data-align'),
        renderHTML: (attributes: Record<string, unknown>) =>
          attributes.align ? { 'data-align': attributes.align } : {},
      },
    };
  },
});
