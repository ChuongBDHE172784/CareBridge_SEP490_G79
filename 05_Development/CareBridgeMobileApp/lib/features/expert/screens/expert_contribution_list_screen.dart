import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../models/contribution_model.dart';
import '../services/expert_contribution_service.dart';
import '../widgets/contribution_status_chip.dart';

class ExpertContributionListScreen extends StatefulWidget {
  const ExpertContributionListScreen({super.key});

  @override
  State<ExpertContributionListScreen> createState() =>
      _ExpertContributionListScreenState();
}

class _ExpertContributionListScreenState
    extends State<ExpertContributionListScreen> {
  final _service = ExpertContributionService.instance;
  final _scrollController = ScrollController();

  List<Contribution> _contributions = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _eligible = false;
  int _currentPage = 0;
  int _totalPages = 0;
  int _totalElements = 0;
  static const _pageSize = 10;

  @override
  void initState() {
    super.initState();
    _loadInitial();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 200 &&
        !_loadingMore &&
        _currentPage < _totalPages - 1) {
      _loadMore();
    }
  }

  Future<void> _loadInitial() async {
    setState(() => _loading = true);
    try {
      await Future.wait([_loadEligibility(), _loadPage(0)]);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadEligibility() async {
    try {
      final eligible = await _service.checkEligibility();
      if (mounted) setState(() => _eligible = eligible);
    } catch (_) {
      if (mounted) setState(() => _eligible = false);
    }
  }

  Future<void> _loadPage(int page) async {
    try {
      final result = await _service.listMyContributions(
        page: page,
        size: _pageSize,
      );
      if (mounted) {
        setState(() {
          if (page == 0) {
            _contributions = result.content;
          } else {
            _contributions.addAll(result.content);
          }
          _currentPage = result.page;
          _totalPages = result.totalPages;
          _totalElements = result.totalElements;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tải danh sách: $e')));
      }
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _currentPage >= _totalPages - 1) return;
    setState(() => _loadingMore = true);
    await _loadPage(_currentPage + 1);
    if (mounted) setState(() => _loadingMore = false);
  }

  Future<void> _refresh() async {
    await _loadInitial();
  }

  Future<void> _deleteContribution(String id) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xóa bản nháp'),
        content: const Text('Xóa vĩnh viễn bản nháp này? Không thể hoàn tác.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _service.deleteContribution(id);
      if (mounted) {
        setState(() => _contributions.removeWhere((c) => c.id == id));
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Đã xóa bản nháp')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Xóa thất bại: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Scaffold(
      backgroundColor: cs.surfaceContainerHighest,
      appBar: AppBar(
        title: const Text('Đóng góp y khoa của tôi'),
        backgroundColor: cs.surface,
        elevation: 0,
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : CustomScrollView(
                controller: _scrollController,
                slivers: [
                  // Header with create button
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Tổng: $_totalElements bài viết',
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: cs.onSurfaceVariant,
                            ),
                          ),
                          if (_eligible)
                            FilledButton.icon(
                              onPressed: () =>
                                  context.push('/expert/contributions/new'),
                              icon: const Icon(Icons.add, size: 18),
                              label: const Text('Tạo mới'),
                            )
                          else
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 8,
                              ),
                              decoration: BoxDecoration(
                                color: Colors.amber.withValues(alpha: 0.1),
                                border: Border.all(
                                  color: Colors.amber.withValues(alpha: 0.3),
                                ),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.lock_outline,
                                    size: 16,
                                    color: Colors.amber,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    'Chưa đủ điều kiện',
                                    style: theme.textTheme.labelMedium
                                        ?.copyWith(color: Colors.amber[800]),
                                  ),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),

                  // Empty state
                  if (_contributions.isEmpty && !_loading)
                    SliverFillRemaining(
                      hasScrollBody: false,
                      child: Center(
                        child: Padding(
                          padding: const EdgeInsets.all(32),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.article_outlined,
                                size: 64,
                                color: cs.onSurfaceVariant.withValues(
                                  alpha: 0.5,
                                ),
                              ),
                              const SizedBox(height: 16),
                              Text(
                                _eligible
                                    ? 'Chưa có bài viết nào'
                                    : 'Hoàn tất xác minh chuyên gia để đóng góp',
                                style: theme.textTheme.titleMedium?.copyWith(
                                  color: cs.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                _eligible
                                    ? 'Bắt đầu chia sẻ kiến thức y khoa bằng cách tạo bài viết đầu tiên.'
                                    : 'Cần xác minh APPROVED và trust ACTIVE.',
                                textAlign: TextAlign.center,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: cs.onSurfaceVariant,
                                ),
                              ),
                              if (_eligible) ...[
                                const SizedBox(height: 20),
                                FilledButton.icon(
                                  onPressed: () =>
                                      context.push('/expert/contributions/new'),
                                  icon: const Icon(Icons.add),
                                  label: const Text('Tạo bài viết đầu tiên'),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ),

                  // List
                  if (_contributions.isNotEmpty)
                    SliverPadding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      sliver: SliverList.separated(
                        itemCount:
                            _contributions.length + (_loadingMore ? 1 : 0),
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          if (index >= _contributions.length) {
                            return const Center(
                              child: Padding(
                                padding: EdgeInsets.all(16),
                                child: CircularProgressIndicator(),
                              ),
                            );
                          }
                          final c = _contributions[index];
                          return _ContributionCard(
                            contribution: c,
                            onTap: () =>
                                context.push('/expert/contributions/${c.id}'),
                            onDelete: c.status == 'DRAFT'
                                ? () => _deleteContribution(c.id)
                                : null,
                            onSubmit: c.status == 'DRAFT'
                                ? () => _submitContribution(c.id)
                                : null,
                          );
                        },
                      ),
                    ),

                  // Pagination indicator
                  if (_totalPages > 1)
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Center(
                          child: Text(
                            'Trang ${_currentPage + 1} / $_totalPages · $_totalElements bài viết',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: cs.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ),
                    ),

                  const SliverToBoxAdapter(child: SizedBox(height: 80)),
                ],
              ),
      ),
    );
  }

  Future<void> _submitContribution(String id) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Gửi duyệt bài viết?'),
        content: const Text(
          'Sau khi gửi, bạn sẽ không thể chỉnh sửa cho đến khi được duyệt hoặc từ chối.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Gửi duyệt'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _service.submitContribution(id);
      if (mounted) {
        await _loadInitial();
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi duyệt thành công')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Gửi duyệt thất bại: $e')));
      }
    }
  }
}

