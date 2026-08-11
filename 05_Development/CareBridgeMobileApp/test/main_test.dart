import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/main.dart';

void main() {
  test('a detected real fall routes to safety from another app screen', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'OPEN',
        currentPath: '/mother-home',
      ),
      isTrue,
    );
  });

  test('does not replace the active safety countdown route', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'OPEN',
        currentPath: '/safety',
      ),
      isFalse,
    );
  });

  test('does not route resolved events as active fall alerts', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'CONFIRMED_SAFE',
        currentPath: '/mother-home',
      ),
      isFalse,
    );
  });
}
