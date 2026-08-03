import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/services/imu_fall_detector.dart';

void main() {
  test('round-trips a JSON-safe sampling snapshot', () {
    final snapshot = ImuDiagnosticsSnapshot(
      generation: 7,
      state: ImuSamplingState.sampling,
      capturedAt: DateTime.utc(2026, 8, 4, 2, 3, 4),
      sampleRateHz: 48.5,
      accelerationMagnitude: 9.81,
      gyroscopeMagnitude: 0.12,
      detectorPhase: FallDetectionPhase.impact,
      detectorReason: ImuDetectorDecisionReason.impactDetected,
    );

    expect(ImuDiagnosticsSnapshot.fromJson(snapshot.toJson()), snapshot);
  });

  test('rejects malformed payloads without throwing', () {
    expect(ImuDiagnosticsSnapshot.tryParse({'state': 'sampling'}), isNull);
    expect(ImuDiagnosticsSnapshot.tryParse('invalid'), isNull);
    expect(
      ImuDiagnosticsSnapshot.tryParse({
        'generation': 1,
        'state': 'sampling',
        'capturedAt': DateTime.utc(2026).toIso8601String(),
        'sampleRateHz': double.nan,
      }),
      isNull,
    );
  });

  test('sanitizes non-finite sensor values before transport', () {
    final json = ImuDiagnosticsSnapshot(
      generation: 1,
      state: ImuSamplingState.sampling,
      capturedAt: DateTime.utc(2026),
      sampleRateHz: double.infinity,
      accelerationMagnitude: double.nan,
      gyroscopeMagnitude: double.negativeInfinity,
    ).toJson();

    expect(json['sampleRateHz'], isNull);
    expect(json['accelerationMagnitude'], isNull);
    expect(json['gyroscopeMagnitude'], isNull);
  });

  test('provides truthful localized lifecycle labels', () {
    expect(ImuSamplingState.awaitingSamples.label, 'Đang chờ dữ liệu cảm biến');
    expect(ImuSamplingState.stale.label, 'Dữ liệu cảm biến đã cũ');
    expect(ImuSamplingState.error.label, 'Lỗi luồng cảm biến');
  });
}
