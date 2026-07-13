import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../services/care_group_service.dart';

Future<bool?> showInviteMemberSheet(
  BuildContext context, {
  required String groupId,
}) {
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => _InviteMemberSheet(groupId: groupId),
  );
}

class _InviteMemberSheet extends StatefulWidget {
  final String groupId;
  const _InviteMemberSheet({required this.groupId});

  @override
  State<_InviteMemberSheet> createState() => _InviteMemberSheetState();
}

class _InviteMemberSheetState extends State<_InviteMemberSheet> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorColor = Color(0xFFBA1A1A);

  final _service = CareGroupService();
  final _emailCtrl = TextEditingController();
  String _role = 'MEMBER';
  bool _submitting = false;
  String? _errorText;

  @override
  void dispose() {
    _emailCtrl.dispose();
    super.dispose();
  }

  bool get _emailValid {
    final v = _emailCtrl.text.trim();
    return v.contains('@') &&
        v.contains('.') &&
        !v.contains(' ') &&
        v.length > 4;
  }

  Future<void> _submit() async {
    if (!_emailValid || _submitting) return;
    setState(() {
      _submitting = true;
      _errorText = null;
    });
    try {
      await _service.inviteMember(
        widget.groupId,
        _emailCtrl.text.trim(),
        memberRole: _role,
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      String message;
      switch (e.statusCode) {
        case 404:
          message = 'Không tìm thấy tài khoản CareBridge với email này.';
          break;
        case 409:
          message = 'Người này đã là thành viên hoặc đã được mời.';
          break;
        case 403:
          message = 'Chỉ quản trị nhóm mới có thể mời thành viên.';
          break;
        default:
          message = 'Không thể gửi lời mời. Vui lòng thử lại.';
      }
      if (mounted) {
        setState(() {
          _errorText = message;
          _submitting = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _errorText = 'Không thể kết nối. Vui lòng thử lại.';
          _submitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: SafeArea(
        child: Container(
          decoration: const BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
          ),
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 48,
                  height: 6,
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFD6C2BD),
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
              ),
              const Text(
                'Mời thành viên',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 4),
              const Text(
                'Nhập email tài khoản CareBridge của người bạn muốn mời.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _emailCtrl,
                keyboardType: TextInputType.emailAddress,
                onChanged: (_) => setState(() {}),
                style: const TextStyle(fontFamily: 'Lexend'),
                decoration: InputDecoration(
                  labelText: 'Email',
                  hintText: 'ten@vidu.com',
                  filled: true,
                  fillColor: _surfaceContainerLow,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'VAI TRO',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _onSurfaceVariant,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: _RoleChip(
                      label: 'Thành viên',
                      selected: _role == 'MEMBER',
                      onTap: () => setState(() => _role = 'MEMBER'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _RoleChip(
                      label: 'Người xem',
                      selected: _role == 'VIEWER',
                      onTap: () => setState(() => _role = 'VIEWER'),
                    ),
                  ),
                ],
              ),
              if (_errorText != null) ...[
                const SizedBox(height: 12),
                Text(
                  _errorText!,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _errorColor,
                  ),
                ),
              ],
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: (_emailValid && !_submitting) ? _submit : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _primaryContainer,
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                  ),
                  child: _submitting
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : const Text(
                          'Gửi lời mời',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: TextButton(
                  onPressed: _submitting
                      ? null
                      : () => Navigator.of(context).pop(false),
                  style: TextButton.styleFrom(foregroundColor: _primary),
                  child: const Text(
                    'Hủy',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w600,
                    ),
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

class _RoleChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _RoleChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(99),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? const Color(0x26C98C7B) : const Color(0xFFFFF1EC),
          borderRadius: BorderRadius.circular(99),
          border: Border.all(
            color: selected ? const Color(0xFFC98C7B) : Colors.transparent,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: selected ? const Color(0xFF845143) : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}
