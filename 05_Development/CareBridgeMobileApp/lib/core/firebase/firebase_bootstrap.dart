import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';

import '../../firebase_options.dart';

class FirebaseBootstrap {
  const FirebaseBootstrap._();

  static bool supportsPlatform({bool? isWeb, TargetPlatform? platform}) {
    if (isWeb ?? kIsWeb) return true;
    final targetPlatform = platform ?? defaultTargetPlatform;
    return targetPlatform == TargetPlatform.android ||
        targetPlatform == TargetPlatform.iOS;
  }

  static Future<bool> initialize({
    bool? isWeb,
    TargetPlatform? platform,
    Future<void> Function(FirebaseOptions? options)? initializeApp,
  }) async {
    final web = isWeb ?? kIsWeb;
    final targetPlatform = platform ?? defaultTargetPlatform;
    if (!supportsPlatform(isWeb: web, platform: targetPlatform)) return false;

    final options = web
        ? DefaultFirebaseOptions.web
        : targetPlatform == TargetPlatform.iOS
        ? DefaultFirebaseOptions.ios
        : null;
    if (initializeApp != null) {
      await initializeApp(options);
    } else if (options != null) {
      await Firebase.initializeApp(options: options);
    } else {
      // Preserve the existing Android setup, which reads google-services.json.
      await Firebase.initializeApp();
    }
    return true;
  }
}
