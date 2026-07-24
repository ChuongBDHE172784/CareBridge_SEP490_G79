import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/triage_entry_context.dart';

class TriageSafetyEntryAction extends StatelessWidget {
  const TriageSafetyEntryAction({
    super.key,
    required this.entryContext,
    this.onStartAssessment,
  });

  final TriageEntryContext entryContext;
  final ValueChanged<TriageEntryContext>? onStartAssessment;

  bool get _isMaternal => const {
    TriageStageIntent.preconception,
    TriageStageIntent.pregnancy,
    TriageStageIntent.postpartum,
  }.contains(entryContext.stage);

  void _openAssessment(BuildContext context) {
    final callback = onStartAssessment;
    if (callback != null) {
      callback(entryContext);
      return;
    }
    context.push('/triage/intake', extra: entryContext);
  }

  @override
  Widget build(BuildContext context) {
    final title = _isMaternal
        ? 'Kiểm tra dấu hiệu an toàn của bạn'
        : 'Kiểm tra dấu hiệu an toàn của bé';
    final description = _isMaternal
        ? 'Đánh giá nhanh theo đúng giai đoạn sức khỏe hiện tại.'
        : 'Đánh giá nhanh theo đúng độ tuổi hiện tại của bé.';

    return Semantics(
      key: Key(
        'triage-safety-entry-${entryContext.stage.apiValue.toLowerCase()}',
      ),
      button: true,
      label: '$title. $description',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _openAssessment(context),
          borderRadius: BorderRadius.circular(32),
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 64),
            child: Ink(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              decoration: BoxDecoration(
                color: const Color(0xFFC98C7B),
                borderRadius: BorderRadius.circular(32),
                border: Border.all(color: const Color(0x00FFFFFF), width: 2),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x1F5A463F),
                    blurRadius: 24,
                    offset: Offset(0, 8),
                  ),
                  BoxShadow(
                    color: Color(0x29FFFFFF),
                    blurRadius: 8,
                    offset: Offset(-2, -2),
                  ),
                ],
              ),
              child: Row(
                children: [
                  const SizedBox(
                    width: 40,
                    height: 40,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: Color(0x29FFFFFF),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.health_and_safety_outlined,
                        color: Colors.white,
                        size: 22,
                      ),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          description,
                          style: const TextStyle(
                            color: Color(0xFFFFF8F6),
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            height: 1.3,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.arrow_forward_rounded,
                    color: Colors.white,
                    size: 22,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
