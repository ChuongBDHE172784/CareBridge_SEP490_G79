import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/firebase/firebase_bootstrap.dart';
import 'package:untitled/firebase_options.dart';

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

    test('supports web and skips unconfigured desktop platforms', () {
      expect(
        FirebaseBootstrap.supportsPlatform(
          isWeb: true,
          platform: TargetPlatform.iOS,
        ),
        isTrue,
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

  group('FirebaseBootstrap.initialize', () {
    test('passes the explicit existing Firebase Web options', () async {
      Object? receivedOptions;

      final initialized = await FirebaseBootstrap.initialize(
        isWeb: true,
        platform: TargetPlatform.windows,
        initializeApp: (options) async => receivedOptions = options,
      );

      expect(initialized, isTrue);
      expect(receivedOptions, same(DefaultFirebaseOptions.web));
      expect(
        DefaultFirebaseOptions.web.projectId,
        'project-d04b488f-17fb-4ae5-b64',
      );
      expect(
        DefaultFirebaseOptions.web.appId,
        '1:772548995876:web:570bb88680e8f883f261b1',
      );
    });

    test('preserves Android google-services initialization', () async {
      Object? receivedOptions = Object();

      final initialized = await FirebaseBootstrap.initialize(
        isWeb: false,
        platform: TargetPlatform.android,
        initializeApp: (options) async => receivedOptions = options,
      );

      expect(initialized, isTrue);
      expect(receivedOptions, isNull);
    });

    test('passes the generated Firebase options on iOS', () async {
      Object? receivedOptions;

      final initialized = await FirebaseBootstrap.initialize(
        isWeb: false,
        platform: TargetPlatform.iOS,
        initializeApp: (options) async => receivedOptions = options,
      );

      expect(initialized, isTrue);
      expect(receivedOptions, same(DefaultFirebaseOptions.ios));
    });

    test('does not invoke Firebase on unsupported desktop platforms', () async {
      var initializeCalls = 0;

      final initialized = await FirebaseBootstrap.initialize(
        isWeb: false,
        platform: TargetPlatform.windows,
        initializeApp: (_) async => initializeCalls++,
      );

      expect(initialized, isFalse);
      expect(initializeCalls, 0);
    });
  });
}
