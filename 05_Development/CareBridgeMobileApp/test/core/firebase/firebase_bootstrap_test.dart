import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/firebase/firebase_bootstrap.dart';

void main() {
  group('FirebaseBootstrap.supportsPlatform', () {
    test('supports configured mobile platforms', () {
      expect(
        FirebaseBootstrap.supportsPlatform(
          isWeb: false,
          platform: TargetPlatform.android,
        ),
        isTrue,
      );
      expect(
        FirebaseBootstrap.supportsPlatform(
          isWeb: false,
          platform: TargetPlatform.iOS,
        ),
        isTrue,
      );
    });

    test('skips web and unconfigured desktop platforms', () {
      expect(
        FirebaseBootstrap.supportsPlatform(
          isWeb: true,
          platform: TargetPlatform.iOS,
        ),
        isFalse,
      );
      for (final platform in [
        TargetPlatform.macOS,
        TargetPlatform.windows,
        TargetPlatform.linux,
      ]) {
        expect(
          FirebaseBootstrap.supportsPlatform(isWeb: false, platform: platform),
          isFalse,
        );
      }
    });
  });
}
