import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class PostAnswerScreen extends StatefulWidget {
  final String questionId;
  final String? questionTitle;

  const PostAnswerScreen({
    super.key,
    required this.questionId,
    this.questionTitle,
  });

  @override
  State<PostAnswerScreen> createState() => _PostAnswerScreenState();
}

class _PostAnswerScreenState extends State<PostAnswerScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _accent = Color(0xFFD4A895);

  final _formKey = GlobalKey<FormState>();
  final _bodyCtrl = TextEditingController();
  bool _isPersonalExperience = false;
  bool _submitting = false;

  @override
  void dispose() {
    _bodyCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      await apiPost(
        '/api/v1/community/questions/${widget.questionId}/answers',
        {
          'body': _bodyCtrl.text.trim(),
          'isPersonalExperience': _isPersonalExperience,
        },
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đăng câu trả lời thành công!'),
            backgroundColor: Color(0xFF4CAF50),
          ),
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: const Text('Trả lời câu hỏi', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (widget.questionTitle != null) ...[
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: _accent.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: _accent.withValues(alpha: 0.3)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Câu hỏi', style: TextStyle(color: _accent, fontSize: 12, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 4),
                      Text(widget.questionTitle!,
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],
              TextFormField(
                controller: _bodyCtrl,
                decoration: InputDecoration(
                  labelText: 'Câu trả lời của bạn',
                  hintText: 'Chia sẻ kiến thức hoặc kinh nghiệm (tối thiểu 20 ký tự)',
                  labelStyle: const TextStyle(color: _primary),
                  alignLabelWithHint: true,
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
                ),
                minLines: 6,
                maxLines: 12,
                maxLength: 3000,
                validator: (v) {
                  if (v == null || v.trim().length < 20) {
                    return 'Câu trả lời phải có ít nhất 20 ký tự';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Đây là kinh nghiệm cá nhân'),
                subtitle: const Text('Đánh dấu nếu câu trả lời dựa trên kinh nghiệm cá nhân',
                    style: TextStyle(fontSize: 12, color: Colors.grey)),
                value: _isPersonalExperience,
                activeThumbColor: _accent,
                onChanged: (v) => setState(() => _isPersonalExperience = v),
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
                    : const Text('Gửi câu trả lời',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
