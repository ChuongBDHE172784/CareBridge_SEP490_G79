/**
 * Rich text HTML always contains wrapper markup even when the user has not typed anything
 * (for example, an empty editor is "<p></p>"). Images still count as content.
 */
export function isRichTextEmpty(html: string): boolean {
  if (/<img\b/i.test(html)) return false;
  return html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;|&#160;|&#x0*a0;/gi, ' ')
    .replace(/\u00a0/g, ' ')
    .trim().length === 0;
}
