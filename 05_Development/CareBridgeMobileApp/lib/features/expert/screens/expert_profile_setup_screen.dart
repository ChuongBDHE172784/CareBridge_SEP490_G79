import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class ExpertProfileSetupScreen extends StatefulWidget {
  const ExpertProfileSetupScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertProfileSetupScreen> createState() => _ExpertProfileSetupScreenState();
}

class _ExpertProfileSetupScreenState extends State<ExpertProfileSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _specialty = TextEditingController();
  final _title = TextEditingController();
  final _experience = TextEditingController();
  final _workplace = TextEditingController();
  final _scope = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _specialty.dispose();
    _title.dispose();
    _experience.dispose();
    _workplace.dispose();
    _scope.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await (widget.service ?? ExpertOnboardingService.instance).createProfile(
        specialty: _specialty.text.trim(),
        professionalTitle: _title.text.trim(),
        experienceYears: int.parse(_experience.text.trim()),
        workplace: _workplace.text.trim(),
        consultationScope: _scope.text.trim(),
      );
      ExpertOnboardingStore.instance.invalidate();
      if (mounted) context.go('/expert/identity');
    } on ApiException catch (e) {
      setState(() => _error = e.statusCode == 409
          ? 'Hồ sơ đã tồn tại. Đang chuyển sang bước tiếp theo…'
          : 'Không thể lưu hồ sơ. Vui lòng kiểm tra thông tin.');
      if (e.statusCode == 409 && mounted) context.go('/expert-onboarding');
    } catch (_) {
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _required(String? value) => (value ?? '').trim().isEmpty ? 'Thông tin này là bắt buộc' : null;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(title: const Text('Hồ sơ chuyên gia'), backgroundColor: Colors.transparent),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              const Text('Bước 1/3', style: TextStyle(color: Color(0xFFC06F5A), fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              const Text('Giới thiệu chuyên môn của bạn', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: Color(0xFF5A463F))),
              const SizedBox(height: 8),
              const Text('Thông tin này giúp hội đồng xác minh và hiển thị hồ sơ sau khi được duyệt.', style: TextStyle(height: 1.5, color: Color(0xFF75635C))),
              const SizedBox(height: 24),
              _field(_specialty, 'Chuyên khoa', 'Ví dụ: Sản khoa', validator: _required),
              _field(_title, 'Chức danh chuyên môn', 'Ví dụ: Bác sĩ chuyên khoa I', validator: _required),
              _field(
                _experience,
                'Số năm kinh nghiệm',
                'Ví dụ: 8',
                keyboardType: TextInputType.number,
                validator: (value) {
                  final years = int.tryParse((value ?? '').trim());
                  return years == null || years < 0 || years > 80 ? 'Nhập số năm từ 0 đến 80' : null;
                },
              ),
              _field(_workplace, 'Nơi công tác', 'Bệnh viện hoặc cơ sở y tế', validator: _required),
              _field(_scope, 'Phạm vi tư vấn', 'Mô tả chủ đề bạn có thể hỗ trợ', maxLines: 4, validator: _required),
              if (_error != null) ...[
                Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(color: const Color(0xFFFFDAD6), borderRadius: BorderRadius.circular(14)),
                  child: Text(_error!, style: const TextStyle(color: Color(0xFF93000A))),
                ),
              ],
              SizedBox(
                height: 54,
                child: FilledButton(
                  onPressed: _loading ? null : _submit,
                  style: FilledButton.styleFrom(backgroundColor: const Color(0xFFC98C7B), shape: const StadiumBorder()),
                  child: _loading
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text('Lưu và tiếp tục', style: TextStyle(fontWeight: FontWeight.w700)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController controller,
    String label,
    String hint, {
    TextInputType? keyboardType,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) => Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          maxLines: maxLines,
          validator: validator,
          decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
          ),
        ),
      );
}
