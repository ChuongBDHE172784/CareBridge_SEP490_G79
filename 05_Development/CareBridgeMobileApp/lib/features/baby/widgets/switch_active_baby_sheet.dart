import 'package:flutter/material.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';

class SwitchActiveBabySheet extends StatefulWidget {
  final List<BabyProfile> profiles;
  final VoidCallback onActiveBabyChanged;

  const SwitchActiveBabySheet({
    super.key,
    required this.profiles,
    required this.onActiveBabyChanged,
  });

  @override
  State<SwitchActiveBabySheet> createState() => _SwitchActiveBabySheetState();
}

class _SwitchActiveBabySheetState extends State<SwitchActiveBabySheet> {
  static const _primary = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _border = Color(0xFFE8DDD6);

  final _service = BabyService();
  bool _isLoading = false;

  Future<void> _switchActiveBaby(String babyId) async {
    setState(() => _isLoading = true);
    try {
      await _service.switchActiveBabyProfile(babyId);
      if (!mounted) return;
      widget.onActiveBabyChanged();
      Navigator.pop(context, 'changed');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể chuyển hồ sơ bé.')),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
        ),
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 48,
              height: 6,
              margin: const EdgeInsets.only(bottom: 22),
              decoration: BoxDecoration(
                color: _border,
                borderRadius: BorderRadius.circular(3),
              ),
            ),
            const Text(
              'Chọn hồ sơ bé đang chăm sóc',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _text,
                fontFamily: 'Quicksand',
              ),
            ),
            const SizedBox(height: 20),
            if (_isLoading)
              const Padding(
                padding: EdgeInsets.all(32),
                child: CircularProgressIndicator(color: _primary),
              )
            else ...[
              for (final profile in widget.profiles)
                _ProfileItem(
                  profile: profile,
                  onTap: profile.isActive
                      ? null
                      : () {
                          _switchActiveBaby(profile.id);
                        },
                ),
              _SheetAction(
                icon: Icons.add_rounded,
                label: 'Thêm hồ sơ bé mới',
                onTap: () => Navigator.pop(context, 'add'),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: () => Navigator.pop(context, 'manage'),
                icon: const Icon(Icons.manage_accounts_rounded),
                label: const Text(
                  'Quản lý hồ sơ',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                ),
                style: OutlinedButton.styleFrom(
                  foregroundColor: _primary,
                  side: const BorderSide(color: _primary, width: 2),
                  minimumSize: const Size(double.infinity, 54),
                  shape: const StadiumBorder(),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _ProfileItem extends StatelessWidget {
  const _ProfileItem({required this.profile, required this.onTap});

  final BabyProfile profile;
  final VoidCallback? onTap;

  static const _primary = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    final isActive = profile.isActive;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isActive ? _surfaceLow : Colors.white,
          border: Border.all(
            color: isActive ? _primary : _border,
            width: 2,
          ),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: _text.withAlpha(10),
              blurRadius: 18,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: const Color(0xFFFFE9E3),
                shape: BoxShape.circle,
                border: Border.all(
                  color: isActive ? _primary : Colors.transparent,
                  width: 2,
                ),
              ),
              child: const Icon(Icons.child_care_rounded, color: _primary),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    profile.nickname,
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: _text,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    profile.ageLabel,
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _muted,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    isActive
                        ? 'Đang dùng cho dashboard, nhật ký và nhắc nhở'
                        : 'Chạm để chuyển sang bé này',
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 12,
                      color: _muted,
                    ),
                  ),
                ],
              ),
            ),
            if (isActive)
              const Icon(Icons.check_circle_rounded, color: _primary),
          ],
        ),
      ),
    );
  }
}

class _SheetAction extends StatelessWidget {
  const _SheetAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  static const _primary = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        width: double.infinity,
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: _border, width: 2),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: const BoxDecoration(
                color: Color(0xFFF2EAE4),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: _primary),
            ),
            const SizedBox(width: 14),
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Quicksand',
                fontSize: 16,
                fontWeight: FontWeight.w800,
                color: _text,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
