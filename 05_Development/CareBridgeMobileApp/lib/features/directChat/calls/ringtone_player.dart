import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

// Selects the web implementation on js_interop-capable targets. Previously keyed on
// `dart.library.html`, which is tied to the deprecated dart:html the web player no
// longer uses.
import 'ringtone_player_stub.dart'
    if (dart.library.js_interop) 'ringtone_player_web.dart';

class RingtonePlayer {
  static final RingtonePlayer instance = RingtonePlayer._();
  RingtonePlayer._();

  String? _mode;
  Timer? _nativeTimer;

  void start(String mode) {
    if (_mode == mode) return;
    stop();
    _mode = mode;

    if (kIsWeb) {
      startRingtoneWeb(mode);
    } else {
      _startNative(mode);
    }
  }

  void _startNative(String mode) {
    _playNativeClick();
    _nativeTimer = Timer.periodic(
      Duration(milliseconds: mode == 'outgoing' ? 3000 : 2500),
      (_) => _playNativeClick(),
    );
  }

  void _playNativeClick() {
    try {
      SystemSound.play(SystemSoundType.click);
    } catch (_) {}
  }

  void stop() {
    _mode = null;
    _nativeTimer?.cancel();
    _nativeTimer = null;
    if (kIsWeb) {
      stopRingtoneWeb();
    }
  }
}
