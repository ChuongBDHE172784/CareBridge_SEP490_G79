part of '../trackasia_gl_web.dart';

class TrackAsiaMapPlugin {
  /// Registers this class as the default instance of [TrackAsiaPlatform].
  static void registerWith(Registrar registrar) {
    TrackAsiaPlatform.createInstance = () => TrackAsiaMapController();
  }
}
