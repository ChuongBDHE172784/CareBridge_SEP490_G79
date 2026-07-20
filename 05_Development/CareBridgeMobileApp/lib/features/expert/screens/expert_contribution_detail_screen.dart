import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:intl/intl.dart';

import '../models/contribution_model.dart';
import '../services/expert_contribution_service.dart';
import '../widgets/contribution_status_chip.dart';

class ExpertContributionDetailScreen extends StatefulWidget {
  const ExpertContributionDetailScreen({super.key, required this.contributionId});

  final String contributionId;

  @override
  State<ExpertContributionDetailScreen> createState() =>
      _ExpertContributionDetailScreenState();
}

class _ExpertContributionDetailScreenState
    extends State<ExpertContributionDetailScreen> {
  final _service = ExpertContributionService.instance;

  Contribution? _contribution;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadContribution();
  }

  Future<void> _loadContribution() async {
    setState(() => _loading = true);
    try {
      final c = await _service.getContribution(widget.contributionId);
      if (mounted) setState(() => _contribution = c);
    } catch (e) {
      if (mounted) setState(() => _error = 'Không thể tải bài viết: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submitForReview() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Gửi duyệt bài viết?'),
        content: const Text(
            'Sau khi gửi, bạn sẽ không thể chỉnh sửa cho đến khi được duyệt hoặc từ chối.'),
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
      await _service.submitContribution(widget.contributionId);
      if (mounted) {
        await _loadContribution();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi duyệt thành công')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Gửi duyệt thất bại: $e')),
        );
      }
    }
  }

  Future<void> _deleteContribution() async {
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
      await _service.deleteContribution(widget.contributionId);
      if (mounted) {
        context.pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Xóa thất bại: $e')),
        );
      }
    }
  }

  Future<void> _openAttachment(ContributionAttachment att) async {
    if (att.presignedUrl != null && att.presignedUrl!.isNotEmpty) {
      final uri = Uri.parse(att.presignedUrl!);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể mở liên kết')),
        );
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
        title: Text(_contribution?.title ?? 'Chi tiết bài viết'),
        backgroundColor: cs.surface,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildError(cs, theme)
              : _contribution == null
                  ? _buildNotFound(theme, cs)
                  : _buildDetail(theme, cs),
    );
  }

  Widget _buildError(ColorScheme cs, ThemeData theme) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text('Lỗi', style: theme.textTheme.headlineSmall),
            const SizedBox(height: 8),
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _loadContribution,
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNotFound(ThemeData theme, ColorScheme cs) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.article_outlined, size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text('Không tìm thấy bài viết',
                style: theme.textTheme.headlineSmall),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () => context.pop(),
              child: const Text('Quay về danh sách'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetail(ThemeData theme, ColorScheme cs) {
    final c = _contribution!;
    final status = ContributionStatus.fromString(c.status);
    final isDraft = c.status == 'DRAFT';

    return CustomScrollView(
      slivers: [
        // Header
        SliverToBoxAdapter(
          child: Container(
            color: cs.surface,
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        c.title,
                        style: theme.textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    ContributionStatusChip(status: status),
                  ],
                ),
                if (c.rejectionReason != null) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: cs.errorContainer,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.error_outline, size: 16, color: cs.onErrorContainer),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Lý do từ chối: ${c.rejectionReason}',
                            style: TextStyle(color: cs.onErrorContainer),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 4,
                  children: [
                    if (c.specialtyId != null)
                      _MetaChip(
                        icon: Icons.medical_services_outlined,
                        label: c.specialtyId!,
                      ),
                    if (c.hospitalId != null)
                      _MetaChip(
                        icon: Icons.local_hospital_outlined,
                        label: c.hospitalId!,
                      ),
                    _MetaChip(
                      icon: Icons.attach_file_outlined,
                      label: '${c.attachments?.length ?? 0} tệp đính kèm',
                    ),
                    _MetaChip(
                      icon: Icons.history_outlined,
                      label: 'Phiên bản #${c.version}',
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  'Tạo: ${_formatDate(c.createdAt)}  ·  Cập nhật: ${_formatDate(c.updatedAt)}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: cs.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),

        const SliverToBoxAdapter(child: Divider(height: 1)),

        // Content
        SliverToBoxAdapter(
          child: Container(
            color: cs.surface,
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Nội dung',
                    style: theme.textTheme.titleLarge
                        ?.copyWith(fontWeight: FontWeight.w600)),
                const SizedBox(height: 12),
                Text(
                  c.content,
                  style: theme.textTheme.bodyLarge?.copyWith(height: 1.6),
                ),
              ],
            ),
          ),
        ),

        const SliverToBoxAdapter(child: Divider(height: 1)),

        // Attachments
        if ((c.attachments?.length ?? 0) > 0)
          SliverToBoxAdapter(
            child: Container(
              color: cs.surface,
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Tệp đính kèm (${c.attachments!.length})',
                      style: theme.textTheme.titleLarge
                          ?.copyWith(fontWeight: FontWeight.w600)),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: c.attachments!.map((att) => _AttachmentCard(
                      attachment: att,
                      onTap: () => _openAttachment(att),
                    )).toList(),
                  ),
                ],
              ),
            ),
          ),

        const SliverToBoxAdapter(child: SizedBox(height: 100)),

        // Actions for draft
        if (isDraft)
          SliverToBoxAdapter(
            child: Container(
              color: cs.surface,
              padding: const EdgeInsets.all(16),
              child: SafeArea(
                top: false,
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  alignment: WrapAlignment.end,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () => context.push(
                          '/expert/contributions/${c.id}/edit'),
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      label: const Text('Chỉnh sửa'),
                    ),
                    OutlinedButton.icon(
                      onPressed: _deleteContribution,
                      icon: const Icon(Icons.delete_outlined, size: 18),
                      label: const Text('Xóa'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.red,
                        side: const BorderSide(color: Colors.red),
                      ),
                    ),
                    FilledButton.icon(
                      onPressed: _submitForReview,
                      icon: const Icon(Icons.send_outlined, size: 18),
                      label: const Text('Gửi duyệt'),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
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

class _MetaChip extends StatelessWidget {
  const _MetaChip({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: cs.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: cs.outlineVariant),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: cs.onSurfaceVariant),
          const SizedBox(width: 6),
          Text(label,
              style: TextStyle(
                fontSize: 12,
                color: cs.onSurfaceVariant,
              )),
        ],
      ),
    );
  }
}

class _AttachmentCard extends StatelessWidget {
  const _AttachmentCard({
    required this.attachment,
    required this.onTap,
  });

  final ContributionAttachment attachment;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    final isImage = attachment.kind == 'IMAGE';
    final hasUrl = attachment.presignedUrl != null && attachment.presignedUrl!.isNotEmpty;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        width: 120,
        height: 120,
        decoration: BoxDecoration(
          color: cs.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: cs.outlineVariant),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (isImage && hasUrl)
              ClipRRect(
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(12),
                ),
                child: Image.network(
                  attachment.presignedUrl!,
                  width: double.infinity,
                  height: 80,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => _buildDocIcon(cs),
                ),
              )
            else
              Expanded(child: _buildDocIcon(cs)),
            if (!isImage) const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
              child: Text(
                attachment.originalName ?? 'attachment',
                style: theme.textTheme.labelSmall,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
              ),
            ),
            if (!isImage && attachment.fileSizeBytes > 0)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(
                  '${(attachment.fileSizeBytes / 1024).toStringAsFixed(1)} KB',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: cs.onSurfaceVariant,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildDocIcon(ColorScheme cs) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Icon(
        Icons.description,
        size: 36,
        color: cs.primary,
      ),
    );
  }
}