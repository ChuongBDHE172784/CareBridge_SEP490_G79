import 'package:flutter/material.dart';

import 'family_alert_detail_screen.dart';

/// Backward-compatible route target for emergency FCM notifications.
/// Both emergency routes now render the same real-data family alert screen.
class EmergencyAlertDetailScreen extends StatelessWidget {
  final String sessionId;

  const EmergencyAlertDetailScreen({super.key, required this.sessionId});

  @override
  Widget build(BuildContext context) {
    return FamilyAlertDetailScreen(sessionId: sessionId);
  }
}