class _ContributionCard extends StatelessWidget {
  const _ContributionCard({
    required this.contribution,
    required this.onTap,
    this.onDelete,
    this.onSubmit,
  });

  final Contribution contribution;
  final VoidCallback onTap;
  final VoidCallback? onDelete;
  final VoidCallback? onSubmit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    final status = ContributionStatus.fromString(contribution.status);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          contribution.title,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          contribution.content,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: cs.onSurfaceVariant,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          runSpacing: 4,
                          children: [
                            if (contribution.specialtyId != null)
                              _InfoChip(
                                icon: Icons.medical_services_outlined,
                                label: contribution.specialtyId!,
                              ),
                            if (contribution.hospitalId != null)
                              _InfoChip(
                                icon: Icons.local_hospital_outlined,
                                label: contribution.hospitalId!,
                              ),
                            _InfoChip(
                              icon: Icons.attach_file,
                              label:
                                  '${contribution.attachments?.length ?? 0} tệp',
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  ContributionStatusChip(status: status),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Cập nhật: ${_formatDate(contribution.updatedAt)}',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: cs.onSurfaceVariant,
                    ),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (onSubmit != null)
                        TextButton.icon(
                          onPressed: onSubmit,
                          icon: const Icon(Icons.send_outlined, size: 16),
                          label: const Text('Gửi duyệt'),
                        ),
                      if (onDelete != null)
                        TextButton.icon(
                          onPressed: onDelete,
                          icon: const Icon(
                            Icons.delete_outline,
                            size: 16,
                            color: Colors.red,
                          ),
                          label: Text(
                            'Xóa',
                            style: TextStyle(color: Colors.red),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatDate(String iso) {
    try {
      final dt = DateTime.parse(iso).toLocal();
      return DateFormat('dd/MM/yyyy HH:mm', 'vi').format(dt);
    } catch (_) {
      return iso;
    }
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: theme.colorScheme.onSurfaceVariant),
          const SizedBox(width: 4),
          Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
