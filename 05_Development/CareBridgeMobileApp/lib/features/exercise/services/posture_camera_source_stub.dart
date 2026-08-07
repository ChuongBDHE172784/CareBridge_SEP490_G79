import 'dart:async';

import 'package:flutter/material.dart';

/// Native fallback for this concept demo.
///
/// Keeping the source available on every target lets the session screen share
/// one transport path without importing web-only libraries into Android/iOS.
class PostureCameraSource {
  Stream<Map<String, dynamic>> get frames => const Stream.empty();

  Stream<String> get errors => const Stream.empty();

  bool get isSupported => false;

  bool get isRunning => false;

  String? get lastError => null;

  bool get hasFeedbackError => false;

  /// Native targets currently have no local skeleton renderer.  Keep this
  /// method as a no-op so the session screen can share its feedback plumbing
  /// with Flutter Web without importing web-only libraries.
  void setFeedbackError(bool value) {}

  void setFeedbackWarning(bool value) => setFeedbackError(value);

  Future<void> start() async {}

  Future<void> stop() async {}

  Future<void> dispose() => stop();

  Widget buildPreview() {
    return const ColoredBox(
      color: Colors.black,
      child: Center(
        child: Icon(
          Icons.videocam_off_outlined,
          color: Colors.white54,
          size: 48,
        ),
      ),
    );
  }
}

PostureCameraSource createPostureCameraSource() => PostureCameraSource();
