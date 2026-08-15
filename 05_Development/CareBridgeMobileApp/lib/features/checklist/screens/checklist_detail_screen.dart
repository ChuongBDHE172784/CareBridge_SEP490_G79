import 'package:flutter/material.dart';

import '../../../core/constants/content_stages.dart';
import '../../community/models/checklist_assignment_context.dart';
import '../../community/models/content_model.dart';
import '../services/user_checklist_service.dart';

/// CB-009 / CB-181 — Checklist Detail Screen
/// Displays full details of a Checklist Template including meta information,
/// stage, target subject, list of items, and action to add template tasks
/// to user's daily checklist.
class ChecklistDetailScreen extends StatefulWidget {
  const ChecklistDetailScreen({
    super.key,
    required this.template,
    this.importedItemIds = const {},
    this.journeyId,
    this.isLifecycleMode = false,
    this.userChecklistService,
  });

  final ChecklistTemplate template;
  final Set<String> importedItemIds;
  final String? journeyId;
  final bool isLifecycleMode;
  final UserChecklistService? userChecklistService;

  @override
  State<ChecklistDetailScreen> createState() => _ChecklistDetailScreenState();
}

class _ChecklistDetailScreenState extends State<ChecklistDetailScreen> {
  static const _background = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _primary = Color(0xFF845143);
  static const _onPrimary = Colors.white;
  static const _muted = Color(0xFF2A211D);
  static const _subtle = Color(0xFF655650);
  static const _error = Color(0xFFBA1A1A);
  static const _accentBadge = Color(0xFFF8EEE9);

