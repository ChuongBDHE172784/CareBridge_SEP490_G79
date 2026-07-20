import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/routes/app_router.dart';

void main() {
  group('mother startup consent gate', () {
    test(
      'cold start dispatches an authenticated mother through auth landing',
      () {
        final redirect = resolveAppRedirect(
          isAuthenticated: true,
          isRestoring: false,
          blockedReason: null,
          role: 'MOTHER',
          location: '/',
        );

        expect(redirect, '/auth-landing');
      },
    );

    test('resolved mother home does not re-enter the startup dispatcher', () {
      final redirect = resolveAppRedirect(
        isAuthenticated: true,
        isRestoring: false,
        blockedReason: null,
        role: 'MOTHER',
        location: '/mother-home',
      );

      expect(redirect, isNull);
    });
  });
}
