import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/expert_content_approval_model.dart';
import '../services/expert_content_approval_service.dart';

class ExpertContentReviewScreen extends StatefulWidget {
  final String contentId;
  final ExpertContentApprovalService? service;

  const ExpertContentReviewScreen({
    super.key,
    required this.contentId,
    this.service,
  });

  @override
  State<ExpertContentReviewScreen> createState() =>
      _ExpertContentReviewScreenState();
}

class _ExpertContentReviewScreenState extends State<ExpertContentReviewScreen> {
  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFFCF9);
  static const _background = Color(0xFFFBF8F5);
  static const _outlineVariant = Color(0xFFE5D3CA);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);

  late final ExpertContentApprovalService _service;

  bool _loading = true;
  String? _error;
  ContentDetailModel? _detail;
  bool _submittingDecision = false;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? ExpertContentApprovalService.instance;
    _loadDetail();
  }

  Future<void> _loadDetail() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final res = await _service.fetchContentDetail(widget.contentId);
      if (!mounted) return;
      setState(() {
        _detail = res;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể tải chi tiết nội dung: $e';
        _loading = false;
      });
    }
  }

  Future<void> _handleDecision(String decision) async {
    if (_detail == null) return;

    if (decision == 'APPROVE') {
      final confirm = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Row(
            children: [
              Icon(Icons.check_circle_outline, color: Color(0xFF137333)),
              SizedBox(width: 8),
              Text(
                'Phê duyệt nội dung',
                style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold),
              ),
            ],
          ),
          content: Text(
            'Xác nhận phê duyệt và xuất bản "${_detail!.title}"?\nNội dung sẽ được hiển thị cho người dùng.',
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 14),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF137333),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              child: const Text('Phê duyệt', style: TextStyle(fontFamily: 'Lexend')),
            ),
          ],
        ),
      );

      if (confirm == true) {
        _executeDecision('APPROVE', null);
      }
    } else {
      // REJECT
      final reasonController = TextEditingController();
      String? reasonError;

      final reason = await showDialog<String>(
        context: context,
        builder: (ctx) => StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            title: const Row(
              children: [
                Icon(Icons.assignment_return_outlined, color: Colors.deepOrange),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Yêu cầu chỉnh sửa',
                    style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
              ],
            ),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Nhập góp ý chuyên môn để tác giả hoàn thiện bài viết:',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: reasonController,
                    maxLines: 4,
                    decoration: InputDecoration(
                      hintText: 'Nhập lý do cần chỉnh sửa (bắt buộc)...',
                      hintStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                      errorText: reasonError,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                        borderSide: const BorderSide(color: _primary, width: 1.5),
                      ),
                    ),
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 14),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, null),
                child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
              ),
              ElevatedButton(
                onPressed: () {
                  final text = reasonController.text.trim();
                  if (text.isEmpty) {
                    setDialogState(() {
                      reasonError = 'Vui lòng nhập lý do từ chối / góp ý.';
                    });
                    return;
                  }
                  Navigator.pop(ctx, text);
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.deepOrange,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: const Text('Gửi yêu cầu', style: TextStyle(fontFamily: 'Lexend')),
              ),
            ],
          ),
        ),
      );

      if (reason != null && reason.isNotEmpty) {
        _executeDecision('REJECT', reason);
      }
    }
  }

  Future<void> _executeDecision(String decision, String? reason) async {
    setState(() => _submittingDecision = true);
    try {
      await _service.decideContent(widget.contentId, decision, reason: reason);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            decision == 'APPROVE'
                ? 'Đã phê duyệt và xuất bản nội dung thành công!'
                : 'Đã gửi yêu cầu chỉnh sửa cho tác giả!',
            style: const TextStyle(fontFamily: 'Lexend'),
          ),
          backgroundColor: decision == 'APPROVE' ? const Color(0xFF137333) : Colors.deepOrange,
        ),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      setState(() => _submittingDecision = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Lỗi khi gửi quyết định: $e', style: const TextStyle(fontFamily: 'Lexend')),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        title: const Text(
          'Chi tiết thẩm định',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: _onSurface,
          ),
        ),
        backgroundColor: _surface,
        elevation: 0.5,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _error != null
              ? _buildErrorView()
              : _detail == null
                  ? const Center(child: Text('Không tìm thấy nội dung'))
                  : _buildContentView(),
      bottomNavigationBar: _detail != null ? _buildBottomActions() : null,
    );
  }

  Widget _buildContentView() {
    final detail = _detail!;
    final dateStr = detail.createdAt != null
        ? DateFormat('dd/MM/yyyy HH:mm').format(detail.createdAt!)
        : '—';

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Previous feedback alert if rejected previously
          if (detail.latestReviewFeedback != null &&
              detail.latestReviewFeedback!.reason.isNotEmpty) ...[
            Container(
              padding: const EdgeInsets.all(14),
              margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(
                color: Colors.amber.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.amber.shade300),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, color: Colors.amber.shade900, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Góp ý từ đợt thẩm định trước:',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                            color: Colors.amber.shade900,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          detail.latestReviewFeedback!.reason,
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            color: Colors.amber.shade900,
                            height: 1.3,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
          // Header Card
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: _outlineVariant),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Tags row
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: detail.type == 'FAQ' ? Colors.purple.shade50 : Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        getApprovalTypeLabel(detail.type),
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: detail.type == 'FAQ' ? Colors.purple.shade700 : Colors.blue.shade700,
                        ),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1E9E4),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        getApprovalStageLabel(detail.stage),
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.amber.shade50,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: Colors.amber.shade300),
                      ),
                      child: Text(
                        getApprovalStatusLabel(detail.status),
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: Colors.amber.shade900,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                // Title
                Text(
                  detail.title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: _onSurface,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 8),
                // Metadata
                Row(
                  children: [
                    const Icon(Icons.calendar_today, size: 13, color: _onSurfaceVariant),
                    const SizedBox(width: 4),
                    Text(
                      'Tạo lúc: $dateStr',
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant),
                    ),
                    if (detail.sourceLabel != null) ...[
                      const SizedBox(width: 12),
                      const Icon(Icons.source, size: 13, color: _onSurfaceVariant),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          detail.sourceLabel!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant),
                        ),
                      ),
                    ],
                  ],
                ),
                if (detail.summary != null && detail.summary!.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: _background,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      detail.summary!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontStyle: FontStyle.italic,
                        color: _onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Body content card
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: _outlineVariant),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Nội dung chi tiết',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: _onSurface,
                  ),
                ),
                const Divider(height: 20, color: _outlineVariant),
                SelectableText(
                  _cleanHtmlTags(detail.body),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    height: 1.6,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _buildBottomActions() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: _surface,
        border: const Border(top: BorderSide(color: _outlineVariant)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: _submittingDecision ? null : () => _handleDecision('REJECT'),
                icon: const Icon(Icons.assignment_return_outlined, size: 18, color: Colors.deepOrange),
                label: const Text(
                  'Yêu cầu sửa',
                  style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold, color: Colors.deepOrange),
                ),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  side: const BorderSide(color: Colors.deepOrange, width: 1.5),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton.icon(
                onPressed: _submittingDecision ? null : () => _handleDecision('APPROVE'),
                icon: _submittingDecision
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                      )
                    : const Icon(Icons.check_circle_outline, size: 18),
                label: const Text(
                  'Phê duyệt',
                  style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF137333),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 12),
            Text(
              _error ?? 'Lỗi',
              textAlign: TextAlign.center,
              style: const TextStyle(fontFamily: 'Lexend', color: Colors.red),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadDetail,
              style: ElevatedButton.styleFrom(backgroundColor: _primary, foregroundColor: Colors.white),
              child: const Text('Thử lại', style: TextStyle(fontFamily: 'Lexend')),
            ),
          ],
        ),
      ),
    );
  }

  String _cleanHtmlTags(String htmlString) {
    // Strips basic html tags to render clean legible plain text
    final exp = RegExp(r'<[^>]*>', multiLine: true, caseSensitive: true);
    return htmlString
        .replaceAll('<br>', '\n')
        .replaceAll('<br/>', '\n')
        .replaceAll('<br />', '\n')
        .replaceAll('</p>', '\n\n')
        .replaceAll('</li>', '\n')
        .replaceAll(exp, '')
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&amp;', '&')
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .trim();
  }
}
