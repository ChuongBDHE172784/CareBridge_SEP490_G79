import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';

import '../../firebase_options.dart';

class FirebaseBootstrap {
  const FirebaseBootstrap._();

  static bool supportsPlatform({bool? isWeb, TargetPlatform? platform}) {
    if (isWeb ?? kIsWeb) return false;
    final targetPlatform = platform ?? defaultTargetPlatform;
    return targetPlatform == TargetPlatform.android ||
        targetPlatform == TargetPlatform.iOS;
  }

  static Future<bool> initialize() async {
    if (!supportsPlatform()) return false;

    if (defaultTargetPlatform == TargetPlatform.iOS) {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    } else {
      // Preserve the existing Android setup, which reads google-services.json.
      await Firebase.initializeApp();
    }
    return true;
  }
}
