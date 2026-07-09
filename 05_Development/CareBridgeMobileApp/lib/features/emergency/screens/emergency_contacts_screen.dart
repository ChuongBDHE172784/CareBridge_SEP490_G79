import 'package:flutter/material.dart';

import '../models/emergency_contact_model.dart';
import '../services/emergency_contact_service.dart';
import 'edit_emergency_contact_screen.dart';

class EmergencyContactsScreen extends StatefulWidget {
  const EmergencyContactsScreen({super.key});

  @override
  State<EmergencyContactsScreen> createState() =>
      _EmergencyContactsScreenState();
}

class _EmergencyContactsScreenState extends State<EmergencyContactsScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = EmergencyContactService();
  EmergencyContact? _contact;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final contact = await _service.getContact();
      if (mounted) setState(() => _contact = contact);
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể tải liên hệ khẩn cấp');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openEditor() async {
    final saved = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => EditEmergencyContactScreen(contact: _contact),
      ),
    );
    if (saved == true) await _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: RefreshIndicator(
                color: _primaryContainer,
                onRefresh: _load,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
                  children: [
                    const Text(
                      'Thêm các liên hệ tin cậy để CareBridge có thể hỗ trợ bạn nhanh nhất trong các trường hợp cần thiết.',
                      style: TextStyle(
                        fontSize: 20,
                        height: 1.5,
                        color: _onSurfaceVariant,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 28),
                    if (_loading)
                      const Padding(
                        padding: EdgeInsets.only(top: 80),
                        child: Center(
                          child: CircularProgressIndicator(
                            color: _primaryContainer,
                          ),
                        ),
                      )
                    else if (_error != null)
                      _EmptyContactState(message: _error!)
                    else if (_contact == null)
                      const _EmptyContactState(
                        message:
                            'Chưa có liên hệ nào. Vui lòng thêm ít nhất một liên hệ để đảm bảo an toàn trong trường hợp khẩn cấp.',
                      )
                    else
                      _ContactCard(contact: _contact!, onEdit: _openEditor),
                  ],
                ),
              ),
            ),
            _buildBottomAction(),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back, color: _onSurface),
              onPressed: () => Navigator.of(context).pop(),
            ),
            const Expanded(
              child: Text(
                'Liên hệ khẩn cấp',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                ),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.notifications_outlined, color: _primary),
              onPressed: () {},
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomAction() {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      decoration: const BoxDecoration(
        color: _surface,
        border: Border(top: BorderSide(color: _surfaceContainerHighest)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: double.infinity,
            height: 56,
            child: ElevatedButton.icon(
              onPressed: _openEditor,
              style: ElevatedButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                elevation: 6,
              ),
              icon: const Icon(Icons.add),
              label: Text(
                _contact == null ? 'Thêm liên hệ' : 'Chỉnh sửa liên hệ',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'CareBridge cam kết bảo mật mọi thông tin liên hệ của bạn.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: _onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _ContactCard extends StatelessWidget {
  const _ContactCard({required this.contact, required this.onEdit});

  final EmergencyContact contact;
  final VoidCallback onEdit;

  static const _primary = Color(0xFF845143);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(32),
        border: const Border(left: BorderSide(color: _primary, width: 4)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: _primary,
                child: const Text(
                  '1',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      contact.name,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    Text(
                      contact.relationship?.isNotEmpty == true
                          ? contact.relationship!
                          : 'Liên hệ khẩn cấp',
                      style: const TextStyle(
                        fontSize: 16,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'ĐÃ LƯU',
                  style: TextStyle(
                    fontSize: 11,
                    color: _primary,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.8,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              const Icon(
                Icons.call_outlined,
                color: _onSurfaceVariant,
                size: 20,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  _maskedPhone(contact.phone),
                  style: const TextStyle(
                    fontSize: 16,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
              TextButton.icon(
                onPressed: onEdit,
                icon: const Icon(Icons.edit, size: 20),
                label: const Text('Sửa'),
                style: TextButton.styleFrom(foregroundColor: _primary),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _maskedPhone(String phone) {
    final digits = phone.replaceAll(RegExp(r'\s+'), '');
    if (digits.length < 6) return phone;
    return '${digits.substring(0, 3)} •••• ${digits.substring(digits.length - 3)}';
  }
}

class _EmptyContactState extends StatelessWidget {
  const _EmptyContactState({required this.message});

  final String message;

  static const _primary = Color(0xFF845143);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 64),
      child: Column(
        children: [
          Container(
            width: 112,
            height: 112,
            decoration: const BoxDecoration(
              color: _secondaryContainer,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.contact_emergency_outlined,
              color: _primary,
              size: 48,
            ),
          ),
          const SizedBox(height: 20),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
