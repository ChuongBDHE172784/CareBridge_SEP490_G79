import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/widgets/verified_content_body.dart';

// RTE-TC-014 — ContentRichTextEditor_Test-Spec.md §4. Content Admin's web editor (ADR-RTE-002)
// produces HTML; this screen must render it as formatted widgets, not raw tag text
// (ADR-RTE-006).
void main() {
  testWidgets(
    'renders HTML tags as formatted widgets, not raw tag text',
    (tester) async {
      const html = '<p>Xin chào <b>mẹ bầu</b></p>';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(body: VerifiedContentBody(html: html)),
        ),
      );
      await tester.pumpAndSettle();

      // The literal raw-HTML string must never appear as rendered text.
      expect(find.text(html), findsNothing);
      expect(find.textContaining('<p>'), findsNothing);
      expect(find.textContaining('<b>'), findsNothing);

      // The actual words are still shown, split by flutter_html into RichText spans
      // (bold text becomes a separate InlineSpan, so "Xin chào" and "mẹ bầu" won't
      // necessarily be found via find.text on the same node — search rendered spans
      // instead).
      final richTextFinder = find.byType(RichText);
      expect(richTextFinder, findsWidgets);
      final renderedText = tester
          .widgetList<RichText>(richTextFinder)
          .map((w) => w.text.toPlainText())
          .join(' ');
      expect(renderedText, contains('Xin chào'));
      expect(renderedText, contains('mẹ bầu'));
    },
  );

  testWidgets('renders an <img> tag as an actual Image widget, not literal text', (
    tester,
  ) async {
    const html =
        '<p>Ảnh minh hoạ: <img src="https://res.cloudinary.com/demo/x.jpg"></p>';

    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: VerifiedContentBody(html: html))),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('<img'), findsNothing);
    expect(find.byType(Image), findsWidgets);
  });
}
