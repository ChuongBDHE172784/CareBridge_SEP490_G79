import 'package:flutter/foundation.dart';

/// Opt-in presentation tooling for signed committee/demo builds.
///
/// Production release builds remain unchanged unless the compile-time flag is
/// explicitly provided with `--dart-define=ENABLE_SAFETY_DEMO=true`.
const bool safetyDemoMode = bool.fromEnvironment(
  'ENABLE_SAFETY_DEMO',
  defaultValue: false,
);

const bool safetyDiagnosticsEnabled = kDebugMode || safetyDemoMode;

const String safetyDiagnosticsModeLabel = safetyDemoMode
    ? 'CHẾ ĐỘ TRÌNH DIỄN'
    : 'DEBUG';
