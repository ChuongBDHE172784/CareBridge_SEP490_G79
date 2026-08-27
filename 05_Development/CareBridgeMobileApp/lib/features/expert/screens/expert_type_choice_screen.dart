import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/expert_onboarding_model.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

/// Bước 2 onboarding — chuyên gia chọn hình thức hợp tác.
///
/// Mỗi thẻ nói thẳng NGHĨA VỤ đi kèm chứ không chỉ mô tả quyền lợi: viết nghĩa vụ ngay trên
/// thẻ chọn lọc bớt người chọn nhóm hợp tác chỉ vì huy hiệu đẹp hơn, và giảm số ca admin
/// phải xếp xuống nhóm cộng đồng.
class ExpertTypeChoiceScreen extends StatefulWidget {
  const ExpertTypeChoiceScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertTypeChoiceScreen> createState() => _ExpertTypeChoiceScreenState();
}

class _ExpertTypeChoiceScreenState extends State<ExpertTypeChoiceScreen> {
  static const _background = Color(0xFFF7F1ED);
  static const _accent = Color(0xFFC98C7B);
  static const _ink = Color(0xFF3B2C27);
  static const _muted = Color(0xFF7B6A63);
  static const _contractedTint = Color(0xFF10B981);
  static const _communityTint = Color(0xFF0EA5E9);

  ExpertKind? _selected;
  bool _saving = false;
  String? _error;

  ExpertOnboardingService get _service =>
      widget.service ?? ExpertOnboardingService.instance;

  Future<void> _submit() async {
    final choice = _selected;
    if (choice == null || _saving) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.chooseExpertType(choice);
      final state = await _service.loadState();
      ExpertOnboardingStore.instance.update(state);
      if (!mounted) return;
      context.go('/expert-onboarding');
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error = 'Không lưu được lựa chọn. Vui lòng kiểm tra kết nối và thử lại.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        elevation: 0,
        foregroundColor: _ink,
        title: const Text(
          'Hình thức hợp tác',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
            fontSize: 18,
          ),
        ),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
          children: [
            const Text(
              'Bạn muốn tham gia CareBridge với hình thức nào?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _ink,
                height: 1.35,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Cả hai hình thức đều phải qua cùng một quy trình xác minh danh tính và '
              'chứng chỉ hành nghề. Bạn có thể đổi lựa chọn trước khi hồ sơ được duyệt.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13.5,
                color: _muted,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 22),
            _ChoiceCard(
              icon: Icons.verified_rounded,
              tint: _contractedTint,
              title: 'Chuyên gia Hệ thống',
              benefit:
                  'Được ưu tiên giới thiệu tới người dùng cần tư vấn, và hiển thị huy hiệu '
                  'Chuyên gia Hệ thống trên hồ sơ công khai.',
              obligation:
                  'Yêu cầu: ký Thoả thuận hợp tác và duy trì tối thiểu 10 ca rảnh mỗi tuần.',
              selected: _selected == ExpertKind.pendingContract,
              onTap: _saving
                  ? null
                  : () => setState(() => _selected = ExpertKind.pendingContract),
            ),
            const SizedBox(height: 14),
            _ChoiceCard(
              icon: Icons.volunteer_activism_outlined,
              tint: _communityTint,
              title: 'Chuyên gia Y tế Cộng đồng',
              benefit:
                  'Tư vấn hỗ trợ cộng đồng theo khả năng của bạn, hiển thị chứng chỉ hành nghề '
                  'đã được kiểm duyệt.',
              obligation:
                  'Không cần cam kết lịch cố định, phản hồi khi bạn rảnh.',
              selected: _selected == ExpertKind.community,
              onTap: _saving
                  ? null
                  : () => setState(() => _selected = ExpertKind.community),
            ),
            if (_error != null) ...[
              const SizedBox(height: 16),
              Text(
                _error!,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: Color(0xFFB4342A),
                ),
              ),
            ],
            const SizedBox(height: 26),
            SizedBox(
              height: 52,
              child: FilledButton(
                key: const Key('expert-type-continue'),
                onPressed: _selected == null || _saving ? null : _submit,
                style: FilledButton.styleFrom(
                  backgroundColor: _accent,
                  disabledBackgroundColor: _accent.withValues(alpha: 0.35),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: _saving
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.4,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        'Tiếp tục',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChoiceCard extends StatelessWidget {
  const _ChoiceCard({
    required this.icon,
    required this.tint,
    required this.title,
    required this.benefit,
    required this.obligation,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final Color tint;
  final String title;
  final String benefit;
  final String obligation;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        key: Key('expert-type-card-$title'),
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(
              color: selected ? tint : const Color(0xFFE6DAD3),
              width: selected ? 2 : 1,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(9),
                    decoration: BoxDecoration(
                      color: tint.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(11),
                    ),
                    child: Icon(icon, size: 22, color: tint),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16.5,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF3B2C27),
                      ),
                    ),
                  ),
                  Icon(
                    selected
                        ? Icons.radio_button_checked
                        : Icons.radio_button_unchecked,
                    color: selected ? tint : const Color(0xFFC5B5AD),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                benefit,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13.5,
                  color: Color(0xFF5C4A43),
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                decoration: BoxDecoration(
                  color: const Color(0xFFF7F1ED),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  obligation,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF7B6A63),
                    height: 1.45,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
