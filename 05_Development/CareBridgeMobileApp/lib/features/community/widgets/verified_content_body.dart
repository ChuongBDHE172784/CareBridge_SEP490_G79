import 'package:flutter/material.dart';
import 'package:flutter_html/flutter_html.dart';

import '../../../core/network/api_client.dart';

String resolveVerifiedContentImageUrls(String value) {
  return value.replaceAllMapped(
    RegExp(
      r'''(<img\b[^>]*\ssrc\s*=\s*["'])(/(?!/)[^"']*)''',
      caseSensitive: false,
    ),
    (match) =>
        '${match.group(1)}${resolveVerifiedContentImageUrl(match.group(2)!)}',
  );
}

String resolveVerifiedContentImageUrl(String value) {
  if (value.startsWith('//') || Uri.tryParse(value)?.hasScheme == true) {
    return value;
  }
  return Uri.parse(apiBaseUrl).resolve(value).toString();
}

/// Renders curated article/FAQ body HTML (ADR-RTE-006, ContentRichTextEditor_TDS.md).
/// The Content Admin web editor produces HTML (bold/color/font-size/images), already
/// sanitized server-side before it reaches this screen (ADR-RTE-005) — a plain Text()
/// would show raw tags like "<p><b>..." to the reader instead of formatted content.
class VerifiedContentBody extends StatelessWidget {
  final String html;
  final Color color;

  const VerifiedContentBody({
    super.key,
    required this.html,
    this.color = const Color(0xFF271812),
  });

  @override
  Widget build(BuildContext context) {
    return Html(
      data: resolveVerifiedContentImageUrls(html),
      style: {
        'body': Style(
          margin: Margins.zero,
          fontSize: FontSize(15),
          color: color,
          lineHeight: const LineHeight(1.6),
        ),
      },
    );
  }
}
