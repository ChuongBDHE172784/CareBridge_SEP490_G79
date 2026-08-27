import 'package:flutter/material.dart';

const callRecordingConsentTitle = 'Xác nhận ghi âm/ghi hình';
const callRecordingConsentMessage =
    'Cuộc gọi này sẽ được ghi âm/ghi hình nhằm đảm bảo chất lượng tư vấn y tế (Tuân thủ PDPA)';
const callRecordingConsentNote =
    'Cuộc gọi chỉ được bắt đầu hoặc chấp nhận sau khi bạn đồng ý.';

Future<bool> showCallRecordingConsentDialog(BuildContext context) async {
  final accepted = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) => AlertDialog(
      icon: const CircleAvatar(
        backgroundColor: Color(0xFFFEE2E2),
        child: Text(
          'REC',
          style: TextStyle(
            color: Color(0xFFB91C1C),
            fontSize: 12,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      title: const Text(callRecordingConsentTitle),
      content: const Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(callRecordingConsentMessage, textAlign: TextAlign.center),
          SizedBox(height: 12),
          Text(
            callRecordingConsentNote,
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(dialogContext, false),
          child: const Text('Không đồng ý'),
        ),
        FilledButton(
          autofocus: true,
          onPressed: () => Navigator.pop(dialogContext, true),
          child: const Text('Đồng ý'),
        ),
      ],
    ),
  );
  return accepted ?? false;
}
