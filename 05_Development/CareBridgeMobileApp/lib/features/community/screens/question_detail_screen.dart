import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/constants/content_stages.dart';
import '../models/community_model.dart';
import '../services/community_service.dart';
import '../widgets/community_image_attachments.dart';
import 'post_answer_screen.dart';
import 'edit_question_screen.dart';

bool canAnswerCommunityQuestion(String? status) => status == 'APPROVED';

/// CB-014 — UC-199 View Community Question Detail
/// Displays a single approved community question with its answers.
/// Supports UC-59 (like answer) and UC-58 (bookmark from header action).
/// Navigates to PostAnswerScreen (UC-56) via FAB.
/// Navigates to EditQuestionScreen (UC-55) for post author.
class QuestionDetailScreen extends StatefulWidget {
  final String questionId;

  const QuestionDetailScreen({super.key, required this.questionId});

  @override
  State<QuestionDetailScreen> createState() => _QuestionDetailScreenState();
}

class _QuestionDetailScreenState extends State<QuestionDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerHigh = Color(0xFFF1E6E0);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _secondaryContainer = Color(0xFFF1E6E0);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFE5D3CA);

  final _service = CommunityService.instance;
  QuestionDetail? _question;
  bool _loading = true;
  bool _bookmarked = false;
  bool _questionLiked = false;
  int _questionLikeCount = 0;

  // Track per-answer like state locally
  final Map<String, bool> _likedAnswers = {};
  final Map<String, int> _answerLikeCounts = {};

  @override
  void initState() {
    super.initState();
    _loadDetail();
  }

  // True when the currently authenticated user authored this question.
  // Computed from the loaded detail (which carries the real authorId), never from
  // navigation-time guesses — the feed/search list responses never expose raw
  // authorId (only a masked display name), so it cannot be known before loading.
  bool get _isMyQuestion =>
      _question != null &&
      _question!.authorId != null &&
      _question!.authorId == AuthState.instance.userId;

  bool get _canAnswerQuestion => canAnswerCommunityQuestion(_question?.status);

  Future<void> _loadDetail() async {
    setState(() => _loading = true);
    try {
      final q = await _service.getQuestionDetail(widget.questionId);
      if (mounted) {
        setState(() {
          _question = q;
          _bookmarked = q.isBookmarked;
          _questionLiked = q.isLiked;
          _questionLikeCount = q.likeCount;
          for (final a in q.answers) {
            _likedAnswers[a.id] = a.liked;
            _answerLikeCounts[a.id] = a.likeCount;
          }
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleBookmark() async {
    try {
      final result = await _service.toggleBookmark(widget.questionId);
      if (mounted) setState(() => _bookmarked = result.bookmarked);
    } catch (_) {}
  }

  Future<void> _toggleQuestionLike() async {
    final wasLiked = _questionLiked;
    final prevCount = _questionLikeCount;
    setState(() {
      _questionLiked = !wasLiked;
      _questionLikeCount = wasLiked ? prevCount - 1 : prevCount + 1;
    });
    try {
      final result = await _service.toggleQuestionLike(widget.questionId);
      if (mounted) {
        setState(() {
          _questionLiked = result.liked;
          _questionLikeCount = result.likeCount;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _questionLiked = wasLiked;
          _questionLikeCount = prevCount;
        });
      }
    }
  }

  // UC-170: soft-delete own question
  Future<void> _deleteQuestion() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xóa câu hỏi'),
        content: const Text(
          'Bạn có chắc muốn xóa câu hỏi này? Hành động này không thể hoàn tác.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Xóa', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _service.deleteQuestion(widget.questionId);
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể xóa câu hỏi: $e')));
      }
    }
  }

  bool _isOwnAnswer(CommunityAnswer answer) =>
      answer.authorId != null && answer.authorId == AuthState.instance.userId;

  Future<void> _editAnswer(CommunityAnswer answer) async {
    final updated = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => PostAnswerScreen(
          questionId: widget.questionId,
          questionTitle: _question?.title,
          questionBody: _question?.body,
          authorName: _question?.authorDisplay,
          existingAnswer: answer,
        ),
      ),
    );
    if (updated == true) {
      _loadDetail();
    }
  }

  // UC-201: soft-delete own answer
  Future<void> _deleteAnswer(CommunityAnswer answer) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xóa câu trả lời'),
        content: const Text('Bạn có chắc muốn xóa câu trả lời này?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Xóa', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _service.deleteAnswer(widget.questionId, answer.id);
      _loadDetail();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể xóa câu trả lời: $e')),
        );
      }
    }
  }

  Future<void> _toggleLike(String answerId) async {
    final wasl = _likedAnswers[answerId] ?? false;
    final prevCount = _answerLikeCounts[answerId] ?? 0;
    // Optimistic update
    setState(() {
      _likedAnswers[answerId] = !wasl;
      _answerLikeCounts[answerId] = wasl ? prevCount - 1 : prevCount + 1;
    });
    try {
      final result = await _service.toggleAnswerLike(answerId);
      if (mounted) {
        setState(() {
          _likedAnswers[answerId] = result.liked;
          _answerLikeCounts[answerId] = result.likeCount;
        });
      }
    } catch (_) {
      // Rollback on error
      if (mounted) {
        setState(() {
          _likedAnswers[answerId] = wasl;
          _answerLikeCounts[answerId] = prevCount;
        });
      }
    }
  }

  Future<void> _reportTarget({
    required String targetType,
    required String targetId,
  }) async {
    const categories = <String, String>{
      'UNSAFE_ADVICE': 'Tư vấn không an toàn',
      'HARASSMENT': 'Quấy rối',
      'SPAM': 'Spam',
      'INACCURATE_INFORMATION': 'Thông tin sai lệch',
      'DISGUISED_ADVERTISING': 'Quảng cáo trá hình',
      'OTHER': 'Khác',
    };
    String category = 'UNSAFE_ADVICE';
    final controller = TextEditingController();
    final submitted = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('Báo cáo nội dung'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              DropdownButtonFormField<String>(
                initialValue: category,
                decoration: const InputDecoration(labelText: 'Lý do'),
                items: categories.entries
                    .map(
                      (entry) => DropdownMenuItem(
                        value: entry.key,
                        child: Text(entry.value),
                      ),
                    )
                    .toList(),
                onChanged: (value) {
                  if (value != null) setDialogState(() => category = value);
                },
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                minLines: 3,
                maxLines: 5,
                maxLength: 500,
                decoration: const InputDecoration(
                  labelText: 'Ghi chú',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Hủy'),
            ),
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Gửi báo cáo'),
            ),
          ],
        ),
      ),
    );
    if (submitted != true) return;
    try {
      await _service.reportTarget(
        targetType: targetType,
        targetId: targetId,
        category: category,
        description: controller.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã gửi báo cáo đến đội ngũ kiểm duyệt'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể gửi báo cáo: $e')));
      }
    }
  }

  String _formatTimeAgo(String iso) {
    try {
      final dt = DateTime.parse(iso);
      final diff = DateTime.now().difference(dt);
      if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
      if (diff.inHours < 24) return '${diff.inHours} giờ trước';
      return '${diff.inDays} ngày trước';
    } catch (_) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = ModalRoute.of(context)?.canPop ?? false;

    return Scaffold(
      backgroundColor: _canvas,
      floatingActionButton: !_canAnswerQuestion
          ? null
          : FloatingActionButton.extended(
              key: const Key('fab-answer-question'),
              backgroundColor: _primary,
              elevation: 4,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              onPressed: () async {
                final answered = await Navigator.of(context).push<bool>(
                  MaterialPageRoute(
                    builder: (_) => PostAnswerScreen(
                      questionId: widget.questionId,
                      questionTitle: _question?.title,
                      questionBody: _question?.body,
                      topicName: _question?.topicName,
                    ),
                  ),
                );
                if (answered == true) _loadDetail();
              },
              label: const Text(
                'Trả lời ngay',
                style: TextStyle(fontFamily: 'Lexend', color: Colors.white, fontWeight: FontWeight.w700),
              ),
              icon: const Icon(Icons.edit_note_rounded, color: Colors.white),
            ),
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
                  const Expanded(
                    child: Text(
                      'Chi tiết câu hỏi',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                        letterSpacing: -0.3,
                      ),
                    ),
                  ),
                  if (_question != null && !_isMyQuestion)
                    IconButton(
                      icon: const Icon(Icons.flag_outlined, color: _onSurfaceVariant, size: 20),
                      onPressed: () => _reportTarget(
                        targetType: 'QUESTION',
                        targetId: widget.questionId,
                      ),
                      tooltip: 'Báo cáo câu hỏi',
                    ),
                  if (_isMyQuestion)
                    IconButton(
                      icon: const Icon(Icons.edit_outlined, color: _onSurfaceVariant, size: 20),
                      onPressed: () async {
                        if (_question == null) return;
                        final updated = await Navigator.of(context).push<bool>(
                          MaterialPageRoute(
                            builder: (_) => EditQuestionScreen(
                              questionId: widget.questionId,
                              initialTitle: _question!.title,
                              initialBody: _question!.body,
                              initialTopicId: _question!.topicId,
                              initialTopicName: _question!.topicName,
                              initialStage: _question!.stage,
                              initialImageUrls: _question!.imageUrls,
                              initialPregnancyWeek: _question!.pregnancyWeek,
                              initialBabyAgeMonths: _question!.babyAgeMonths,
                              initialIsAnonymous: _question!.anonymous,
                              initialUrgency: _question!.urgency,
                            ),
                          ),
                        );
                        if (updated == true) _loadDetail();
                      },
                      tooltip: 'Chỉnh sửa',
                    ),
                  if (_isMyQuestion)
                    IconButton(
                      icon: const Icon(Icons.delete_outline_rounded, color: Color(0xFFBA1A1A), size: 20),
                      onPressed: _deleteQuestion,
                      tooltip: 'Xóa câu hỏi',
                    ),
                  IconButton(
                    icon: Icon(
                      _bookmarked ? Icons.bookmark_rounded : Icons.bookmark_border_rounded,
                      color: _bookmarked ? _primary : _onSurfaceVariant,
                      size: 22,
                    ),
                    onPressed: _toggleBookmark,
                    tooltip: 'Lưu câu hỏi',
                  ),
                ],
              ),
            ),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: _primary))
                  : _question == null
                  ? _buildError()
                  : RefreshIndicator(
                      color: _primary,
                      onRefresh: _loadDetail,
                      child: _buildContent(),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: _outline, size: 48),
          const SizedBox(height: 12),
          const Text(
            'Không tải được câu hỏi',
            style: TextStyle(color: _onSurfaceVariant),
          ),
          const SizedBox(height: 16),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
            ),
            onPressed: _loadDetail,
            child: const Text('Thử lại'),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    final q = _question!;
    return CustomScrollView(
      slivers: [
        // Question header card
        SliverToBoxAdapter(
          child: Container(
            margin: const EdgeInsets.fromLTRB(16, 16, 16, 0),
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.06),
                  blurRadius: 16,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Topic & stage tags
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    if (q.topicName.isNotEmpty) _Tag(q.topicName),
                    if (q.stage.isNotEmpty) _Tag(contentStageLabel(q.stage)),
                    if (q.urgency == 'URGENT')
                      _Tag(
                        'Khẩn cấp',
                        color: const Color(0xFFFFDAD6),
                        textColor: const Color(0xFF93000A),
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                // Title
                Text(
                  q.title,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 12),
                // Author & time row
                Row(
                  children: [
                    CircleAvatar(
                      radius: 16,
                      backgroundColor: _secondaryContainer,
                      child: const Icon(
                        Icons.person,
                        size: 16,
                        color: _primary,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          q.authorDisplay ?? 'Người dùng',
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                        Text(
                          _formatTimeAgo(q.createdAt),
                          style: const TextStyle(
                            fontSize: 11,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                    if (q.anonymous)
                      Padding(
                        padding: const EdgeInsets.only(left: 6),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: _surfaceContainerHigh,
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: const Text(
                            'Ẩn danh',
                            style: TextStyle(fontSize: 10, color: _outline),
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 14),
                // Body
                Text(
                  q.body,
                  style: const TextStyle(
                    fontSize: 15,
                    color: _onSurface,
                    height: 1.5,
                  ),
                ),
                if (q.imageUrls.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  CommunityNetworkImageGallery(imageUrls: q.imageUrls),
                ],
                const SizedBox(height: 14),
                const Divider(color: _outlineVariant, height: 1),
                const SizedBox(height: 12),
                // Stats row
                Row(
                  children: [
                    const Icon(
                      Icons.chat_bubble_outline,
                      size: 16,
                      color: _onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '${q.answerCount} câu trả lời',
                      style: const TextStyle(
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(width: 16),
                    GestureDetector(
                      onTap: _toggleQuestionLike,
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            _questionLiked
                                ? Icons.favorite
                                : Icons.favorite_border,
                            size: 16,
                            color: _questionLiked
                                ? _primary
                                : _onSurfaceVariant,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            '$_questionLikeCount',
                            style: const TextStyle(
                              fontSize: 12,
                              color: _onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),

        if (!_canAnswerQuestion)
          SliverToBoxAdapter(
            child: Container(
              margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEFB8),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Row(
                children: [
                  Icon(Icons.hourglass_top_rounded, color: Color(0xFF745600)),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Câu hỏi đang chờ duyệt. Bạn có thể trả lời sau khi câu hỏi được phê duyệt.',
                      style: TextStyle(color: Color(0xFF5C4300), height: 1.35),
                    ),
                  ),
                ],
              ),
            ),
          ),

        // Answers header
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 8),
            child: Row(
              children: [
                Text(
                  'Câu trả lời (${q.answers.length})',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _primary,
                  ),
                ),
              ],
            ),
          ),
        ),

        // Empty answers state
        if (q.answers.isEmpty)
          SliverToBoxAdapter(
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: _surfaceContainerLow,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  const Icon(
                    Icons.forum_outlined,
                    color: _primaryContainer,
                    size: 40,
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    'Chưa có câu trả lời nào',
                    style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _canAnswerQuestion
                        ? 'Hãy là người đầu tiên trả lời!'
                        : 'Câu hỏi cần được duyệt trước khi nhận câu trả lời.',
                    style: const TextStyle(fontSize: 12, color: _outline),
                  ),
                ],
              ),
            ),
          ),

        // Answer cards
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
          sliver: SliverList(
            delegate: SliverChildBuilderDelegate(
              (context, i) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _AnswerCard(
                  answer: q.answers[i],
                  liked: _likedAnswers[q.answers[i].id] ?? false,
                  likeCount: _answerLikeCounts[q.answers[i].id] ?? 0,
                  onLike: () => _toggleLike(q.answers[i].id),
                  formatTimeAgo: _formatTimeAgo,
                  isOwnAnswer: _isOwnAnswer(q.answers[i]),
                  onEdit: () => _editAnswer(q.answers[i]),
                  onDelete: () => _deleteAnswer(q.answers[i]),
                  onReport: () => _reportTarget(
                    targetType: 'ANSWER',
                    targetId: q.answers[i].id,
                  ),
                  onReportAuthor:
                      q.answers[i].authorId != null &&
                          !_isOwnAnswer(q.answers[i])
                      ? () => _reportTarget(
                          targetType: 'USER',
                          targetId: q.answers[i].authorId!,
                        )
                      : null,
                ),
              ),
              childCount: q.answers.length,
            ),
          ),
        ),
      ],
    );
  }
}

