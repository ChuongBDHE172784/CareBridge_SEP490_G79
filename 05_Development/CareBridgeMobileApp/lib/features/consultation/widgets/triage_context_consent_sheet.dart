import 'package:flutter/material.dart';

import '../models/triage_expert_handoff.dart';

class TriageContextConsentSheet extends StatefulWidget {
  final TriageExpertHandoffPreview preview;
  final String expertDisplayName;

  const TriageContextConsentSheet({
    super.key,
    required this.preview,
    required this.expertDisplayName,
  });

  @override
  State<TriageContextConsentSheet> createState() =>
      _TriageContextConsentSheetState();
}

class _TriageContextConsentSheetState extends State<TriageContextConsentSheet> {
  static const _accent = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _secondaryText = Color(0xFF9C857C);
  static const _nestedSurface = Color(0xFFF2EAE4);
  bool _approved = false;

  @override
  Widget build(BuildContext context) {
    return Material(
      key: const Key('triage-handoff-consent-sheet'),
      color: Colors.white,
      borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
      clipBehavior: Clip.antiAlias,
      child: SafeArea(
        top: false,
        child: SingleChildScrollView(
          padding: EdgeInsets.fromLTRB(
            24,
            12,
            24,
            24 + MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: Semantics(
            container: true,
            namesRoute: true,
            label: 'Đồng ý chia sẻ ngữ cảnh YELLOW tối thiểu',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Center(
                  child: Container(
                    width: 48,
                    height: 6,
                    decoration: BoxDecoration(
                      color: const Color(0xFFE8DDD6),
                      borderRadius: BorderRadius.circular(99),
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                const Text(
                  'Bạn kiểm soát nội dung được chia sẻ',
                  style: TextStyle(
                    color: _text,
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'CareBridge chỉ gửi ngữ cảnh tối thiểu dưới đây cho ${widget.expertDisplayName}.',
                  style: const TextStyle(
                    color: _text,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    height: 1.45,
                  ),
                ),
                const SizedBox(height: 20),
                _DisclosureCard(
                  icon: Icons.check_circle_outline,
                  title: 'Sẽ chia sẻ',
                  items: widget.preview.sharedFields,
                ),
                const SizedBox(height: 12),
                _DisclosureCard(
                  icon: Icons.shield_outlined,
                  title: 'Không chia sẻ',
                  items: widget.preview.excludedFields,
                ),
                const SizedBox(height: 16),
                Semantics(
                  checked: _approved,
                  label: 'Xác nhận đồng ý chia sẻ đúng danh sách trên',
                  child: Material(
                    color: _nestedSurface,
                    borderRadius: BorderRadius.circular(24),
                    child: CheckboxListTile(
                      key: const Key('triage-handoff-consent-checkbox'),
                      value: _approved,
                      activeColor: _accent,
                      controlAffinity: ListTileControlAffinity.leading,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 4,
                      ),
                      title: const Text(
                        'Tôi đồng ý chia sẻ đúng danh sách trên với chuyên gia này.',
                        style: TextStyle(
                          color: _text,
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          height: 1.35,
                        ),
                      ),
                      onChanged: (value) =>
                          setState(() => _approved = value == true),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 52,
                  child: FilledButton(
                    key: const Key('triage-handoff-consent-submit'),
                    onPressed: _approved
                        ? () => Navigator.of(context).pop(true)
                        : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: _accent,
                      foregroundColor: Colors.white,
                      disabledBackgroundColor: const Color(0xFFE8DDD6),
                      disabledForegroundColor: _secondaryText,
                      shape: const StadiumBorder(),
                    ),
                    child: const Text(
                      'Đồng ý và gửi yêu cầu',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  height: 48,
                  child: TextButton(
                    key: const Key('triage-handoff-consent-cancel'),
                    onPressed: () => Navigator.of(context).pop(false),
                    style: TextButton.styleFrom(
                      foregroundColor: _text,
                      shape: const StadiumBorder(),
                    ),
                    child: const Text(
                      'Chưa chia sẻ lúc này',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _DisclosureCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final List<String> items;

  const _DisclosureCard({
    required this.icon,
    required this.title,
    required this.items,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE8DDD6)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: const Color(0xFFC98C7B)),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    color: Color(0xFF5A463F),
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          for (final item in items)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 7),
                    child: Icon(
                      Icons.circle,
                      size: 7,
                      color: Color(0xFFC98C7B),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      item,
                      style: const TextStyle(
                        color: Color(0xFF5A463F),
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
