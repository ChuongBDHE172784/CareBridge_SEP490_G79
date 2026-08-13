/// Display mapping for notification `type` strings (CB-EPDS-IMP-001).
///
/// Extracted from the notification screens so the mapping is unit-testable without
/// pumping a full screen (which needs auth/providers). Behaviour is unchanged —
/// each screen delegates to these functions.
library;

import 'package:flutter/material.dart';

/// Human-readable label shown on the notifications list.
String notificationTypeLabel(String type) {
  switch (type.toUpperCase()) {
    case 'LOCATION_SHARE':
      return 'Vị trí của Mother';
    case 'HEALTH':
    case 'HEALTH_ALERT':
      return 'Cảnh báo sức khỏe';
    case 'GROUP_INVITE':
      return 'Lời mời vào nhóm';
    case 'EPDS_RESULT':
      return 'Kết quả sàng lọc EPDS';
    case 'APPOINTMENT':
    case 'REMINDER':
      return 'Nhắc lịch';
    case 'MESSAGE':
    case 'CHAT':
      return 'Tin nhắn mới';
    case 'CONSULTATION':
      return 'Yêu cầu tư vấn';
    default:
      return 'Thông báo';
  }
}

/// Icon used by the notification centre for a given type.
///
/// Returns `null` for unmapped types so the caller keeps its own default.
IconData? notificationCenterIcon(String type) {
  switch (type.toUpperCase()) {
    case 'EPDS_RESULT':
      return Icons.psychology_outlined;
    default:
      return null;
  }
}
