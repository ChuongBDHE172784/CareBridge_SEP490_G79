import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/constants/content_stages.dart';
import '../models/content_model.dart';
import '../services/content_service.dart';
import '../widgets/verified_content_body.dart';

/// CB-181 — View Verified Content Detail (UC-225)
/// Displays the full body of a curated article or FAQ answer.
/// Reachable from ViewContentScreen (UC-82).
class VerifiedContentDetailScreen extends StatefulWidget {
  final String contentId;
  final ContentBrowseMode mode;
  final ContentService? contentService;

  const VerifiedContentDetailScreen({
    super.key,
    required this.contentId,
    this.mode = ContentBrowseMode.generic,
    this.contentService,
  });

  @override
  State<VerifiedContentDetailScreen> createState() =>
      _VerifiedContentDetailScreenState();
}

class _VerifiedContentDetailScreenState
    extends State<VerifiedContentDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outline = Color(0xFF84736F);

  ContentDetail? _content;
  String? _resolvedStage;
  bool _loading = true;
  String? _error;
  int _loadGeneration = 0;
  String? _observedAccountId;

  late ContentService _contentService;

  @override
  void initState() {
    super.initState();
    _contentService = widget.contentService ?? ContentService.instance;
    _observedAccountId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    _load();
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_onAccountChanged);
    super.dispose();
  }

  void _onAccountChanged() {
    final accountId = AuthState.instance.userId;
    if (accountId == _observedAccountId) return;
    _observedAccountId = accountId;
    _load();
  }

  @override
  void didUpdateWidget(covariant VerifiedContentDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.contentId != widget.contentId ||
        oldWidget.mode != widget.mode ||
        oldWidget.contentService != widget.contentService) {
      _contentService = widget.contentService ?? ContentService.instance;
      _load();
    }
  }

  Future<void> _load() async {
    final requestGeneration = ++_loadGeneration;
    final accountId = AuthState.instance.userId;
    setState(() {
      _loading = true;
      _error = null;
      _content = null;
      _resolvedStage = null;
    });
    try {
      late final ContentDetail content;
      String? resolvedStage;
      if (widget.mode == ContentBrowseMode.lifecycle) {
        final envelope = await _contentService.getLifecycleContentDetail(
          widget.contentId,
        );
        content = envelope.payload;
        resolvedStage = envelope.stage;
        if (envelope.stage != content.stage) {
          throw const FormatException('Lifecycle detail stage mismatch');
        }
      } else {
        content = await _contentService.getContentDetail(widget.contentId);
      }
      if (_canApply(requestGeneration, accountId)) {
        setState(() {
          _content = content;
          _resolvedStage = resolvedStage;
          _loading = false;
        });
      }
    } catch (_) {
      if (_canApply(requestGeneration, accountId)) {
        setState(() {
          _content = null;
          _resolvedStage = null;
          _error = widget.mode == ContentBrowseMode.lifecycle
              ? 'Nội dung này không còn phù hợp với giai đoạn hiện tại. Vui lòng thử lại.'
              : 'Không tải được nội dung. Vui lòng thử lại.';
          _loading = false;
        });
      }
    }
  }

  bool _canApply(int requestGeneration, String? accountId) =>
      mounted &&
      requestGeneration == _loadGeneration &&
      AuthState.instance.userId == accountId;

  String _stageLabel(String stage) => contentStageLabel(stage);

  String _typeLabel(String type) {
    switch (type) {
      case 'FAQ':
        return 'Câu hỏi thường gặp';
      case 'CHECKLIST':
        return 'Checklist';
      default:
        return 'Bài viết';
    }
  }

  String _formatDate(String? iso) {
    if (iso == null) return '';
    try {
      final dt = DateTime.parse(iso);
      return '${dt.day}/${dt.month}/${dt.year}';
    } catch (_) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          _content != null ? _typeLabel(_content!.type) : 'Nội dung',
          style: const TextStyle(
            color: _primary,
            fontWeight: FontWeight.bold,
            fontSize: 17,
          ),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _error != null || _content == null
          ? _buildError()
          : _buildContent(_content!),
    );
  }

  Widget _buildError() {
    return Center(
      key: const Key('lifecycle-content-detail-error'),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: _outline, size: 48),
          const SizedBox(height: 12),
          Text(
            _error ?? 'Không tìm thấy nội dung',
            style: const TextStyle(color: _onSurfaceVariant),
          ),
          const SizedBox(height: 16),
          Semantics(
            button: true,
            label: 'Thử tải lại nội dung',
            child: ElevatedButton(
              key: const Key('lifecycle-content-detail-retry'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(48, 48),
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
              ),
              onPressed: _load,
              child: const Text(
                'Thử lại',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContent(ContentDetail content) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(32),
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
            // Verified badge + stage tag
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: _primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.verified, size: 14, color: _primary),
                      SizedBox(width: 4),
                      Text(
                        'Nguồn tin cậy',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          color: _primary,
                        ),
                      ),
                    ],
                  ),
                ),
                if (content.stage.isNotEmpty)
                  Container(
                    key: widget.mode == ContentBrowseMode.lifecycle
                        ? const Key('lifecycle-content-detail-stage')
                        : null,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: _surfaceContainerLow,
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: Text(
                      _stageLabel(_resolvedStage ?? content.stage),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            // Title
            Text(
              content.title,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _onSurface,
                height: 1.3,
              ),
            ),
            if (content.publishedAt != null) ...[
              const SizedBox(height: 6),
              Text(
                'Xuất bản ${_formatDate(content.publishedAt)} • Phiên bản ${content.version}',
                style: const TextStyle(fontSize: 12, color: _outline),
              ),
            ],
            const SizedBox(height: 16),
            const Divider(height: 1, color: Color(0xFFD6C2BD)),
            const SizedBox(height: 16),
            // Rich text is sanitized by the backend before this renderer receives it.
            VerifiedContentBody(html: content.body, color: _onSurface),
            if ((content.sources != null && content.sources!.isNotEmpty) ||
                (content.sourceLabel != null && content.sourceLabel!.isNotEmpty)) ...[
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFEADBCE)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.verified_outlined, size: 18, color: _primary),
                        SizedBox(width: 6),
                        Text(
                          'NGUỒN THAM KHẢO / KIỂM DUYỆT',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: _primary,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    if (content.sources != null && content.sources!.isNotEmpty)
                      ...content.sources!.map((src) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (src.title.isNotEmpty)
                                Row(
                                  children: [
                                    const Icon(Icons.person_outline, size: 16, color: _outline),
                                    const SizedBox(width: 4),
                                    Expanded(
                                      child: Text(
                                        src.title,
                                        style: const TextStyle(
                                          fontSize: 13,
                                          fontWeight: FontWeight.w600,
                                          color: _onSurface,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              if (src.publisher != null && src.publisher!.isNotEmpty) ...[
                                const SizedBox(height: 2),
                                Row(
                                  children: [
                                    const Icon(Icons.domain_outlined, size: 16, color: _outline),
                                    const SizedBox(width: 4),
                                    Expanded(
                                      child: Text(
                                        'Đơn vị xuất bản: ${src.publisher}',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: _onSurfaceVariant,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                              if (src.url != null && src.url!.isNotEmpty) ...[
                                const SizedBox(height: 4),
                                InkWell(
                                  onTap: () async {
                                    final rawUrl = src.url!;
                                    final urlString = rawUrl.startsWith('http')
                                        ? rawUrl
                                        : 'https://$rawUrl';
                                    final uri = Uri.tryParse(urlString);
                                    if (uri != null) {
                                      await launchUrl(uri, mode: LaunchMode.externalApplication);
                                    }
                                  },
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Icon(Icons.link_outlined, size: 16, color: _primary),
                                      const SizedBox(width: 4),
                                      Expanded(
                                        child: Text(
                                          'Bài viết gốc tại: ${src.url}',
                                          style: const TextStyle(
                                            fontSize: 12,
                                            fontWeight: FontWeight.w600,
                                            color: _primary,
                                            decoration: TextDecoration.underline,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ],
                          ),
                        );
                      })
                    else if (content.sourceLabel != null && content.sourceLabel!.isNotEmpty)
                      Row(
                        children: [
                          const Icon(Icons.menu_book_outlined, size: 16, color: _outline),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              'Nguồn: ${content.sourceLabel}',
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w500,
                                color: _onSurface,
                              ),
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 20),
            // Disclaimer
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFFFBE6),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 20, color: Color(0xFFB89A00)),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Nội dung mang tính chất tham khảo. Nếu dấu hiệu nặng lên, hãy liên hệ '
                      'cơ sở y tế phù hợp ngay lập tức.',
                      style: TextStyle(
                        fontSize: 16,
                        color: Color(0xFF6D5B00),
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
