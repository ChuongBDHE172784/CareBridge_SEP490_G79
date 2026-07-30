import 'package:flutter/material.dart';

import '../models/community_model.dart';
import '../services/community_service.dart';
import 'question_detail_screen.dart';

class MyQuestionsScreen extends StatefulWidget {
  const MyQuestionsScreen({super.key});

  @override
  State<MyQuestionsScreen> createState() => _MyQuestionsScreenState();
}

class _MyQuestionsScreenState extends State<MyQuestionsScreen> {
  static const _accent = Color(0xFFC98C7B);
  static const _background = Color(0xFFF6F1EC);
  static const _primaryText = Color(0xFF5A463F);
  static const _secondaryText = Color(0xFF74615A);

  final _scrollController = ScrollController();
  final _questions = <MyCommunityQuestion>[];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 180) {
      _loadMore();
    }
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _page = 0;
      _hasMore = true;
    });
    try {
      final page = await CommunityService.instance.getMyQuestions();
      if (!mounted) return;
      setState(() {
        _questions
          ..clear()
          ..addAll(page.where((item) => item.status != 'DELETED'));
        _page = 1;
        _hasMore = page.length >= 20;
      });
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể tải câu hỏi: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadMore() async {
    if (_loading || _loadingMore || !_hasMore) return;
    setState(() => _loadingMore = true);
    try {
      final page = await CommunityService.instance.getMyQuestions(page: _page);
      if (!mounted) return;
      setState(() {
        _questions.addAll(page.where((item) => item.status != 'DELETED'));
        _page++;
        _hasMore = page.length >= 20;
      });
    } finally {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  Future<void> _deleteQuestion(MyCommunityQuestion question) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xóa câu hỏi'),
        content: const Text(
          'Câu hỏi và toàn bộ hình ảnh đính kèm sẽ bị xóa khỏi Cloudinary. Bạn có chắc muốn tiếp tục?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await CommunityService.instance.deleteQuestion(question.id);
      if (!mounted) return;
      setState(() {
        _questions.removeWhere((item) => item.id == question.id);
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã xóa câu hỏi và hình ảnh đính kèm')),
      );
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể xóa câu hỏi: $error')),
        );
      }
    }
  }

  String _dateLabel(String value) {
    final date = DateTime.tryParse(value)?.toLocal();
    if (date == null) return '';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  ({String label, Color foreground, Color background}) _statusStyle(
    String status,
  ) {
    return switch (status) {
      'APPROVED' => (
        label: 'Đã duyệt',
        foreground: const Color(0xFF28633C),
        background: const Color(0xFFDDF4E4),
      ),
      'HIDDEN' => (
        label: 'Đã ẩn',
        foreground: const Color(0xFF7C4B00),
        background: const Color(0xFFFFE8B5),
      ),
      'LOCKED' => (
        label: 'Đã khóa',
        foreground: const Color(0xFF8B1A1A),
        background: const Color(0xFFFFDAD6),
      ),
      'DELETED' => (
        label: 'Đã xóa',
        foreground: const Color(0xFF6D6260),
        background: const Color(0xFFEAE1DE),
      ),
      _ => (
        label: 'Chờ duyệt',
        foreground: const Color(0xFF745600),
        background: const Color(0xFFFFEFB8),
      ),
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _primaryText,
        elevation: 0,
        scrolledUnderElevation: 0,
        title: const Text(
          'Câu hỏi của tôi',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _accent))
          : _questions.isEmpty
          ? _buildEmptyState()
          : RefreshIndicator(
              onRefresh: _reload,
              color: _accent,
              child: ListView.separated(
                controller: _scrollController,
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
                itemCount: _questions.length + (_loadingMore ? 1 : 0),
                separatorBuilder: (_, _) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  if (index == _questions.length) {
                    return const Padding(
                      padding: EdgeInsets.all(16),
                      child: Center(
                        child: CircularProgressIndicator(color: _accent),
                      ),
                    );
                  }
                  return _buildQuestionCard(_questions[index]);
                },
              ),
            ),
    );
  }

  Widget _buildEmptyState() {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 34,
              backgroundColor: Color(0x26C98C7B),
              child: Icon(Icons.article_outlined, color: _accent, size: 34),
            ),
            SizedBox(height: 18),
            Text(
              'Bạn chưa đặt câu hỏi nào',
              style: TextStyle(
                color: _primaryText,
                fontSize: 18,
                fontWeight: FontWeight.w800,
              ),
            ),
            SizedBox(height: 8),
            Text(
              'Các câu hỏi đã đăng sẽ xuất hiện tại đây để bạn theo dõi và quản lý.',
              textAlign: TextAlign.center,
              style: TextStyle(color: _secondaryText, height: 1.45),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuestionCard(MyCommunityQuestion question) {
    final status = _statusStyle(question.status);
    final canOpen =
        question.status == 'APPROVED' || question.status == 'PENDING';
    final canDelete =
        question.status != 'LOCKED' && question.status != 'DELETED';
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: canOpen
            ? () async {
                final changed = await Navigator.of(context).push<bool>(
                  MaterialPageRoute(
                    builder: (_) =>
                        QuestionDetailScreen(questionId: question.id),
                  ),
                );
                if (changed == true) _reload();
              }
            : null,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: status.background,
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: Text(
                      status.label,
                      style: TextStyle(
                        color: status.foreground,
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  const Spacer(),
                  Text(
                    _dateLabel(question.createdAt),
                    style: const TextStyle(color: _secondaryText, fontSize: 12),
                  ),
                  if (canDelete)
                    IconButton(
                      tooltip: 'Xóa câu hỏi',
                      onPressed: () => _deleteQuestion(question),
                      icon: const Icon(
                        Icons.delete_outline,
                        color: Color(0xFFBA1A1A),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                question.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: _primaryText,
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  height: 1.3,
                ),
              ),
              const SizedBox(height: 7),
              Text(
                question.body,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: _secondaryText, height: 1.4),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  const Icon(Icons.chat_bubble_outline, size: 17),
                  const SizedBox(width: 5),
                  Text('${question.answerCount} trả lời'),
                  const SizedBox(width: 16),
                  const Icon(Icons.favorite_border, size: 17),
                  const SizedBox(width: 5),
                  Text('${question.likeCount}'),
                  if (question.imageUrls.isNotEmpty) ...[
                    const SizedBox(width: 16),
                    const Icon(Icons.photo_outlined, size: 17),
                    const SizedBox(width: 5),
                    Text('${question.imageUrls.length} ảnh'),
                  ],
                  const Spacer(),
                  if (canOpen)
                    const Icon(
                      Icons.chevron_right_rounded,
                      color: _secondaryText,
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
