import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class PostAnswerScreen extends StatefulWidget {
  final String questionId;
  final String? questionTitle;
  final String? questionBody;
  final String? authorName;
  final String? authorAvatarUrl;
  final String? topicName;
  final String? timeAgo;

  const PostAnswerScreen({
    super.key,
    required this.questionId,
    this.questionTitle,
    this.questionBody,
    this.authorName,
    this.authorAvatarUrl,
    this.topicName,
    this.timeAgo,
  });

  @override
  State<PostAnswerScreen> createState() => _PostAnswerScreenState();
}

class _PostAnswerScreenState extends State<PostAnswerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _error = Color(0xFFBA1A1A);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _surfaceAccentMuted = Color(0xFFF2EAE4);
  static const _outlineVariant = Color(0xFFD6C2BD);

  static const _experienceTags = ['Dinh dưỡng', 'Tâm lý', 'Mẹo dân gian', 'Khích lệ tinh thần'];

  final _bodyCtrl = TextEditingController();
  final Set<int> _selectedTags = {};
  bool _submitting = false;
  bool _showPendingModal = false;

  @override
  void dispose() {
    _bodyCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_bodyCtrl.text.trim().length < 10) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Câu trả lời phải có ít nhất 10 ký tự')),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      await apiPost(
        '/api/v1/community/questions/${widget.questionId}/answers',
        {
          'body': _bodyCtrl.text.trim(),
          'isPersonalExperience': _selectedTags.isNotEmpty,
        },
      );
      if (mounted) setState(() => _showPendingModal = true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Thất bại: $e'), backgroundColor: Colors.red));
      }
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
        elevation: 1,
        shadowColor: Colors.black12,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'CareBridge',
          style: TextStyle(
            color: _primary,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close, color: _onSurfaceVariant),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildQuestionContextCard(),
                const SizedBox(height: 16),
                _buildAnswerComposerCard(),
              ],
            ),
          ),
          if (_showPendingModal) _buildPendingModal(),
        ],
      ),
    );
  }

  Widget _buildQuestionContextCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: _primaryContainer.withValues(alpha: 0.08), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: _surfaceAccentMuted,
                backgroundImage: widget.authorAvatarUrl != null
                    ? NetworkImage(widget.authorAvatarUrl!)
                    : null,
                child: widget.authorAvatarUrl == null
                    ? const Icon(Icons.person, color: _primaryContainer)
                    : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.authorName ?? 'Ẩn danh',
                      style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: Color(0xFF2D2A28)),
                    ),
                    Row(
                      children: [
                        const Icon(Icons.calendar_today, size: 12, color: _onSurfaceVariant),
                        const SizedBox(width: 4),
                        Text(
                          '${widget.timeAgo ?? ''} ${widget.topicName != null ? "• ${widget.topicName}" : ""}',
                          style: const TextStyle(fontSize: 12, color: _onSurfaceVariant),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: _canvas,
                  borderRadius: BorderRadius.circular(99),
                  border: Border.all(color: _outlineVariant.withValues(alpha: 0.4)),
                ),
                child: const Text('Câu hỏi', style: TextStyle(fontSize: 12, color: _primary, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          if (widget.questionTitle != null) ...[
            const SizedBox(height: 12),
            Text(widget.questionTitle!, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: Color(0xFF2D2A28))),
          ],
          if (widget.questionBody != null) ...[
            const SizedBox(height: 8),
            Text(widget.questionBody!, style: const TextStyle(fontSize: 15, color: Color(0xFF524F4C), height: 1.5)),
          ],
        ],
      ),
    );
  }

  Widget _buildAnswerComposerCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: _primaryContainer.withValues(alpha: 0.08), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.edit_note, color: _primary),
              const SizedBox(width: 8),
              const Text('Viết câu trả lời', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 16, color: Color(0xFF2D2A28))),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDBD1),
                  borderRadius: BorderRadius.circular(99),
                ),
                child: const Text('Chia sẻ kinh nghiệm', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Color(0xFF693A2D))),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Safety warning
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: _errorContainer.withValues(alpha: 0.4),
              borderRadius: const BorderRadius.only(topRight: Radius.circular(8), bottomRight: Radius.circular(8)),
              border: const Border(left: BorderSide(color: _error, width: 4)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.warning_amber_rounded, color: _error, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: RichText(
                    text: const TextSpan(
                      style: TextStyle(fontSize: 13, color: _onErrorContainer),
                      children: [
                        TextSpan(text: 'Lưu ý an toàn:\n', style: TextStyle(fontWeight: FontWeight.bold)),
                        TextSpan(text: 'Mọi chia sẻ chỉ mang tính chất tham khảo. Vui lòng '),
                        TextSpan(text: 'không chẩn đoán bệnh', style: TextStyle(fontWeight: FontWeight.bold)),
                        TextSpan(text: ' hoặc '),
                        TextSpan(text: 'kê đơn thuốc', style: TextStyle(fontWeight: FontWeight.bold)),
                        TextSpan(text: '. Hãy khuyên người nhà thăm khám bác sĩ chuyên khoa.'),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Experience tags
          const Text('Thẻ kinh nghiệm liên quan:', style: TextStyle(fontSize: 12, color: _onSurfaceVariant, fontWeight: FontWeight.w500)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: List.generate(_experienceTags.length, (i) {
              final selected = _selectedTags.contains(i);
              return GestureDetector(
                onTap: () => setState(() {
                  if (selected) _selectedTags.remove(i); else _selectedTags.add(i);
                }),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surfaceAccentMuted,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Text(
                    _experienceTags[i],
                    style: TextStyle(fontSize: 13, color: selected ? Colors.white : const Color(0xFF2D2A28)),
                  ),
                ),
              );
            }),
          ),
          const SizedBox(height: 16),
          // Textarea
          Stack(
            alignment: Alignment.bottomRight,
            children: [
              TextField(
                controller: _bodyCtrl,
                maxLines: 8,
                maxLength: 3000,
                decoration: InputDecoration(
                  hintText: 'Hãy chia sẻ những trải nghiệm thực tế của bạn để giúp đỡ cộng đồng...',
                  hintStyle: const TextStyle(color: _onSurfaceVariant, fontSize: 14),
                  contentPadding: const EdgeInsets.fromLTRB(16, 16, 16, 48),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _surfaceAccentMuted, width: 2),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _surfaceAccentMuted, width: 2),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _primary, width: 2),
                  ),
                  filled: true,
                  fillColor: _surface,
                  counterText: '',
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(right: 8, bottom: 8),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(icon: const Icon(Icons.image_outlined, color: _onSurfaceVariant), onPressed: () {}),
                    IconButton(icon: const Icon(Icons.attach_file, color: _onSurfaceVariant), onPressed: () {}),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(color: _outlineVariant),
          const SizedBox(height: 12),
          // Submit row
          Row(
            children: [
              const Icon(Icons.verified_user_outlined, size: 18, color: _onSurfaceVariant),
              const SizedBox(width: 6),
              const Expanded(
                child: Text(
                  'Đóng góp của bạn sẽ được kiểm duyệt trước khi hiển thị.',
                  style: TextStyle(fontSize: 12, color: _onSurfaceVariant),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: const StadiumBorder(),
                elevation: 2,
              ),
              onPressed: _submitting ? null : _submit,
              icon: _submitting
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : const Icon(Icons.send, size: 18),
              label: const Text('Đăng trả lời', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPendingModal() {
    return Container(
      color: Colors.black54,
      child: Center(
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 32),
          padding: const EdgeInsets.all(28),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), boxShadow: [const BoxShadow(blurRadius: 24, color: Colors.black26)]),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 72, height: 72,
                decoration: const BoxDecoration(color: _canvas, shape: BoxShape.circle),
                child: const Icon(Icons.hourglass_empty, color: _primary, size: 36),
              ),
              const SizedBox(height: 20),
              const Text('Đang chờ kiểm duyệt', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF2D2A28))),
              const SizedBox(height: 8),
              const Text(
                'Cảm ơn bạn đã đóng góp! Câu trả lời của bạn đang được đội ngũ y khoa xem xét để đảm bảo an toàn nội dung.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, color: Color(0xFF524F4C), height: 1.5),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _primary,
                    side: BorderSide.none,
                    backgroundColor: _surfaceAccentMuted,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: const StadiumBorder(),
                  ),
                  onPressed: () => Navigator.of(context).pop(true),
                  child: const Text('Quay lại Trang chủ', style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
