import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/expert_content_approval_model.dart';
import '../services/expert_content_approval_service.dart';
import 'expert_checklist_review_screen.dart';
import 'expert_content_review_screen.dart';

class ExpertContentApprovalQueueScreen extends StatefulWidget {
  final ExpertContentApprovalService? service;

  const ExpertContentApprovalQueueScreen({super.key, this.service});

  @override
  State<ExpertContentApprovalQueueScreen> createState() =>
      _ExpertContentApprovalQueueScreenState();
}

class _ExpertContentApprovalQueueScreenState
    extends State<ExpertContentApprovalQueueScreen> {
  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFFCF9);
  static const _background = Color(0xFFFBF8F5);
  static const _outlineVariant = Color(0xFFE5D3CA);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);

  late final ExpertContentApprovalService _service;

  bool _loading = false;
  String? _error;
  List<ExpertApprovalQueueItem> _items = [];
  int _totalElements = 0;
  int _page = 0;
  final int _pageSize = 20;

  // Filter states
  String _selectedType = 'ALL';
  String _selectedStage = 'ALL';
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? ExpertContentApprovalService.instance;
    _loadQueue();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadQueue({bool resetPage = false}) async {
    if (resetPage) _page = 0;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final res = await _service.fetchQueue(
        type: _selectedType,
        stage: _selectedStage,
        keyword: _searchController.text.trim(),
        page: _page,
        size: _pageSize,
      );
      if (!mounted) return;
      setState(() {
        _items = res.content;
        _totalElements = res.totalElements;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể tải danh sách thẩm định: $e';
        _loading = false;
      });
    }
  }

  void _openDetail(ExpertApprovalQueueItem item) async {
    final bool isChecklist = item.kind == 'CHECKLIST' || item.type == 'CHECKLIST';
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => isChecklist
            ? ExpertChecklistReviewScreen(checklistId: item.id)
            : ExpertContentReviewScreen(contentId: item.id),
      ),
    );

    if (result == true) {
      _loadQueue();
    }
  }

  Future<void> _handleQuickDecision(
    ExpertApprovalQueueItem item,
    String decision,
  ) async {
    if (decision == 'APPROVE') {
      final confirm = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Row(
            children: [
              Icon(Icons.check_circle_outline, color: Colors.green),
              SizedBox(width: 8),
              Text(
                'Phê duyệt nội dung',
                style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold),
              ),
            ],
          ),
          content: Text(
            'Bạn có chắc chắn muốn phê duyệt và xuất bản "${item.title}" không?',
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
        _submitDecision(item, 'APPROVE', null);
      }
    } else {
      // REJECT
      final reasonController = TextEditingController();
      String? reasonError;

      final submitReason = await showDialog<String>(
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
                  Text(
                    'Góp ý cho tác giả về "${item.title}":',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant),
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

      if (submitReason != null && submitReason.isNotEmpty) {
        _submitDecision(item, 'REJECT', submitReason);
      }
    }
  }

  Future<void> _submitDecision(
    ExpertApprovalQueueItem item,
    String decision,
    String? reason,
  ) async {
    setState(() => _loading = true);
    try {
      if (item.kind == 'CHECKLIST' || item.type == 'CHECKLIST') {
        await _service.decideChecklist(item.id, decision, reason: reason);
      } else {
        await _service.decideContent(item.id, decision, reason: reason);
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            decision == 'APPROVE'
                ? 'Đã phê duyệt thành công "${item.title}"'
                : 'Đã gửi yêu cầu chỉnh sửa cho "${item.title}"',
            style: const TextStyle(fontFamily: 'Lexend'),
          ),
          backgroundColor: decision == 'APPROVE' ? const Color(0xFF137333) : Colors.deepOrange,
        ),
      );
      _loadQueue();
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Thao tác thất bại: $e', style: const TextStyle(fontFamily: 'Lexend')),
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
        title: Text(
          _totalElements > 0
              ? 'Thẩm định nội dung ($_totalElements)'
              : 'Thẩm định nội dung',
          style: const TextStyle(
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
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: _primary),
            tooltip: 'Làm mới',
            onPressed: () => _loadQueue(resetPage: true),
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter section
          _buildFilterBar(),
          // Content
          Expanded(
            child: _loading && _items.isEmpty
                ? const Center(child: CircularProgressIndicator(color: _primary))
                : _error != null
                    ? _buildErrorView()
                    : _items.isEmpty
                        ? _buildEmptyView()
                        : RefreshIndicator(
                            color: _primary,
                            onRefresh: () => _loadQueue(),
                            child: ListView.separated(
                              padding: const EdgeInsets.all(16),
                              itemCount: _items.length,
                              separatorBuilder: (_, _) => const SizedBox(height: 12),
                              itemBuilder: (context, index) {
                                return _buildQueueCard(_items[index]);
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterBar() {
    return Container(
      color: _surface,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Column(
        children: [
          // Search input
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: 'Tìm kiếm theo tiêu đề...',
                    hintStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant),
                    prefixIcon: const Icon(Icons.search, size: 20, color: _onSurfaceVariant),
                    suffixIcon: _searchController.text.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear, size: 18),
                            onPressed: () {
                              _searchController.clear();
                              _loadQueue(resetPage: true);
                            },
                          )
                        : null,
                    contentPadding: const EdgeInsets.symmetric(vertical: 0, horizontal: 12),
                    filled: true,
                    fillColor: _background,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  onSubmitted: (_) => _loadQueue(resetPage: true),
                ),
              ),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: () => _loadQueue(resetPage: true),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
                child: const Text('Lọc', style: TextStyle(fontFamily: 'Lexend', fontSize: 13)),
              ),
            ],
          ),
          const SizedBox(height: 10),
          // Horizontal Filter Chips for Type
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _buildFilterChip('Tất cả loại', 'ALL', _selectedType, (val) {
                  setState(() => _selectedType = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Bài viết', 'ARTICLE', _selectedType, (val) {
                  setState(() => _selectedType = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Hỏi đáp (FAQ)', 'FAQ', _selectedType, (val) {
                  setState(() => _selectedType = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Checklist', 'CHECKLIST', _selectedType, (val) {
                  setState(() => _selectedType = val);
                  _loadQueue(resetPage: true);
                }),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // Horizontal Filter Chips for Stage
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _buildFilterChip('Mọi giai đoạn', 'ALL', _selectedStage, (val) {
                  setState(() => _selectedStage = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Mang thai', 'PREGNANCY', _selectedStage, (val) {
                  setState(() => _selectedStage = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Sau sinh', 'POSTPARTUM', _selectedStage, (val) {
                  setState(() => _selectedStage = val);
                  _loadQueue(resetPage: true);
                }),
                const SizedBox(width: 6),
                _buildFilterChip('Chăm sóc bé', 'BABY_CARE', _selectedStage, (val) {
                  setState(() => _selectedStage = val);
                  _loadQueue(resetPage: true);
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChip(
    String label,
    String value,
    String currentValue,
    ValueChanged<String> onSelected,
  ) {
    final isSelected = value == currentValue;
    return InkWell(
      onTap: () => onSelected(value),
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: isSelected ? _primary : _surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected ? _primary : _outlineVariant,
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            color: isSelected ? Colors.white : _onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  Widget _buildQueueCard(ExpertApprovalQueueItem item) {
    final dateStr = item.assignedAt != null
        ? DateFormat('dd/MM/yyyy HH:mm').format(item.assignedAt!)
        : '—';

    Color typeColor = Colors.blue.shade700;
    Color typeBg = Colors.blue.shade50;
    if (item.type == 'FAQ') {
      typeColor = Colors.purple.shade700;
      typeBg = Colors.purple.shade50;
    } else if (item.kind == 'CHECKLIST' || item.type == 'CHECKLIST') {
      typeColor = const Color(0xFF845143);
      typeBg = const Color(0xFFFBF2EF);
    }

    return Card(
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: _outlineVariant),
      ),
      color: _surface,
      child: InkWell(
        onTap: () => _openDetail(item),
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Badges
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: typeBg,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      getApprovalTypeLabel(item.type),
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: typeColor,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF1E9E4),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      getApprovalStageLabel(item.stage),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                  const Spacer(),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: Colors.amber.shade50,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: Colors.amber.shade300),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.hourglass_top, size: 12, color: Colors.amber.shade800),
                        const SizedBox(width: 4),
                        Text(
                          'Chờ duyệt',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: Colors.amber.shade900,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              // Title
              Text(
                item.title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: _onSurface,
                  height: 1.3,
                ),
              ),
              if (item.summary != null && item.summary!.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(
                  item.summary!,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                    height: 1.3,
                  ),
                ),
              ],
              const SizedBox(height: 10),
              // Meta info
              Row(
                children: [
                  const Icon(Icons.calendar_today, size: 13, color: _onSurfaceVariant),
                  const SizedBox(width: 4),
                  Text(
                    'Giao lúc: $dateStr',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  if (item.itemCount != null) ...[
                    const SizedBox(width: 12),
                    const Icon(Icons.format_list_bulleted, size: 13, color: _onSurfaceVariant),
                    const SizedBox(width: 4),
                    Text(
                      '${item.itemCount} mục',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ],
              ),
              const Divider(height: 20, color: _outlineVariant),
              // Card Actions
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  OutlinedButton.icon(
                    onPressed: () => _handleQuickDecision(item, 'REJECT'),
                    icon: const Icon(Icons.assignment_return_outlined, size: 15, color: Colors.deepOrange),
                    label: const Text(
                      'Yêu cầu sửa',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Colors.deepOrange),
                    ),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      side: const BorderSide(color: Colors.deepOrange),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                  const SizedBox(width: 8),
                  ElevatedButton.icon(
                    onPressed: () => _handleQuickDecision(item, 'APPROVE'),
                    icon: const Icon(Icons.check, size: 15),
                    label: const Text('Phê duyệt', style: TextStyle(fontFamily: 'Lexend', fontSize: 12)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF137333),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyView() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.assignment_turned_in_outlined, size: 54, color: Colors.grey.shade400),
          const SizedBox(height: 12),
          const Text(
            'Không có nội dung nào chờ thẩm định',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Tất cả bài viết và checklist đã được thẩm định xong.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: () => _loadQueue(resetPage: true),
            icon: const Icon(Icons.refresh, size: 16),
            label: const Text('Làm mới', style: TextStyle(fontFamily: 'Lexend')),
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
            ),
          ),
        ],
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
              _error ?? 'Lỗi không xác định',
              textAlign: TextAlign.center,
              style: const TextStyle(fontFamily: 'Lexend', color: Colors.red),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => _loadQueue(),
              style: ElevatedButton.styleFrom(backgroundColor: _primary, foregroundColor: Colors.white),
              child: const Text('Thử lại', style: TextStyle(fontFamily: 'Lexend')),
            ),
          ],
        ),
      ),
    );
  }
}
