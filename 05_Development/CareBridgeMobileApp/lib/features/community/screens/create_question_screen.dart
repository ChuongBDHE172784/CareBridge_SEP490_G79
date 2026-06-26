import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class CreateQuestionScreen extends StatefulWidget {
  const CreateQuestionScreen({super.key});

  @override
  State<CreateQuestionScreen> createState() => _CreateQuestionScreenState();
}

class _CreateQuestionScreenState extends State<CreateQuestionScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _accent = Color(0xFFD4A895);

  final _formKey = GlobalKey<FormState>();
  final _titleCtrl = TextEditingController();
  final _bodyCtrl = TextEditingController();

  List<dynamic> _topics = [];
  String? _selectedTopicId;
  String _stage = 'PREGNANCY';
  String _urgency = 'NORMAL';
  bool _isAnonymous = false;
  bool _loading = false;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _loadTopics();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _bodyCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadTopics() async {
    setState(() => _loading = true);
    try {
      final data = await apiGet('/api/v1/community/topics');
      setState(() {
        _topics = (data['content'] ?? data) as List;
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      await apiPost('/api/v1/community/questions', {
        'title': _titleCtrl.text.trim(),
        'body': _bodyCtrl.text.trim(),
        if (_selectedTopicId != null) 'topicId': _selectedTopicId,
        'stage': _stage,
        'urgency': _urgency,
        'isAnonymous': _isAnonymous,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đăng câu hỏi thành công!'),
              backgroundColor: Color(0xFF4CAF50)),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Thất bại: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  InputDecoration _inputDeco(String label, {String? hint}) => InputDecoration(
        labelText: label,
        hintText: hint,
        labelStyle: const TextStyle(color: _primary),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: _primary, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Colors.red),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Colors.red, width: 2),
        ),
        filled: true,
        fillColor: Colors.white,
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text('Đặt câu hỏi', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextFormField(
                      controller: _titleCtrl,
                      decoration: _inputDeco('Tiêu đề', hint: 'Nhập tiêu đề câu hỏi'),
                      validator: (v) => (v == null || v.trim().length < 10)
                          ? 'Tiêu đề phải có ít nhất 10 ký tự' : null,
                      maxLength: 200,
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _bodyCtrl,
                      decoration: _inputDeco('Chi tiết', hint: 'Mô tả chi tiết câu hỏi của bạn'),
                      maxLines: 5,
                      validator: (v) => (v == null || v.trim().length < 20)
                          ? 'Chi tiết phải có ít nhất 20 ký tự' : null,
                      maxLength: 2000,
                    ),
                    const SizedBox(height: 14),
                    DropdownButtonFormField<String>(
                      value: _selectedTopicId,
                      decoration: _inputDeco('Chủ đề (tuỳ chọn)'),
                      items: [
                        const DropdownMenuItem(value: null, child: Text('-- Chọn chủ đề --')),
                        ..._topics.map((t) => DropdownMenuItem(
                            value: t['id'].toString(),
                            child: Text(t['name'] ?? t['id'].toString()))),
                      ],
                      onChanged: (v) => setState(() => _selectedTopicId = v),
                    ),
                    const SizedBox(height: 14),
                    DropdownButtonFormField<String>(
                      value: _stage,
                      decoration: _inputDeco('Giai đoạn'),
                      items: const [
                        DropdownMenuItem(value: 'PREGNANCY', child: Text('Thai kỳ')),
                        DropdownMenuItem(value: 'POSTPARTUM', child: Text('Sau sinh')),
                        DropdownMenuItem(value: 'BABY_CARE', child: Text('Chăm sóc bé')),
                      ],
                      onChanged: (v) => setState(() => _stage = v!),
                    ),
                    const SizedBox(height: 14),
                    DropdownButtonFormField<String>(
                      value: _urgency,
                      decoration: _inputDeco('Mức độ khẩn cấp'),
                      items: const [
                        DropdownMenuItem(value: 'NORMAL', child: Text('Bình thường')),
                        DropdownMenuItem(value: 'HIGH', child: Text('Khẩn')),
                        DropdownMenuItem(value: 'EMERGENCY', child: Text('Khẩn cấp')),
                      ],
                      onChanged: (v) => setState(() => _urgency = v!),
                    ),
                    const SizedBox(height: 8),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Đăng ẩn danh'),
                      subtitle: const Text('Tên của bạn sẽ được ẩn',
                          style: TextStyle(fontSize: 12, color: Colors.grey)),
                      value: _isAnonymous,
                      activeColor: _accent,
                      onChanged: (v) => setState(() => _isAnonymous = v),
                    ),
                    const SizedBox(height: 20),
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 15),
                        shape: const StadiumBorder(),
                      ),
                      onPressed: _submitting ? null : _submit,
                      child: _submitting
                          ? const SizedBox(
                              height: 20, width: 20,
                              child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                          : const Text('Đăng câu hỏi', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
