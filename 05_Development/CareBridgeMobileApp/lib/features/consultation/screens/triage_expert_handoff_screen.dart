import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../directChat/models/expert_directory_item.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../services/triage_expert_handoff_service.dart';
import '../widgets/triage_context_consent_sheet.dart';

class TriageExpertHandoffScreen extends StatefulWidget {
  final String intakeSessionId;
  final TriageExpertHandoffService? handoffService;
  final DirectChatService? directoryService;
  final String Function()? clientRequestIdFactory;
  final Duration directoryTimeout;

  const TriageExpertHandoffScreen({
    super.key,
    required this.intakeSessionId,
    this.handoffService,
    this.directoryService,
    this.clientRequestIdFactory,
    this.directoryTimeout = const Duration(
      seconds: int.fromEnvironment(
        'EXPERT_HANDOFF_TIMEOUT_SECONDS',
        defaultValue: 8,
      ),
    ),
  });

  @override
  State<TriageExpertHandoffScreen> createState() =>
      _TriageExpertHandoffScreenState();
}

class _TriageExpertHandoffScreenState extends State<TriageExpertHandoffScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _accent = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _secondaryText = Color(0xFF9C857C);
  static const _nestedSurface = Color(0xFFF2EAE4);

  late final TriageExpertHandoffService _handoffService;
  late final DirectChatService _directoryService;
  late String? _accountId;
  int _generation = 0;
  bool _loading = true;
  bool _submitting = false;
  bool _consentOpen = false;
  String? _error;
  String? _status;
  TriageExpertHandoffPreview? _preview;
  List<ExpertDirectoryItem> _experts = const [];
  ExpertDirectoryItem? _selectedExpert;
  String? _clientRequestId;

  @override
  void initState() {
    super.initState();
    _handoffService =
        widget.handoffService ?? TriageExpertHandoffService.instance;
    _directoryService = widget.directoryService ?? DirectChatService.instance;
    _accountId = AuthState.instance.userId;
    AuthState.instance.addListener(_handleAuthChanged);
    _load();
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAuthChanged);
    super.dispose();
  }

  void _handleAuthChanged() {
    final current = AuthState.instance.userId;
    if (current == _accountId) return;
    _accountId = current;
    _generation++;
    if (_consentOpen && mounted) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && Navigator.of(context).canPop()) {
          Navigator.of(context).pop(false);
        }
      });
    }
    if (!mounted) return;
    setState(() {
      _loading = false;
      _submitting = false;
      _consentOpen = false;
      _preview = null;
      _experts = const [];
      _selectedExpert = null;
      _clientRequestId = null;
      _status = null;
      _error =
          'Phiên đăng nhập đã thay đổi. Hãy quay lại kết quả YELLOW của tài khoản hiện tại.';
    });
  }

  bool _isCurrent(int generation, String? accountId) =>
      mounted &&
      generation == _generation &&
      accountId != null &&
      accountId == AuthState.instance.userId;

  Future<void> _load() async {
    final accountId = AuthState.instance.userId;
    final generation = ++_generation;
    if (accountId == null || accountId.isEmpty) {
      setState(() {
        _loading = false;
        _error = 'Vui lòng đăng nhập lại để mở hỗ trợ chuyên gia.';
      });
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _status = null;
      _preview = null;
      _experts = const [];
    });
    try {
      final previewFuture = _handoffService.getPreview(widget.intakeSessionId);
      final directoryFuture = _directoryService
          .getExpertDirectory(page: 0, size: 50)
          .timeout(widget.directoryTimeout);
      final loaded = await Future.wait<Object>([
        previewFuture,
        directoryFuture,
      ], eagerError: false);
      final preview = loaded[0] as TriageExpertHandoffPreview;
      final directory = loaded[1] as ExpertDirectoryPage;
      if (!_isCurrent(generation, accountId)) return;
      setState(() {
        _preview = preview;
        _experts = directory.experts
            .where((expert) => expert.isEligibleForTriageHandoff)
            .toList(growable: false);
      });
    } catch (_) {
      if (!_isCurrent(generation, accountId)) return;
      setState(() {
        _error =
            'Chưa thể tải hỗ trợ chuyên gia lúc này. Kết quả YELLOW vẫn được giữ an toàn; hãy thử lại.';
      });
    } finally {
      if (_isCurrent(generation, accountId)) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _selectExpert(ExpertDirectoryItem expert) async {
    if (_submitting || !expert.isEligibleForTriageHandoff) return;
    final preview = _preview;
    final accountId = AuthState.instance.userId;
    final generation = _generation;
    if (preview == null || accountId == null || accountId != _accountId) return;
    if (_selectedExpert?.expertProfileId != expert.expertProfileId) {
      _selectedExpert = expert;
      _clientRequestId =
          widget.clientRequestIdFactory?.call() ?? const Uuid().v4();
    }
    _consentOpen = true;
    final approved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: _text.withValues(alpha: 0.4),
      builder: (_) => TriageContextConsentSheet(
        preview: preview,
        expertDisplayName:
            expert.displayName ?? expert.professionalTitle ?? 'Chuyên gia',
      ),
    );
    _consentOpen = false;
    if (approved != true || !_isCurrent(generation, accountId)) return;
    await _create(expert, preview, generation, accountId);
  }

  Future<void> _create(
    ExpertDirectoryItem expert,
    TriageExpertHandoffPreview preview,
    int generation,
    String accountId,
  ) async {
    if (_submitting || !_isCurrent(generation, accountId)) return;
    final clientRequestId = _clientRequestId;
    if (clientRequestId == null) return;
    setState(() {
      _submitting = true;
      _status = 'Đang gửi yêu cầu sau khi bạn đã đồng ý...';
    });
    try {
      final result = await _handoffService.create(
        intakeSessionId: widget.intakeSessionId,
        clientRequestId: clientRequestId,
        expertProfileId: expert.expertProfileId,
        consentAccepted: true,
        consentPolicyVersion: preview.consentPolicyVersion,
      );
      if (!_isCurrent(generation, accountId)) return;
      setState(() {
        _status = result.replayed
            ? 'Yêu cầu đã được xác nhận trước đó.'
            : 'Đã chia sẻ ngữ cảnh tối thiểu và gửi yêu cầu.';
      });
      if (!mounted) return;
      context.push('/consultation-requests/${result.consultationRequestId}');
    } catch (error) {
      if (!_isCurrent(generation, accountId)) return;
      setState(() => _status = _safeCreateError(error));
    } finally {
      if (_isCurrent(generation, accountId)) {
        setState(() => _submitting = false);
      }
    }
  }

  String _safeCreateError(Object error) {
    if (error is ApiException && error.statusCode == 409) {
      return 'Chuyên gia hoặc nội dung đồng ý đã thay đổi. Không có dữ liệu nào được chia sẻ; hãy tải lại.';
    }
    return 'Chưa thể hoàn tất chia sẻ. Không có xác nhận thành công; bạn có thể thử lại với cùng yêu cầu.';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        title: const Text(
          'Chuyên gia cho kết quả YELLOW',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return Center(
        child: Semantics(
          label: 'Đang tải chuyên gia đã xác thực',
          child: const CircularProgressIndicator(color: _accent),
        ),
      );
    }
    if (_error != null) {
      return Center(
        child: Semantics(
          liveRegion: true,
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.shield_outlined, color: _accent, size: 40),
                const SizedBox(height: 16),
                Text(
                  _error!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: _text,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    height: 1.45,
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 48,
                  child: FilledButton.icon(
                    key: const Key('triage-handoff-retry'),
                    onPressed: AuthState.instance.userId == _accountId
                        ? _load
                        : null,
                    style: FilledButton.styleFrom(
                      backgroundColor: _accent,
                      shape: const StadiumBorder(),
                    ),
                    icon: const Icon(Icons.refresh),
                    label: const Text(
                      'Thử lại',
                      style: TextStyle(fontSize: 16),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }
    final preview = _preview;
    if (preview == null) return const SizedBox.shrink();
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
      children: [
        _PreviewCard(preview: preview),
        const SizedBox(height: 24),
        const Text(
          'Chọn chuyên gia đang đủ điều kiện',
          style: TextStyle(
            color: _text,
            fontSize: 20,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Danh sách chỉ hiển thị chuyên gia đã được xác thực và hiện có thể nhận yêu cầu mới.',
          style: TextStyle(
            color: _secondaryText,
            fontSize: 16,
            fontWeight: FontWeight.w600,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 16),
        if (_experts.isEmpty)
          const _EmptyExperts()
        else
          for (final expert in _experts) ...[
            _ExpertCard(
              expert: expert,
              submitting: _submitting,
              onSelect: () => _selectExpert(expert),
            ),
            const SizedBox(height: 12),
          ],
        if ((_status ?? '').isNotEmpty) ...[
          const SizedBox(height: 4),
          Semantics(
            liveRegion: true,
            container: true,
            child: Container(
              key: const Key('triage-handoff-status'),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _nestedSurface,
                borderRadius: BorderRadius.circular(20),
                border: const Border(
                  left: BorderSide(color: _accent, width: 4),
                ),
              ),
              child: Text(
                _status!,
                style: const TextStyle(
                  color: _text,
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  height: 1.4,
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _PreviewCard extends StatelessWidget {
  final TriageExpertHandoffPreview preview;
  const _PreviewCard({required this.preview});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: const Color(0xFFE8DDD6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 32,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.warning_amber_rounded, color: Color(0xFFC98C7B)),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Ngữ cảnh tối thiểu được bảo vệ',
                  style: TextStyle(
                    color: Color(0xFF5A463F),
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            '${preview.context.riskLevel} • ${preview.context.stage}',
            style: const TextStyle(
              color: Color(0xFF5A463F),
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            preview.context.riskSummary,
            style: const TextStyle(
              color: Color(0xFF5A463F),
              fontSize: 16,
              fontWeight: FontWeight.w600,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }
}

class _ExpertCard extends StatelessWidget {
  final ExpertDirectoryItem expert;
  final bool submitting;
  final VoidCallback onSelect;

  const _ExpertCard({
    required this.expert,
    required this.submitting,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    final displayName =
        expert.displayName ?? expert.professionalTitle ?? 'Chuyên gia';
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: const Color(0xFFE8DDD6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const CircleAvatar(
                radius: 24,
                backgroundColor: Color(0xFFF2EAE4),
                child: Icon(Icons.person, color: Color(0xFFC98C7B)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            displayName,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: Color(0xFF5A463F),
                              fontSize: 17,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        const Icon(
                          Icons.verified,
                          color: Color(0xFFC98C7B),
                          size: 20,
                          semanticLabel: 'Đã xác thực',
                        ),
                      ],
                    ),
                    if ((expert.specialty ?? '').isNotEmpty)
                      Text(
                        expert.specialty!,
                        style: const TextStyle(
                          color: Color(0xFF9C857C),
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: FilledButton(
              key: Key('triage-handoff-select-${expert.expertProfileId}'),
              onPressed: submitting ? null : onSelect,
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFC98C7B),
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Chọn và xem nội dung đồng ý',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyExperts extends StatelessWidget {
  const _EmptyExperts();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
      ),
      child: const Text(
        'Hiện chưa có chuyên gia đã xác thực và đủ điều kiện. Bạn vẫn có thể tiếp tục theo dõi hướng dẫn YELLOW hoặc quay lại sau.',
        style: TextStyle(
          color: Color(0xFF5A463F),
          fontSize: 16,
          fontWeight: FontWeight.w700,
          height: 1.45,
        ),
      ),
    );
  }
}