  late UserChecklistService _userChecklistService;
  late Set<String> _importedIds;
  bool _isAdding = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _userChecklistService =
        widget.userChecklistService ?? UserChecklistService.instance;
    _importedIds = Set.from(widget.importedItemIds);
  }

  ChecklistAssignmentContext get _assignmentContext {
    final rawJourneyId = widget.journeyId;
    final normalizedJourneyId = rawJourneyId == null || rawJourneyId.isEmpty
        ? null
        : rawJourneyId;
    return ChecklistAssignmentContext.resolve(
      templateStage: widget.template.stage,
      journeyId: normalizedJourneyId,
      lifecycleMode: widget.isLifecycleMode,
    );
  }

  bool get _allAdded =>
      widget.template.items.isEmpty ||
      widget.template.items.every((item) => _importedIds.contains(item.id));

  int get _addedCount => widget.template.items
      .where((item) => _importedIds.contains(item.id))
      .length;

  bool get _hasBabyItems {
    final nameLower = widget.template.name.toLowerCase();
    return widget.template.stage == 'BABY_CARE' ||
        nameLower.contains('bé') ||
        nameLower.contains('trẻ') ||
        nameLower.contains('sơ sinh');
  }

  bool get _isTargetlessV2 => widget.template.checklistContractVersion == 2;

  String? get _windowLabel {
    final start = widget.template.eligibilityStartInclusive;
    final end = widget.template.eligibilityEndInclusive;
    if (start == null || end == null) return null;
    return end >= 2000000000 ? 'Tuần ${start + 1}+' : 'Tuần ${start + 1}–${end + 1}';
  }

  String? get _cadenceLabel {
    final type = widget.template.scheduleType;
    final policy = widget.template.materializationPolicy;
    if (type == 'WEEKLY' || policy == 'EACH_WEEK') return 'Theo tuần';
    if (type == 'DAILY' || policy == 'EACH_DAY') return 'Theo ngày';
    if (type == 'SET' || policy == 'ONCE_PER_WINDOW') return 'Theo bộ';
    return null;
  }

  Future<void> _handleAddTemplate() async {
    if (!_assignmentContext.canAssign || _isAdding || _allAdded) return;

    setState(() {
      _isAdding = true;
      _errorMessage = null;
    });

    try {
      await _userChecklistService.addTemplate(
        templateId: widget.template.id,
        journeyId: _assignmentContext.journeyId,
      );

      if (!mounted) return;

      setState(() {
        _isAdding = false;
        _importedIds.addAll(widget.template.items.map((e) => e.id));
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Đã thêm ${widget.template.items.length} việc vào danh sách công việc!',
          ),
          backgroundColor: _primary,
          behavior: SnackBarBehavior.floating,
        ),
      );
    } catch (e) {
      if (!mounted) return;

      setState(() {
        _isAdding = false;
        _errorMessage = 'Không thể thêm checklist: ${e.toString()}';
      });

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Lỗi: ${e.toString()}'),
          backgroundColor: _error,
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final template = widget.template;
    final assignmentContext = _assignmentContext;

    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _muted,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _primary),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết Checklist',
          style: TextStyle(
            fontFamily: 'Quicksand',
            fontWeight: FontWeight.w800,
            fontSize: 20,
            color: _muted,
          ),
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
                children: [
                  _buildHeaderCard(template),
                  const SizedBox(height: 20),
                  _buildProgressCard(template),
                  const SizedBox(height: 20),
                  _buildItemsSection(template),
                  if (_errorMessage != null) ...[
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: _error.withAlpha(20),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: _error.withAlpha(80)),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.error_outline_rounded,
                            color: _error,
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                color: _error,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  if (!assignmentContext.canAssign) ...[
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.amber.shade50,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: Colors.amber.shade300),
                      ),
                      child: Row(
                        children: const [
                          Icon(
                            Icons.warning_amber_rounded,
                            color: Colors.amber,
                            size: 20,
                          ),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Hãy thiết lập hành trình (Journey) trước khi thêm checklist.',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                color: Color(0xFF7A5400),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
            _buildBottomActionBar(assignmentContext),
          ],
        ),
      ),
    );
  }

  Widget _buildHeaderCard(ChecklistTemplate template) {
    return Container(
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(12),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: _accentBadge,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(
                  _hasBabyItems
                      ? Icons.child_care_rounded
                      : Icons.pregnant_woman_rounded,
                  color: _primary,
                  size: 28,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      template.name,
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: _muted,
                        height: 1.25,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 6,
                      children: [
                        _buildBadge(
                          contentStageLabel(template.stage),
                          icon: Icons.auto_awesome_rounded,
                        ),
                        if (template.templateType.isNotEmpty)
                          _buildBadge(
                            _isTargetlessV2
                                ? 'Nội dung khuyến nghị'
                                : template.templateType == 'MANDATORY'
                                ? 'Bắt buộc'
                                : 'Khuyến nghị',
                            isHighlight:
                                !_isTargetlessV2 &&
                                template.templateType == 'MANDATORY',
                          ),
                        if (template.planNumber != null)
                          _buildBadge(
                            'Plan ${template.planNumber} · ${template.section ?? 'chung'}',
                          ),
                        if (_windowLabel != null) _buildBadge(_windowLabel!),
                        if (_cadenceLabel != null) _buildBadge(_cadenceLabel!),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (template.description.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Divider(height: 1, color: Color(0xFFF2EAE4)),
            const SizedBox(height: 14),
            Text(
              template.description,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _subtle,
                height: 1.5,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildProgressCard(ChecklistTemplate template) {
    final total = template.items.length;
    final added = _addedCount;
    final progress = total > 0 ? added / total : 0.0;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _accentBadge,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Tiến độ quản lý',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: _muted,
                ),
              ),
              Text(
                '$added / $total mục đã chọn',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              backgroundColor: Colors.white,
              valueColor: const AlwaysStoppedAnimation<Color>(_primary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildItemsSection(ChecklistTemplate template) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 12),
          child: Text(
            'Các công việc cần chuẩn bị (${template.items.length})',
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _muted,
            ),
          ),
        ),
        Container(
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(8),
                blurRadius: 12,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: template.items.length,
            separatorBuilder: (_, _) =>
                const Divider(height: 1, indent: 56, endIndent: 16),
            itemBuilder: (context, index) {
              final item = template.items[index];
              final isImported = _importedIds.contains(item.id);
              return ListTile(
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 4,
                ),
                leading: Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: isImported
                        ? _primary.withAlpha(20)
                        : const Color(0xFFF6F1EC),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    isImported
                        ? Icons.check_rounded
                        : Icons.radio_button_unchecked_rounded,
                    color: isImported ? _primary : _subtle,
                    size: 18,
                  ),
                ),
                title: Text(
                  item.itemText,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: item.isRequired == true
                        ? FontWeight.w600
                        : FontWeight.w400,
                    color: isImported ? _subtle : _muted,
                    decoration: isImported ? TextDecoration.lineThrough : null,
                  ),
                ),
                trailing: item.isRequired == true
                    ? Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: _error.withAlpha(15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: const [
                            Icon(Icons.star_rounded, color: _error, size: 12),
                            SizedBox(width: 3),
                            Text(
                              'Cần thiết',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                                color: _error,
                              ),
                            ),
                          ],
                        ),
                      )
                    : null,
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildBadge(String label, {IconData? icon, bool isHighlight = false}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: isHighlight ? _error.withAlpha(15) : _accentBadge,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: isHighlight ? _error : _primary),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: isHighlight ? _error : _primary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomActionBar(ChecklistAssignmentContext assignmentContext) {
    final canClick = assignmentContext.canAssign && !_allAdded && !_isAdding;

    return Container(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 16),
      decoration: BoxDecoration(
        color: _surface,
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 16,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: SizedBox(
        width: double.infinity,
        height: 52,
        child: ElevatedButton.icon(
          onPressed: canClick ? _handleAddTemplate : null,
          icon: _isAdding
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    valueColor: AlwaysStoppedAnimation<Color>(_onPrimary),
                  ),
                )
              : Icon(
                  _allAdded
                      ? Icons.check_circle_rounded
                      : Icons.add_task_rounded,
                  color: _onPrimary,
                ),
          label: Text(
            _isAdding
                ? 'Đang thêm công việc...'
                : _allAdded
                ? 'Đã thêm tất cả vào việc cần làm'
                : 'Thêm vào danh sách việc cần làm',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onPrimary,
            ),
          ),
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            disabledBackgroundColor: _allAdded
                ? const Color(0xFF81A987)
                : const Color(0xFFD6C2BD),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            elevation: 0,
          ),
        ),
      ),
    );
  }
}
