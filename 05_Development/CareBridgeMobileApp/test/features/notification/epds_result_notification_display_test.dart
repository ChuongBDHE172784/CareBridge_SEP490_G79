import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/notification/notification_type_display.dart';
import 'package:untitled/features/notification/models/notification_model.dart';

/// CB-EPDS-TEST-001 — TC-20, TC-21.
///
/// Verifies the EPDS_RESULT notification type is rendered with its own label and
/// icon rather than falling through to the generic default, which would make the
/// feature effectively invisible in the app despite a green backend suite.
void main() {
  group('TC-20 — notifications list label', () {
    test('EPDS_RESULT gets a dedicated label, not the generic default', () {
      expect(notificationTypeLabel('EPDS_RESULT'), 'Kết quả sàng lọc EPDS');
      expect(notificationTypeLabel('EPDS_RESULT'),
          isNot(notificationTypeLabel('SOMETHING_UNKNOWN')));
    });

    test('label lookup is case-insensitive', () {
      expect(notificationTypeLabel('epds_result'), 'Kết quả sàng lọc EPDS');
    });

    test('unknown types still fall back to the generic label', () {
      expect(notificationTypeLabel('SOMETHING_UNKNOWN'), 'Thông báo');
    });

    test('existing type labels are unchanged by the extraction', () {
      expect(notificationTypeLabel('GROUP_INVITE'), 'Lời mời vào nhóm');
      expect(notificationTypeLabel('LOCATION_SHARE'), 'Vị trí của Mother');
      expect(notificationTypeLabel('REMINDER'), 'Nhắc lịch');
      expect(notificationTypeLabel('MESSAGE'), 'Tin nhắn mới');
      expect(notificationTypeLabel('CONSULTATION'), 'Yêu cầu tư vấn');
      expect(notificationTypeLabel('HEALTH_ALERT'), 'Cảnh báo sức khỏe');
    });
  });

  group('TC-21 — notification centre icon', () {
    test('EPDS_RESULT maps to its own icon', () {
      expect(notificationCenterIcon('EPDS_RESULT'), Icons.psychology_outlined);
    });

    test('unmapped types return null so the caller keeps its default', () {
      expect(notificationCenterIcon('SOMETHING_UNKNOWN'), isNull);
    });
  });

  group('model parsing', () {
    test('an EPDS_RESULT record parses with care-group reference', () {
      final record = NotificationRecord.fromJson({
        'id': 'notification-epds-1',
        'userId': 'family-1',
        'type': 'EPDS_RESULT',
        'title': 'Kết quả sàng lọc EPDS',
        'body': 'Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS. Điểm 8/30 — Nguy cơ hiện tại thấp. '
            'Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.',
        'referenceId': 'care-group-1',
        'referenceType': 'CARE_GROUP',
        'status': 'SENT',
        'createdAt': '2026-08-14T09:00:00Z',
      });

      expect(record.type, 'EPDS_RESULT');
      expect(record.referenceType, 'CARE_GROUP');
      expect(record.referenceId, 'care-group-1');
      expect(notificationTypeLabel(record.type), 'Kết quả sàng lọc EPDS');
    });

    test('family-facing escalation body discloses no self-harm wording', () {
      // Mirrors the backend escalation string (EpdsSeverityPolicy) — the mobile
      // client must never receive or render Question-10 detail (BR-SAFETY-EPDS-001).
      const escalationBody =
          'Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS. '
          'Kết quả cần được quan tâm ngay — hãy liên hệ và ở bên mẹ. '
          'Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.';

      final lower = escalationBody.toLowerCase();
      for (final forbidden in ['tự hại', 'tự sát', 'câu 10', 'self-harm', 'suicid']) {
        expect(lower.contains(forbidden), isFalse,
            reason: 'escalation body must not contain "$forbidden"');
      }
    });
  });
}