class _Tag extends StatelessWidget {
  static const _container = Color(0xFFEEDDD5);
  static const _txt = Color(0xFF845143);

  final String label;
  final Color? color;
  final Color? textColor;

  const _Tag(this.label, {this.color, this.textColor});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color ?? _container,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          color: textColor ?? _txt,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}

class _AnswerCard extends StatelessWidget {
  final CommunityAnswer answer;
  final bool liked;
  final int likeCount;
  final VoidCallback onLike;
  final String Function(String) formatTimeAgo;
  final bool isOwnAnswer;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final VoidCallback onReport;
  final VoidCallback? onReportAuthor;

  const _AnswerCard({
    required this.answer,
    required this.liked,
    required this.likeCount,
    required this.onLike,
    required this.formatTimeAgo,
    this.isOwnAnswer = false,
    required this.onEdit,
    required this.onDelete,
    required this.onReport,
    this.onReportAuthor,
  });

  @override
  Widget build(BuildContext context) {
    final hasExpertProfileId =
        answer.expertLabeled &&
        answer.expertProfileId != null &&
        answer.expertProfileId!.isNotEmpty;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFFFF),
        borderRadius: BorderRadius.circular(16),
        border: answer.expertLabeled
            ? Border.all(
                color: const Color(0xFFC98C7B).withValues(alpha: 0.4),
                width: 1.5,
              )
            : null,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 12,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (isOwnAnswer &&
              (answer.status == 'PENDING' || answer.status == 'AI_PENDING')) ...[
            Container(
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF3CD),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFFFEEBA)),
              ),
              child: const Row(
                children: [
                  Icon(
                    Icons.hourglass_top_rounded,
                    size: 16,
                    color: Color(0xFF856404),
                  ),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Câu trả lời của bạn đang chờ AI kiểm duyệt nội dung...',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: Color(0xFF856404),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          // Author row
          Row(
            children: [
              CircleAvatar(
                radius: 16,
                backgroundColor: answer.expertLabeled
                    ? const Color(0xFFC98C7B)
                    : const Color(0xFFF6DACF),
                child: Icon(
                  answer.expertLabeled ? Icons.verified : Icons.person,
                  size: 16,
                  color: answer.expertLabeled
                      ? Colors.white
                      : const Color(0xFF845143),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    GestureDetector(
                      onTap: hasExpertProfileId
                          ? () => context.push(
                              '/expert/public/${answer.expertProfileId}',
                            )
                          : null,
                      child: Row(
                        children: [
                          Text(
                            answer.expertLabeled
                                ? 'Chuyên gia'
                                : (answer.authorDisplay ?? 'Thành viên'),
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: answer.expertLabeled
                                  ? const Color(0xFF845143)
                                  : const Color(0xFF271812),
                              decoration: hasExpertProfileId
                                  ? TextDecoration.underline
                                  : null,
                              decorationColor: const Color(0xFFC98C7B),
                            ),
                          ),
                          if (hasExpertProfileId)
                            const Icon(
                              Icons.verified,
                              size: 14,
                              color: Color(0xFF845143),
                            ),
                        ],
                      ),
                    ),
                    Text(
                      formatTimeAgo(answer.createdAt),
                      style: const TextStyle(
                        fontSize: 11,
                        color: Color(0xFF524440),
                      ),
                    ),
                  ],
                ),
              ),
              if (answer.personalExperience)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFE2D9),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    answer.experienceTag ?? 'Kinh nghiệm',
                    style: TextStyle(fontSize: 10, color: Color(0xFF845143)),
                  ),
                ),
              if (isOwnAnswer)
                PopupMenuButton<String>(
                  icon: const Icon(
                    Icons.more_vert,
                    size: 18,
                    color: Color(0xFF524440),
                  ),
                  onSelected: (value) {
                    if (value == 'edit') onEdit();
                    if (value == 'delete') onDelete();
                  },
                  itemBuilder: (ctx) => const [
                    PopupMenuItem(value: 'edit', child: Text('Chỉnh sửa')),
                    PopupMenuItem(value: 'delete', child: Text('Xóa')),
                  ],
                ),
            ],
          ),
          const SizedBox(height: 10),
          // Body
          Text(
            answer.body,
            style: const TextStyle(
              fontSize: 14,
              color: Color(0xFF271812),
              height: 1.5,
            ),
          ),
          if (answer.imageUrls.isNotEmpty) ...[
            const SizedBox(height: 10),
            CommunityNetworkImageGallery(imageUrls: answer.imageUrls),
          ],
          const SizedBox(height: 10),
          const Divider(color: Color(0xFFD6C2BD), height: 1),
          const SizedBox(height: 8),
          // Like action
          Row(
            children: [
              InkWell(
                onTap: onLike,
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        liked ? Icons.favorite : Icons.favorite_border,
                        size: 18,
                        color: liked
                            ? const Color(0xFF845143)
                            : const Color(0xFF524440),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '$likeCount',
                        style: TextStyle(
                          fontSize: 12,
                          color: liked
                              ? const Color(0xFF845143)
                              : const Color(0xFF524440),
                          fontWeight: liked
                              ? FontWeight.w600
                              : FontWeight.normal,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const Spacer(),
              if (!isOwnAnswer)
                PopupMenuButton<String>(
                  tooltip: 'Báo cáo',
                  icon: const Icon(
                    Icons.flag_outlined,
                    size: 18,
                    color: Color(0xFF524440),
                  ),
                  onSelected: (value) {
                    if (value == 'answer') onReport();
                    if (value == 'author' && onReportAuthor != null) {
                      onReportAuthor!();
                    }
                  },
                  itemBuilder: (ctx) => [
                    const PopupMenuItem(
                      value: 'answer',
                      child: Text('Báo cáo câu trả lời'),
                    ),
                    if (onReportAuthor != null)
                      const PopupMenuItem(
                        value: 'author',
                        child: Text('Báo cáo tài khoản'),
                      ),
                  ],
                ),
            ],
          ),
        ],
      ),
    );
  }
}
