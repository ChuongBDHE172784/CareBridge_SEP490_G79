import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../../../core/constants/content_stages.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import '../widgets/community_image_attachments.dart';

/// UC-55 — Edit Community Post
/// Allows a MOTHER to edit their own community question.
/// Uses PATCH /api/v1/community/questions/{id}.
/// Navigated to from QuestionDetailScreen when isMyQuestion=true.
class EditQuestionScreen extends StatefulWidget {
  final String questionId;
  final String initialTitle;
  final String initialBody;
  final String? initialTopicId;
  final String initialTopicName;
  final String initialStage;
  final List<String> initialImageUrls;
  final int? initialPregnancyWeek;
  final int? initialBabyAgeMonths;
  final bool initialIsAnonymous;
  final String initialUrgency;

  const EditQuestionScreen({
    super.key,
    required this.questionId,
    required this.initialTitle,
    required this.initialBody,
    required this.initialTopicId,
    required this.initialTopicName,
    required this.initialStage,
    this.initialImageUrls = const [],
    this.initialPregnancyWeek,
    this.initialBabyAgeMonths,
    required this.initialIsAnonymous,
    required this.initialUrgency,
  });

  @override
  State<EditQuestionScreen> createState() => _EditQuestionScreenState();
}

class _EditQuestionScreenState extends State<EditQuestionScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleCtrl;
  late final TextEditingController _bodyCtrl;
  final _imageService = CommunityImageService();
  final List<CommunityImageAttachment> _newImages = [];
  late final List<String> _existingImageUrls;
  List<CommunityTopic> _topics = [];
  String? _selectedTopicId;
  late String _stage;
  late String _urgency;
  late bool _isAnonymous;
  bool _loadingTopics = false;
  bool _submitting = false;

  static const _urgencyOptions = [
    {'value': 'LOW', 'label': 'Không gấp'},
    {'value': 'NORMAL', 'label': 'Bình thường'},
    {'value': 'URGENT', 'label': 'Khẩn cấp'},
  ];

  @override
  void initState() {
    super.initState();
    _titleCtrl = TextEditingController(text: widget.initialTitle);
    _bodyCtrl = TextEditingController(text: widget.initialBody);
    _selectedTopicId = widget.initialTopicId;
    _stage = normalizeContentStage(widget.initialStage);
    _existingImageUrls = List<String>.from(widget.initialImageUrls);
    _urgency = widget.initialUrgency;
    _isAnonymous = widget.initialIsAnonymous;
    _loadTopics();
  }

  Future<void> _loadTopics() async {
    setState(() => _loadingTopics = true);
    try {
      final topics = await CommunityService.instance.getQuestionTopics();
      if (!mounted) return;
      setState(() {
        _topics = topics;
        final selectedStillExists = topics.any(
          (topic) => topic.id == _selectedTopicId,
        );
        if (!selectedStillExists && _selectedTopicId != null) {
          _selectedTopicId = null;
        }
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _topics = [];
          _selectedTopicId = null;
        });
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể tải chủ đề: $error')));
      }
    } finally {
      if (mounted) setState(() => _loadingTopics = false);
    }
  }

  Future<void> _pickImage(ImageSource source) async {
    if (_existingImageUrls.length + _newImages.length >= communityImageLimit) {
      return;
    }
    try {
      final image = await _imageService.pick(source);
      if (image != null && mounted) setState(() => _newImages.add(image));
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể thêm ảnh: $error')));
      }
    }
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _bodyCtrl.dispose();
    super.dispose();
  }

  bool get _hasValidTopicSelection =>
      _selectedTopicId != null &&
      _topics.any((topic) => topic.id == _selectedTopicId);

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || !_hasValidTopicSelection) return;
    setState(() => _submitting = true);
    try {
      final uploadedUrls = await _imageService.uploadAll(
        _newImages,
        purpose: 'COMMUNITY_QUESTION_IMAGE',
      );
      await CommunityService.instance.editQuestion(
        widget.questionId,
        topicId: _selectedTopicId,
        title: _titleCtrl.text.trim(),
        body: _bodyCtrl.text.trim(),
        imageUrls: [..._existingImageUrls, ...uploadedUrls],
        stage: _stage,
        pregnancyWeek: widget.initialPregnancyWeek,
        babyAgeMonths: widget.initialBabyAgeMonths,
        isAnonymous: _isAnonymous,
        urgency: _urgency,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã cập nhật câu hỏi'),
            backgroundColor: Color(0xFF4CAF50),
          ),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Cập nhật thất bại: $e'),
            backgroundColor: _error,
          ),
        );
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
        elevation: 0,
        shadowColor: Colors.black12,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chỉnh sửa câu hỏi',
          style: TextStyle(
            color: _primary,
            fontWeight: FontWeight.bold,
            fontSize: 17,
          ),
        ),
        actions: [
          if (_submitting)
            const Center(
              child: Padding(
                padding: EdgeInsets.only(right: 16),
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _primary,
                  ),
                ),
              ),
            )
          else
            TextButton(
              onPressed: (_loadingTopics || !_hasValidTopicSelection)
                  ? null
                  : _submit,
              child: const Text(
                'Lưu',
                style: TextStyle(
                  color: _primary,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            // Title field
            _SectionLabel('Tiêu đề câu hỏi'),
            const SizedBox(height: 8),
            TextFormField(
              controller: _titleCtrl,
              maxLength: 255,
              decoration: _inputDecoration('Nhập tiêu đề câu hỏi...'),
              style: const TextStyle(fontSize: 15, color: _onSurface),
              validator: (v) {
                if (v == null || v.trim().length < 5) {
                  return 'Tiêu đề cần ít nhất 5 ký tự';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Body field
            _SectionLabel('Nội dung'),
            const SizedBox(height: 8),
            TextFormField(
              controller: _bodyCtrl,
              maxLength: 5000,
              maxLines: 8,
              decoration: _inputDecoration('Mô tả chi tiết vấn đề của bạn...'),
              style: const TextStyle(fontSize: 15, color: _onSurface),
              validator: (v) {
                if (v == null || v.trim().length < 10) {
                  return 'Nội dung cần ít nhất 10 ký tự';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            _SectionLabel('Hình ảnh'),
            const SizedBox(height: 8),
            CommunityImagePickerField(
              images: _newImages,
              existingImageUrls: _existingImageUrls,
              enabled: !_submitting,
              onCamera: () => _pickImage(ImageSource.camera),
              onGallery: () => _pickImage(ImageSource.gallery),
              onRemove: (index) => setState(() => _newImages.removeAt(index)),
              onRemoveExisting: (index) =>
                  setState(() => _existingImageUrls.removeAt(index)),
            ),
            const SizedBox(height: 16),

            _SectionLabel('Chủ đề'),
            const SizedBox(height: 8),
            if (_loadingTopics)
              const Center(child: CircularProgressIndicator(color: _primary))
            else
              DropdownButtonFormField<String>(
                initialValue: _selectedTopicId,
                decoration: _inputDecoration('Chọn chủ đề'),
                items: _topics
                    .map(
                      (topic) => DropdownMenuItem<String>(
                        value: topic.id,
                        child: Text(topic.name),
                      ),
                    )
                    .toList(),
                validator: (value) => value == null || value.isEmpty
                    ? 'Vui lòng chọn chủ đề'
                    : null,
                onChanged: (value) => setState(() => _selectedTopicId = value),
              ),
            const SizedBox(height: 16),

            _SectionLabel('Giai đoạn liên quan'),
            const SizedBox(height: 4),
            const Text(
              'Chọn giai đoạn của mẹ hoặc em bé được nhắc đến.',
              style: TextStyle(color: _onSurfaceVariant, fontSize: 12),
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              initialValue: _stage,
              decoration: _inputDecoration('Chọn giai đoạn'),
              items: contentStageOptions
                  .map(
                    (stage) => DropdownMenuItem(
                      value: stage.value,
                      child: Text(stage.label),
                    ),
                  )
                  .toList(growable: false),
              onChanged: (value) {
                if (value != null) setState(() => _stage = value);
              },
            ),
            const SizedBox(height: 16),

            // Urgency picker
            _SectionLabel('Mức độ ưu tiên'),
            const SizedBox(height: 8),
            Container(
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: _outlineVariant),
              ),
              child: RadioGroup<String>(
                groupValue: _urgency,
                onChanged: (value) {
                  if (value != null) {
                    setState(() => _urgency = value);
                  }
                },
                child: Column(
                  children: _urgencyOptions.map((opt) {
                    final selected = _urgency == opt['value'];
                    return RadioListTile<String>(
                      value: opt['value']!,
                      title: Text(
                        opt['label']!,
                        style: TextStyle(
                          fontSize: 14,
                          color: selected ? _primary : _onSurface,
                          fontWeight: selected
                              ? FontWeight.w600
                              : FontWeight.normal,
                        ),
                      ),
                      activeColor: _primary,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                      ),
                      dense: true,
                    );
                  }).toList(),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Anonymous toggle
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: _outlineVariant),
              ),
              child: Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: _surfaceContainerHigh,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(
                      Icons.person_off_outlined,
                      color: _primary,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Đăng ẩn danh',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                        Text(
                          'Tên của bạn sẽ bị ẩn',
                          style: TextStyle(
                            fontSize: 12,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    value: _isAnonymous,
                    onChanged: (v) => setState(() => _isAnonymous = v),
                    activeThumbColor: _primary,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Save button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: const StadiumBorder(),
                  textStyle: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                onPressed: _submitting ? null : _submit,
                child: _submitting
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('Lưu thay đổi'),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  InputDecoration _inputDecoration(String hint) => InputDecoration(
    hintText: hint,
    hintStyle: const TextStyle(color: _outline, fontSize: 14),
    filled: true,
    fillColor: _surface,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: _outlineVariant),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: _outlineVariant),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: _primaryContainer, width: 1.5),
    ),
    errorBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: _error),
    ),
    counterStyle: const TextStyle(color: _outline, fontSize: 11),
  );
}

class _SectionLabel extends StatelessWidget {
  final String text;
  const _SectionLabel(this.text);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w600,
        color: Color(0xFF524440),
        letterSpacing: 0.2,
      ),
    );
  }
}
