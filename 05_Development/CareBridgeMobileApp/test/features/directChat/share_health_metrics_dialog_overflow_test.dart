import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/widgets/share_health_metrics_dialog.dart';

void main() {
  testWidgets('ShareHealthMetricsDialog does not overflow RenderFlex on narrow screens', (tester) async {
    FlutterErrorDetails? errorDetails;
    final originalOnError = FlutterError.onError;
    FlutterError.onError = (details) {
      errorDetails = details;
    };
    addTearDown(() {
      FlutterError.onError = originalOnError;
    });

    // Narrow phone width (e.g. 320px)
    tester.view.physicalSize = const Size(320 * 2, 800 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: ShareHealthMetricsDialog(),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Verify dialog loaded fallback metrics
    expect(find.text('Chia sẻ chỉ số sức khỏe'), findsOneWidget);
    expect(find.byType(CheckboxListTile), findsWidgets);

    // Expand the first item if it has history to test history sub-list
    final expandButton = find.byIcon(Icons.expand_more_rounded);
    if (expandButton.evaluate().isNotEmpty) {
      await tester.tap(expandButton.first);
      await tester.pumpAndSettle();
    }

    // Restore FlutterError.onError before performing assertions
    FlutterError.onError = originalOnError;

    if (errorDetails != null) {
      debugPrint('CAPTURED FLUTTER ERROR:\n${errorDetails!.toString()}');
    }
    expect(errorDetails, isNull);
  });

  testWidgets('ShareHealthMetricsDialog handles select all and deselect all without overflow', (tester) async {
    FlutterErrorDetails? errorDetails;
    final originalOnError = FlutterError.onError;
    FlutterError.onError = (details) {
      errorDetails = details;
    };
    addTearDown(() {
      FlutterError.onError = originalOnError;
    });

    tester.view.physicalSize = const Size(375 * 2, 812 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: ShareHealthMetricsDialog(),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Toggle "Bỏ chọn tất cả"
    final selectAllBtn = find.text('Bỏ chọn tất cả');
    expect(selectAllBtn, findsOneWidget);
    await tester.tap(selectAllBtn);
    await tester.pumpAndSettle();

    expect(find.text('Chọn tất cả'), findsOneWidget);

    // Toggle back "Chọn tất cả"
    await tester.tap(find.text('Chọn tất cả'));
    await tester.pumpAndSettle();

    FlutterError.onError = originalOnError;
    expect(errorDetails, isNull);
  });
}
