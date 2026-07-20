import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../aiTriage/models/triage_entry_context.dart';

class PostpartumSafetyHelpScreen extends StatefulWidget {
  const PostpartumSafetyHelpScreen({
    super.key,
    this.onStartAssessment,
    this.emergencyCaller,
  });

  final ValueChanged<TriageEntryContext>? onStartAssessment;
  final Future<bool> Function()? emergencyCaller;

  @override
  State<PostpartumSafetyHelpScreen> createState() =>
      _PostpartumSafetyHelpScreenState();
}

class _PostpartumSafetyHelpScreenState
    extends State<PostpartumSafetyHelpScreen> {
  bool _callingEmergency = false;
  bool _manualCallRequired = false;

  Future<void> _callEmergency() async {
    if (_callingEmergency) return;
    setState(() {
      _callingEmergency = true;
      _manualCallRequired = false;
    });
    var opened = false;
    try {
      opened =
          await (widget.emergencyCaller?.call() ??
              launchUrl(
                Uri.parse('tel:115'),
                mode: LaunchMode.externalApplication,
              ));
    } catch (_) {
      opened = false;
    }
    if (!mounted) return;
    setState(() {
      _callingEmergency = false;
      _manualCallRequired = !opened;
    });
  }

  void _startAssessment(BuildContext context) {
    if (widget.onStartAssessment != null) {
      widget.onStartAssessment!(const TriageEntryContext.postpartum());
      return;
    }
    context.push(
      '/triage/intake',
      extra: const TriageEntryContext.postpartum(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF8F6),
        foregroundColor: const Color(0xFF845143),
        title: const Text('Hỗ trợ an toàn sau sinh'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Semantics(
            header: true,
            child: const Text(
              'Bạn không cần tự đánh giá một mình',
              style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800),
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Nếu triệu chứng nghiêm trọng, xuất hiện đột ngột, tăng nhanh hoặc khiến bạn cảm thấy không an toàn, hãy liên hệ cơ sở y tế ngay.',
            style: TextStyle(fontSize: 16, height: 1.5),
          ),
          const SizedBox(height: 24),
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFFFFDAD6),
              borderRadius: BorderRadius.circular(22),
            ),
            child: const Text(
              'CareBridge không thay thế chẩn đoán hoặc chăm sóc y tế khẩn cấp.',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            key: const Key('postpartum-start-symptom-assessment'),
            onPressed: () => _startAssessment(context),
            icon: const Icon(Icons.health_and_safety_outlined),
            label: const Text('Bắt đầu đánh giá dấu hiệu sau sinh'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              backgroundColor: const Color(0xFFC98C7B),
            ),
          ),
          const SizedBox(height: 12),
          if (_manualCallRequired) ...[
            Semantics(
              liveRegion: true,
              child: Container(
                key: const Key('postpartum-manual-call-guidance'),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF0ED),
                  borderRadius: BorderRadius.circular(18),
                  border: const Border(
                    left: BorderSide(color: Color(0xFFBA1A1A), width: 4),
                  ),
                ),
                child: const Text(
                  'Không thể mở ứng dụng gọi điện. Hãy tự gọi 115 ngay hoặc nhờ người bên cạnh gọi giúp.',
                  style: TextStyle(
                    color: Color(0xFF5A463F),
                    fontWeight: FontWeight.w700,
                    height: 1.4,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
          FilledButton.icon(
            key: const Key('postpartum-call-emergency'),
            onPressed: _callingEmergency ? null : _callEmergency,
            icon: _callingEmergency
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.call_rounded),
            label: const Text('Gọi cấp cứu 115'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              backgroundColor: const Color(0xFFBA1A1A),
            ),
          ),
        ],
      ),
    );
  }
}
