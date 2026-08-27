import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../widgets/community_image_attachments.dart';

import '../models/community_model.dart';

String postAnswerErrorMessage(Object error) {
  if (error is ApiException && error.statusCode == 422) {
    try {
      final payload = jsonDecode(error.message) as Map<String, dynamic>;
      if (payload['error'] == 'COM-007') {
        return 'Câu hỏi đang chờ duyệt hoặc không còn mở để trả lời.';
      }
    } catch (_) {
      // Fall through to the generic user-facing message.
    }
  }
  return 'Không thể gửi câu trả lời. Vui lòng thử lại.';
}

class PostAnswerScreen extends StatefulWidget {
  final String questionId;
  final String? questionTitle;
  final String? questionBody;
  final String? authorName;
  final String? authorAvatarUrl;
  final String? topicName;
  final String? timeAgo;
  final CommunityAnswer? existingAnswer;

  const PostAnswerScreen({
    super.key,
    required this.questionId,
    this.questionTitle,
    this.questionBody,
    this.authorName,
    this.authorAvatarUrl,
    this.topicName,
    this.timeAgo,
    this.existingAnswer,
  });

  @override
  State<PostAnswerScreen> createState() => _PostAnswerScreenState();
}

class _PostAnswerScreenState extends State<PostAnswerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _error = Color(0xFFBA1A1A);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _surfaceAccentMuted = Color(0xFFF8EEE9);
  static const _outlineVariant = Color(0xFFE5D3CA);

  static const _experienceTags = [
    'Trải nghiệm thực tế',
    'Chăm sóc hằng ngày',
    'Hỗ trợ tinh thần',
    'Thông tin tham khảo',
  ];

  final _bodyCtrl = TextEditingController();
  final Set<int> _selectedTags = {};
  final _imageService = CommunityImageService();
  final List<CommunityImageAttachment> _images = [];
  final List<String> _existingImageUrls = [];
  bool _submitting = false;
  bool _showPublishedModal = false;
  bool get _isExpert => AuthState.instance.role?.trim().toUpperCase() == 'EXPERT';

  @override
  void initState() {
    super.initState();
    if (widget.existingAnswer != null) {
      _bodyCtrl.text = widget.existingAnswer!.body;
      final tag = widget.existingAnswer!.experienceTag;
      final index = tag == null ? 0 : _experienceTags.indexOf(tag);
      if (!_isExpert && widget.existingAnswer!.personalExperience && index >= 0) {
        _selectedTags.add(index);
      }
      _existingImageUrls.addAll(widget.existingAnswer!.imageUrls);
    }
  }

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
      final newUploadedUrls = await _imageService.uploadAll(
        _images,
        purpose: 'COMMUNITY_ANSWER_IMAGE',
      );
      final finalImageUrls = [..._existingImageUrls, ...newUploadedUrls];
      if (widget.existingAnswer != null) {
        await apiPatch(
          '/api/v1/community/questions/${widget.questionId}/answers/${widget.existingAnswer!.id}',
          {
            'body': _bodyCtrl.text.trim(),
            'isPersonalExperience': _selectedTags.isNotEmpty,
            'experienceTag': _selectedTags.isEmpty ? null : _experienceTags[_selectedTags.first],
            'imageUrls': finalImageUrls,
          },
        );
      } else {
        await apiPost(
          '/api/v1/community/questions/${widget.questionId}/answers',
          {
            'body': _bodyCtrl.text.trim(),
            'isPersonalExperience': _selectedTags.isNotEmpty,
            'experienceTag': _selectedTags.isEmpty ? null : _experienceTags[_selectedTags.first],
            'imageUrls': finalImageUrls,
          },
        );
      }
      if (mounted) setState(() => _showPublishedModal = true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(postAnswerErrorMessage(e)),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _pickImage(ImageSource source) async {
    if (_images.length >= communityImageLimit) return;
    try {
      final image = await _imageService.pick(source);
      if (image != null && mounted) setState(() => _images.add(image));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể thêm ảnh: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = ModalRoute.of(context)?.canPop ?? false;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Container(
              width: double.infinity,
              decoration: const BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
                boxShadow: [
                  BoxShadow(
                    color: Color(0x0A845143),
                    blurRadius: 16,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              padding: EdgeInsets.fromLTRB(16, topInset + 12, 16, 16),
              child: Row(
                children: [
                  if (canPop) ...[
                    IconButton(
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      icon: const Icon(Icons.arrow_back_ios_new_rounded, color: _primary, size: 20),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                    const SizedBox(width: 12),
                  ] else
                    const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      widget.existingAnswer != null ? 'Chỉnh sửa câu trả lời' : 'Gửi câu trả lời',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                        letterSpacing: -0.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: Stack(
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
                  if (_showPublishedModal) _buildPublishedModal(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuestionContextCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: _primaryContainer.withValues(alpha: 0.08),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
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
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                        color: Color(0xFF2D2A28),
                      ),
                    ),
                    Row(
                      children: [
                        const Icon(
                          Icons.calendar_today,
                          size: 12,
                          color: _onSurfaceVariant,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          '${widget.timeAgo ?? ''} ${widget.topicName != null ? "• ${widget.topicName}" : ""}',
                          style: const TextStyle(
                            fontSize: 12,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: _canvas,
                  borderRadius: BorderRadius.circular(99),
                  border: Border.all(
                    color: _outlineVariant.withValues(alpha: 0.4),
                  ),
                ),
                child: const Text(
                  'Câu hỏi',
                  style: TextStyle(
                    fontSize: 12,
                    color: _primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          if (widget.questionTitle != null) ...[
            const SizedBox(height: 12),
            Text(
              widget.questionTitle!,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: Color(0xFF2D2A28),
              ),
            ),
          ],
          if (widget.questionBody != null) ...[
            const SizedBox(height: 8),
            Text(
              widget.questionBody!,
              style: const TextStyle(
                fontSize: 15,
                color: Color(0xFF524F4C),
                height: 1.5,
              ),
            ),
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
        boxShadow: [
          BoxShadow(
            color: _primaryContainer.withValues(alpha: 0.08),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.edit_note, color: _primary),
              const SizedBox(width: 8),
              const Text(
                'Viết câu trả lời',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 16,
                  color: Color(0xFF2D2A28),
                ),
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDBD1),
                  borderRadius: BorderRadius.circular(99),
                ),
                child: const Text(
                  'Chia sẻ kinh nghiệm',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF693A2D),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Safety warning
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: _errorContainer.withValues(alpha: 0.4),
              borderRadius: const BorderRadius.only(
                topRight: Radius.circular(8),
                bottomRight: Radius.circular(8),
              ),
              border: const Border(left: BorderSide(color: _error, width: 4)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  Icons.warning_amber_rounded,
                  color: _error,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: RichText(
                    text: const TextSpan(
                      style: TextStyle(fontSize: 13, color: _onErrorContainer),
                      children: [
                        TextSpan(
                          text: 'Lưu ý an toàn:\n',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        TextSpan(
                          text:
                              'Mọi chia sẻ chỉ mang tính chất tham khảo. Vui lòng ',
                        ),
                        TextSpan(
                          text: 'không chẩn đoán bệnh',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        TextSpan(text: ' hoặc '),
                        TextSpan(
                          text: 'kê đơn thuốc',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        TextSpan(
                          text:
                              '. Hãy khuyên người nhà thăm khám bác sĩ chuyên khoa.',
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (!_isExpert) ...[
          // Experience tags
          const Text(
            'Thẻ kinh nghiệm liên quan:',
            style: TextStyle(
              fontSize: 12,
              color: _onSurfaceVariant,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: List.generate(_experienceTags.length, (i) {
              final selected = _selectedTags.contains(i);
              return GestureDetector(
                onTap: () => setState(() {
                  if (selected) {
                    _selectedTags.remove(i);
                  } else {
                    _selectedTags.clear();
                    _selectedTags.add(i);
                  }
                }),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surfaceAccentMuted,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Text(
                    _experienceTags[i],
                    style: TextStyle(
                      fontSize: 13,
                      color: selected ? Colors.white : const Color(0xFF2D2A28),
                    ),
                  ),
                ),
              );
            }),
          ),
          const SizedBox(height: 16),
          ],
          TextField(
            controller: _bodyCtrl,
            maxLines: 8,
            maxLength: 3000,
            decoration: InputDecoration(
              hintText:
                  'Hãy chia sẻ những trải nghiệm thực tế của bạn để giúp đỡ cộng đồng...',
              hintStyle: const TextStyle(
                color: _onSurfaceVariant,
                fontSize: 14,
              ),
              contentPadding: const EdgeInsets.all(16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(
                  color: _surfaceAccentMuted,
                  width: 2,
                ),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(
                  color: _surfaceAccentMuted,
                  width: 2,
                ),
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
          const SizedBox(height: 12),
          CommunityImagePickerField(
            images: _images,
            existingImageUrls: _existingImageUrls,
            enabled: !_submitting,
            onCamera: () => _pickImage(ImageSource.camera),
            onGallery: () => _pickImage(ImageSource.gallery),
            onRemove: (index) => setState(() => _images.removeAt(index)),
            onRemoveExisting: (index) =>
                setState(() => _existingImageUrls.removeAt(index)),
          ),
          const SizedBox(height: 16),
          const Divider(color: _outlineVariant),
          const SizedBox(height: 12),
          // Submit row
          Row(
            children: [
              const Icon(
                Icons.verified_user_outlined,
                size: 18,
                color: _onSurfaceVariant,
              ),
              const SizedBox(width: 6),
              const Expanded(
                child: Text(
                  'Câu trả lời sẽ được kiểm duyệt trước khi hiển thị.',
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
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : Icon(widget.existingAnswer != null ? Icons.save : Icons.send, size: 18),
              label: Text(
                widget.existingAnswer != null ? 'Lưu thay đổi' : 'Đăng trả lời',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPublishedModal() {
    return Container(
      color: Colors.black54,
      child: Center(
        child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 32),
          padding: const EdgeInsets.all(28),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [const BoxShadow(blurRadius: 24, color: Colors.black26)],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: const BoxDecoration(
                  color: _canvas,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.check_circle_outline,
                  color: _primary,
                  size: 36,
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                'Đã gửi câu trả lời',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2D2A28),
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Cảm ơn bạn đã đóng góp! Câu trả lời đang được kiểm duyệt trước khi hiển thị trong cộng đồng.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  color: Color(0xFF524F4C),
                  height: 1.5,
                ),
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
                  child: const Text(
                    'Quay lại bài viết',
                    style: TextStyle(fontWeight: FontWeight.bold),
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
