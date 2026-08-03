import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/services/safety_demo_mode.dart';

void main() {
  test('compile-time demo flag controls release diagnostics', () {
    const expectedDemoMode = bool.fromEnvironment(
      'EXPECT_SAFETY_DEMO',
      defaultValue: false,
    );

    expect(safetyDemoMode, expectedDemoMode);
    expect(safetyDiagnosticsEnabled, kDebugMode || expectedDemoMode);
    expect(
      safetyDiagnosticsModeLabel,
      expectedDemoMode ? 'CHẾ ĐỘ TRÌNH DIỄN' : 'DEBUG',
    );
  });
}
