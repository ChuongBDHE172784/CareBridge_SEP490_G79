import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../../core/network/api_client.dart';

class ExpertProfileSetupScreen extends StatefulWidget {
  const ExpertProfileSetupScreen({super.key});

  @override
  State<ExpertProfileSetupScreen> createState() =>
      _ExpertProfileSetupScreenState();
}

class _ExpertProfileSetupScreenState extends State<ExpertProfileSetupScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);

  static const _specialtyOptions = [
    'Nhi khoa',
    'Sản khoa',
    'Dinh dưỡng',
    'Tâm lý',
    'Giáo dục sớm',
  ];

  static const _modalityOptions = [
    ('Chat nhắn tin', 'CHAT'),
    ('Cuộc gọi thoại', 'VOICE'),
    ('Tư vấn Video', 'VIDEO'),
  ];

  final _formKey = GlobalKey<FormState>();
  final _displayNameCtrl = TextEditingController();
  final _yearsCtrl = TextEditingController();
  final _feeCtrl = TextEditingController();
  final _bioCtrl = TextEditingController();

  final Set<String> _selectedSpecialties = {};
  final Set<String> _selectedModalities = {};
  bool _submitting = false;
  String? _errorMsg;

  @override
  void dispose() {
    _displayNameCtrl.dispose();
    _yearsCtrl.dispose();
    _feeCtrl.dispose();
    _bioCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedSpecialties.isEmpty) {
      setState(() => _errorMsg = 'Vui lòng chọn ít nhất 1 chuyên khoa');
      return;
    }
    if (_selectedModalities.isEmpty) {
      setState(() => _errorMsg = 'Vui lòng chọn ít nhất 1 kênh hỗ trợ');
      return;
    }

    setState(() {
      _submitting = true;
      _errorMsg = null;
    });

    try {
      final body = <String, dynamic>{
        'displayName': _displayNameCtrl.text.trim(),
        'bio': _bioCtrl.text.trim().isEmpty ? null : _bioCtrl.text.trim(),
        'specialties': _selectedSpecialties.toList(),
        'yearsOfExperience': int.tryParse(_yearsCtrl.text.trim()) ?? 0,
        'consultationFeeVnd':
            int.tryParse(_feeCtrl.text.trim().replaceAll('.', '')) ?? 0,
        'consultationModalities': _selectedModalities.toList(),
      };

      await apiPost('/api/v1/expert-profiles', body);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã gửi hồ sơ duyệt thành công!'),
            backgroundColor: Color(0xFF845143),
          ),
        );
        await Future.delayed(const Duration(seconds: 1));
        if (mounted) Navigator.of(context).pop();
      }
    } catch (e) {
      setState(() => _errorMsg = 'Lỗi: $e');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary, size: 28),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Hồ sơ chuyên gia',
          style: TextStyle(
            color: _onSurface,
            fontSize: 20,
            fontWeight: FontWeight.w600,
            fontFamily: 'Lexend',
          ),
        ),
        centerTitle: true,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
          children: [
            const SizedBox(height: 8),
            // Header
            Center(
              child: Column(
                children: [
                  Text(
                    'Thiết lập hồ sơ',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w600,
                      color: _primary,
                      fontFamily: 'Lexend',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Hoàn thiện thông tin để bắt đầu hỗ trợ cộng đồng CareBridge.',
                    style: TextStyle(
                      fontSize: 14,
                      color: _onSurfaceVariant,
                      fontFamily: 'Lexend',
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            // Section 1: Basic Info
            _SectionCard(
              color: _primary,
              children: [
                // Avatar
                Center(
                  child: GestureDetector(
                    onTap: () {},
                    child: Container(
                      width: 96,
                      height: 96,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: _surfaceVariant,
                        border: Border.all(color: _surface, width: 4),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.06),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.add_a_photo,
                        size: 40,
                        color: Color(0xFF845143),
                      ),
                    ),
                  ),
                ),
                Center(
                  child: Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      'Tải ảnh đại diện',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: _primary,
                        fontFamily: 'Lexend',
                        letterSpacing: 0.05,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                _InputField(
                  label: 'Tên hiển thị công khai *',
                  hint: 'VD: BS. Nguyễn Văn A',
                  controller: _displayNameCtrl,
                  validator: (v) =>
                      (v == null || v.trim().isEmpty) ? 'Bắt buộc' : null,
                ),
                Row(
                  children: [
                    Expanded(
                      child: _InputField(
                        label: 'Năm kinh nghiệm *',
                        hint: '0',
                        controller: _yearsCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        validator: (v) {
                          if (v == null || v.trim().isEmpty) return 'Bắt buộc';
                          final n = int.tryParse(v);
                          if (n == null || n < 0) return '>= 0';
                          if (n > 50) return '<= 50';
                          return null;
                        },
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _InputField(
                        label: 'Phí tư vấn (VNĐ) *',
                        hint: 'VD: 300000',
                        controller: _feeCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                          LengthLimitingTextInputFormatter(10),
                        ],
                        validator: (v) {
                          if (v == null || v.trim().isEmpty) return 'Bắt buộc';
                          final n = int.tryParse(v);
                          if (n == null || n < 0) return '>= 0';
                          return null;
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 16),
            // Section 2: Specialties & Modalities
            _SectionCard(
              color: _primary,
              children: [
                Row(
                  children: [
                    Icon(Icons.medical_services, color: _primary, size: 20),
                    const SizedBox(width: 8),
                    Text(
                      'Chuyên môn & Dịch vụ',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                        fontFamily: 'Lexend',
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  'Chuyên khoa (Chọn tối đa 3)',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: _onSurfaceVariant,
                    fontFamily: 'Lexend',
                    letterSpacing: 0.05,
                  ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _specialtyOptions.map((s) {
                    final selected = _selectedSpecialties.contains(s);
                    return _Chip(
                      label: s,
                      selected: selected,
                      onTap: () {
                        setState(() {
                          if (selected) {
                            _selectedSpecialties.remove(s);
                          } else {
                            if (_selectedSpecialties.length >= 3) return;
                            _selectedSpecialties.add(s);
                          }
                        });
                      },
                    );
                  }).toList(),
                ),
                const SizedBox(height: 20),
                Text(
                  'Kênh hỗ trợ được cấp phép',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: _onSurfaceVariant,
                    fontFamily: 'Lexend',
                    letterSpacing: 0.05,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: _modalityOptions.map((m) {
                    final selected = _selectedModalities.contains(m.$2);
                    return Expanded(
                      child: _ModalityCard(
                        icon: _modalityIcon(m.$2),
                        label: m.$1,
                        selected: selected,
                        onTap: () {
                          setState(() {
                            if (selected) {
                              _selectedModalities.remove(m.$2);
                            } else {
                              _selectedModalities.add(m.$2);
                            }
                          });
                        },
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
            const SizedBox(height: 16),
            // Section 3: Bio
            _SectionCard(
              color: _primary,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.edit_note, color: _primary, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          'Giới thiệu bản thân',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ],
                    ),
                    Text(
                      '${_bioCtrl.text.length}/500',
                      style: TextStyle(
                        fontSize: 12,
                        color: _outline,
                        fontFamily: 'Lexend',
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _bioCtrl,
                  maxLength: 500,
                  maxLines: 4,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                    hintText:
                        'Viết một đoạn giới thiệu ngắn về kinh nghiệm, phương pháp tiếp cận và mong muốn hỗ trợ cộng đồng của bạn...',
                    hintStyle: TextStyle(color: _outline, fontFamily: 'Lexend'),
                    filled: true,
                    fillColor: _surfaceContainerLowest,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide(color: _outlineVariant),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide(color: _outlineVariant),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide(color: _primary, width: 1.5),
                    ),
                    contentPadding: const EdgeInsets.all(16),
                  ),
                  style: TextStyle(
                    fontSize: 14,
                    color: _onSurface,
                    fontFamily: 'Lexend',
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: _surfaceContainerLow,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: _surfaceVariant),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        Icons.lightbulb,
                        color: _primary,
                        size: 20,
                        fill: 1,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Mẹo: Một lời giới thiệu ấm áp, chân thành sẽ giúp cha mẹ cảm thấy tin tưởng và dễ kết nối hơn.',
                          style: TextStyle(
                            fontSize: 13,
                            color: _onSurfaceVariant,
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            // Submit buttons
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: _submitting ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primaryContainer,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(26),
                  ),
                  elevation: 4,
                ),
                child: _submitting
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.verified, color: Colors.white, size: 20),
                          const SizedBox(width: 8),
                          Text(
                            'Gửi hồ sơ duyệt',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              height: 52,
              child: OutlinedButton(
                onPressed: () {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Đã lưu bản nháp (mock)')),
                  );
                },
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF5A463F),
                  side: BorderSide.none,
                  backgroundColor: const Color(0xFFF2EAE4),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(26),
                  ),
                ),
                child: Text(
                  'Lưu bản nháp',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    fontFamily: 'Lexend',
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  static IconData _modalityIcon(String code) {
    switch (code) {
      case 'CHAT':
        return Icons.chat;
      case 'VOICE':
        return Icons.call;
      case 'VIDEO':
        return Icons.videocam;
      default:
        return Icons.help_outline;
    }
  }
}

class _SectionCard extends StatelessWidget {
  final Color color;
  final List<Widget> children;

  const _SectionCard({required this.color, required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            top: 0,
            left: 0,
            bottom: 0,
            width: 4,
            child: Container(
              decoration: BoxDecoration(
                color: color,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: children,
            ),
          ),
        ],
      ),
    );
  }
}

class _InputField extends StatelessWidget {
  final String label;
  final String hint;
  final TextEditingController? controller;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final String? Function(String?)? validator;

  const _InputField({
    required this.label,
    required this.hint,
    this.controller,
    this.keyboardType,
    this.inputFormatters,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: const Color(0xFF524440),
              fontFamily: 'Lexend',
              letterSpacing: 0.05,
            ),
          ),
          const SizedBox(height: 6),
          TextFormField(
            controller: controller,
            keyboardType: keyboardType,
            inputFormatters: inputFormatters,
            validator: validator,
            decoration: InputDecoration(
              hintText: hint,
              hintStyle: TextStyle(
                color: const Color(0xFF84736F),
                fontFamily: 'Lexend',
              ),
              filled: true,
              fillColor: const Color(0xFFFFF8F6),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide(color: const Color(0xFFD6C2BD)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide(color: const Color(0xFFD6C2BD)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide(color: const Color(0xFF845143), width: 1.5),
              ),
              errorBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: Color(0xFFBA1A1A)),
              ),
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            ),
            style: TextStyle(
              fontSize: 16,
              color: const Color(0xFF271812),
              fontFamily: 'Lexend',
            ),
          ),
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _Chip({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: selected
              ? const Color(0xFF845143).withValues(alpha: 0.1)
              : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(99),
          border: Border.all(
            color: selected ? const Color(0xFF845143) : const Color(0xFFD6C2BD),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (selected) ...[
              Icon(Icons.check, size: 18, color: const Color(0xFF845143)),
              const SizedBox(width: 4),
            ],
            Text(
              label,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: selected
                    ? const Color(0xFF845143)
                    : const Color(0xFF524440),
                fontFamily: 'Lexend',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ModalityCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _ModalityCard({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
        decoration: BoxDecoration(
          color: selected
              ? const Color(0xFF845143).withValues(alpha: 0.05)
              : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: selected ? const Color(0xFF845143) : const Color(0xFFD6C2BD),
            width: selected ? 2 : 1,
          ),
        ),
        child: Column(
          children: [
            Icon(
              icon,
              size: 28,
              color: selected
                  ? const Color(0xFF845143)
                  : const Color(0xFF524440),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: selected
                    ? const Color(0xFF845143)
                    : const Color(0xFF524440),
                fontFamily: 'Lexend',
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
