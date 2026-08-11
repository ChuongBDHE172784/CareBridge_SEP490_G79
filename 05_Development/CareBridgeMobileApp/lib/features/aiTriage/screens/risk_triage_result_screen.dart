import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../models/triage_continuation.dart';
import '../models/triage_result_model.dart';
import '../services/triage_continuation_restore_coordinator.dart';
import '../services/triage_continuation_store.dart';
import '../services/triage_service.dart';
import '../../emergency/services/emergency_service.dart';

/// CB-016 — Risk Triage Result (UC-61)
/// Shows the AI risk classification (GREEN/YELLOW/RED) for a completed
/// intake session. Data: GET /api/v1/triage/intake/{sessionId}.
class RiskTriageResultScreen extends StatefulWidget {
  final String sessionId;
  final TriageService? triageService;
  final EmergencyService? emergencyService;
  final TriageContinuationStore? continuationStore;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;
  final Future<bool> Function()? postpartumEmergencyLauncher;

  const RiskTriageResultScreen({
    super.key,
    required this.sessionId,
    this.triageService,
    this.emergencyService,
    this.continuationStore,
    this.continuationCoordinator,
    this.postpartumEmergencyLauncher,
  });

  @override
  State<RiskTriageResultScreen> createState() => _RiskTriageResultScreenState();
}

class _RiskTriageResultScreenState extends State<RiskTriageResultScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFF8F5F1);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _surfaceContainerLowest = Color(0xFFFFFCF9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFF1E6E0);
  static const _tertiary = Color(0xFF625D59);
  static const _secondaryContainerAlt = Color(0xFFF8EEE9);
  static const _secondary = Color(0xFF6E5A52);

  late final TriageService _triageService;
  late final EmergencyService _emergencyService;
  late final TriageContinuationRestoreCoordinator _continuationCoordinator;
  TriageResult? _result;
  bool _loading = true;
  bool _openingEmergency = false;
  bool _dialing115 = false;
  bool _emergencyLoadFailed = false;
  bool _returningToOrigin = false;
  String? _emergencyNotice;
  String? _dialerNotice;
  String? _error;
  String? _returnNotice;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _triageService = widget.triageService ?? TriageService();
    _emergencyService = widget.emergencyService ?? EmergencyService();
    _continuationCoordinator =
        widget.continuationCoordinator ??
        TriageContinuationRestoreCoordinator(
          store: widget.continuationStore ?? SecureTriageContinuationStore(),
          gateway: _triageService,
        );
    _load();
  }

  Future<void> _returnToValidatedOrigin() async {
    if (_returningToOrigin) return;
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) {
      setState(() => _returnNotice = 'Vui lòng đăng nhập lại để tiếp tục.');
      return;
    }
    final router = GoRouter.of(context);
    setState(() {
      _returningToOrigin = true;
      _returnNotice = null;
    });
    TriageContinuationDecision decision;
    try {
      decision = await _continuationCoordinator.restoreForUser(userId);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _returningToOrigin = false;
        _returnNotice =
            'Chưa thể khôi phục điểm quay lại. Dữ liệu vẫn được giữ; hãy thử lại.';
      });
      return;
    }
    if (!mounted) return;
    setState(() => _returningToOrigin = false);
    final isFamily =
        (AuthState.instance.role ?? '').trim().toUpperCase() == 'FAMILY';
    String? location;
    switch (decision.destination) {
      case TriageContinuationDestination.motherJourney:
        if (!isFamily) {
          location =
              '/mother-home?tab=1&triageReturn=${Uri.encodeQueryComponent(widget.sessionId)}';
        }
        break;
      case TriageContinuationDestination.babyProfile:
        final reference = decision.originReferenceId;
        if (!isFamily && reference != null && reference.isNotEmpty) {
          location = '/babies/detail/${Uri.encodeComponent(reference)}';
        }
        break;
      case TriageContinuationDestination.safeDashboard:
        location = isFamily ? '/?triageChecked=true' : '/mother-home';
        break;
      case TriageContinuationDestination.emergency:
        await _openEmergencyFlow();
        return;
      case TriageContinuationDestination.none:
        break;
    }
    if (location == null) {
      setState(() {
        _returnNotice =
            'Chưa thể khôi phục điểm quay lại. Dữ liệu vẫn được giữ; hãy thử lại.';
      });
      return;
    }
    router.go(
      location,
      extra:
          !isFamily &&
              (decision.destination ==
                      TriageContinuationDestination.motherJourney ||
                  decision.destination ==
                      TriageContinuationDestination.babyProfile)
          ? TriageContinuationArrival(
              userId: userId,
              decision: decision,
              coordinator: _continuationCoordinator,
            )
          : decision.destination ==
                    TriageContinuationDestination.safeDashboard &&
                decision.isRecoverable
          ? const TriageContinuationRecoveryNotice()
          : null,
    );
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    final requestUserId = AuthState.instance.userId;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await _triageService.getResult(widget.sessionId);
      if (_isCurrentLoad(generation, requestUserId)) {
        setState(() => _result = result);
      }
    } catch (_) {
      if (_isCurrentLoad(generation, requestUserId)) {
        setState(() => _error = 'Không thể tải kết quả. Vui lòng thử lại.');
      }
    }
    if (!mounted || generation != _loadGeneration) return;
    if (AuthState.instance.userId != requestUserId) {
      setState(() {
        _result = null;
        _loading = false;
        _error =
            'Phiên đăng nhập đã thay đổi. Vui lòng tải lại kết quả cho tài khoản hiện tại.';
      });
    } else {
      setState(() => _loading = false);
    }
  }

  bool _isCurrentLoad(int generation, String? requestUserId) =>
      mounted &&
      generation == _loadGeneration &&
      AuthState.instance.userId == requestUserId;

  _RiskPresentation get _presentation =>
      _RiskPresentation.forLevel(_result?.riskLevel, stage: _result?.stage);

  bool get _isPostpartumRed =>
      _result?.stage == 'POSTPARTUM' &&
      (_result?.riskLevel == 'RED' || _result?.emergencyActionRequired == true);

  bool _canOpenYellowHandoff(TriageResult result) =>
      result.status == 'COMPLETED' &&
      result.riskLevel == 'YELLOW' &&
      const {
        'MOTHER_JOURNEY',
        'BABY_PROFILE',
      }.contains(result.originDashboard) &&
      (result.originDashboard == 'BABY_PROFILE' ||
          (result.journeyId ?? '').isNotEmpty) &&
      (result.originReferenceId ?? '').isNotEmpty;

  void _openYellowHandoff(TriageResult result) {
    if (!_canOpenYellowHandoff(result)) return;
    context.push('/triage/expert-handoff', extra: result.sessionId);
  }

  Future<void> _openEmergencyFlow() async {
    if (_openingEmergency) return;
    final requestUserId = AuthState.instance.userId;
    if (requestUserId == null || requestUserId.isEmpty) {
      setState(() {
        _emergencyLoadFailed = true;
        _emergencyNotice = 'Vui lòng đăng nhập lại trước khi tải phiên hỗ trợ.';
      });
      return;
    }
    setState(() {
      _openingEmergency = true;
      _emergencyLoadFailed = false;
      _emergencyNotice = 'Đang tải phiên hỗ trợ khẩn cấp...';
    });
    try {
      final session = await _emergencyService.getActive().timeout(
        const Duration(seconds: 8),
      );
      if (!mounted) return;
      if (AuthState.instance.userId != requestUserId) {
        setState(() {
          _emergencyLoadFailed = true;
          _emergencyNotice =
              'Phiên đăng nhập đã thay đổi. Hãy tải lại phiên hỗ trợ cho tài khoản hiện tại.';
        });
        return;
      }
      if (session == null) {
        throw StateError('Missing backend-created emergency session');
      }
      if (mounted) {
        final location = Uri(
          path: '/emergency/map',
          queryParameters: {
            'mode': 'triage',
            'stage': _result?.stage ?? 'INFANT',
          },
        ).toString();
        context.push(location, extra: session);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            behavior: SnackBarBehavior.floating,
            shape: StadiumBorder(),
            content: Text(
              'Phiên hỗ trợ đã mở; yêu cầu thông báo đang được xử lý.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _emergencyLoadFailed = true;
          _emergencyNotice =
              'Không thể tải phiên hỗ trợ lúc này. Phiên có thể vẫn đang được hệ thống xử lý.';
        });
      }
    } finally {
      if (mounted) setState(() => _openingEmergency = false);
    }
  }

  Future<void> _call115() async {
    if (_dialing115 || !mounted) return;
    setState(() {
      _dialing115 = true;
      _dialerNotice = null;
    });
    var opened = false;
    try {
      opened =
          await (widget.postpartumEmergencyLauncher?.call() ??
              launchUrl(
                Uri.parse('tel:115'),
                mode: LaunchMode.externalApplication,
              ));
    } catch (_) {
      opened = false;
    }
    if (!mounted) return;
    setState(() {
      _dialing115 = false;
      _dialerNotice = opened
          ? 'Đang mở cuộc gọi cấp cứu 115.'
          : 'Không thể mở ứng dụng gọi điện. Hãy tự gọi 115 ngay hoặc nhờ người bên cạnh gọi giúp.';
    });
  }

  Future<void> _openSourceUrl(TriageCitation citation) async {
    final uri = Uri.tryParse(citation.url);
    if (uri == null || !_isSafeCitationUri(uri, citation.domain)) return;
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở nguồn tham khảo trên thiết bị này.'),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở nguồn tham khảo trên thiết bị này.'),
          ),
        );
      }
    }
  }

  bool _isSafeCitationUri(Uri uri, String? approvedDomain) {
    final domain = (approvedDomain ?? '').toLowerCase().replaceFirst(
      RegExp(r'^www\\.'),
      '',
    );
    final host = uri.host.toLowerCase().replaceFirst(RegExp(r'^www\\.'), '');
    final path = uri.path.replaceAll('/', '').trim().toLowerCase();
    final genericSearchHost = const {
      'google.com',
      'bing.com',
      'yahoo.com',
    }.contains(host);
    final genericSearchPath = RegExp(
      r'(^|/)(search|query|find)(/|$)',
    ).hasMatch(uri.path.toLowerCase());
    return uri.scheme == 'https' &&
        uri.host.isNotEmpty &&
        (uri.port == -1 || uri.port == 443) &&
        uri.userInfo.isEmpty &&
        host != 'localhost' &&
        host != '127.0.0.1' &&
        !genericSearchHost &&
        !genericSearchPath &&
        domain.isNotEmpty &&
        path.isNotEmpty &&
        path != 'vi' &&
        path != 'en' &&
        (host == domain || host.endsWith('.$domain'));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: _loading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : _error != null
                  ? _buildError()
                  : SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
                      child: _buildContent(),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 48,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            SizedBox(
              width: 48,
              height: 48,
              child: IconButton(
                padding: EdgeInsets.zero,
                icon: const Icon(Icons.arrow_back, color: _primary),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
            const Expanded(
              child: Text(
                'Kết quả',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ),
            IconButton(
              key: const Key('triage-result-history'),
              tooltip: 'Lịch sử AI Triage',
              onPressed: () => context.push('/triage/history'),
              icon: const Icon(Icons.history_rounded, color: _primary),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              key: const Key('risk-result-retry'),
              onPressed: _load,
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    final p = _presentation;
    final result = _result;
    final needsMoreInfo =
        result?.triageStatus == 'NEED_MORE_INFO' ||
        result?.status == 'NEED_MORE_INFO';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Risk Result Card
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: p.cardColor,
            borderRadius: BorderRadius.circular(40),
            boxShadow: const [
              BoxShadow(
                color: Color(0x0F5A463F),
                blurRadius: 20,
                offset: Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            children: [
              Container(
                width: 64,
                height: 64,
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(
                  color: p.iconBg,
                  shape: BoxShape.circle,
                ),
                child: Icon(p.icon, size: 32, color: p.iconColor),
              ),
              Text(
                needsMoreInfo ? 'Cần thêm thông tin' : p.title,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                result?.summary ?? p.description,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 14,
                  color: _onSurfaceVariant,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),
        if (needsMoreInfo) ...[
          _buildListSection(
            title: 'Câu hỏi cần bổ sung',
            icon: Icons.help_outline,
            items: result?.questions.isNotEmpty == true
                ? result!.questions
                : const [
                    'Vui lòng bổ sung tuổi, tình trạng thở, tỉnh táo và bú/uống của trẻ.',
                  ],
          ),
          const SizedBox(height: 24),
        ],
        if ((result?.possibleConcern ?? '').isNotEmpty) ...[
          _buildInfoSection(
            title: 'Điểm cần chú ý',
            icon: Icons.health_and_safety_outlined,
            body: result!.possibleConcern!,
          ),
          const SizedBox(height: 16),
        ],
        if ((result?.recommendedAction ?? '').isNotEmpty) ...[
          _buildInfoSection(
            title: 'Hành động khuyến nghị',
            icon: Icons.checklist_rtl,
            body: result!.recommendedAction!,
          ),
          const SizedBox(height: 16),
        ],
        if (result?.redFlags.isNotEmpty == true) ...[
          _buildListSection(
            title: 'Dấu hiệu cảnh báo',
            icon: Icons.warning_amber_outlined,
            items: result!.redFlags,
          ),
          const SizedBox(height: 16),
        ],
        // Recommended actions
        const Text(
          'Hành động khuyến nghị',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 16),
        ...p.actions.map(
          (a) => Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(32),
                border: Border.all(
                  color: _outlineVariant.withValues(alpha: 0.3),
                ),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 20,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Icon(a.icon, color: _primary),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          a.title,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          a.description,
                          style: const TextStyle(
                            fontSize: 14,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 8),
        if (_result?.riskLevel != 'RED' &&
            (_result?.continuationToken ?? '').isNotEmpty) ...[
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton.icon(
              key: const Key('risk-result-return-to-origin'),
              onPressed: _returningToOrigin ? null : _returnToValidatedOrigin,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
              ),
              icon: _returningToOrigin
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.arrow_back_rounded),
              label: const Text(
                'Quay lại nơi bắt đầu',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
            ),
          ),
          const SizedBox(height: 8),
        ],
        // Action buttons
        if (result?.riskLevel == 'YELLOW') ...[
          if (_canOpenYellowHandoff(result!))
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton.icon(
                key: const Key('risk-result-yellow-expert-handoff-cta'),
                onPressed: () => _openYellowHandoff(result),
                style: FilledButton.styleFrom(
                  backgroundColor: _primaryContainer,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                ),
                icon: const Icon(Icons.support_agent),
                label: const Text(
                  'Tìm chuyên gia đã xác thực',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
                ),
              ),
            )
          else
            Semantics(
              liveRegion: true,
              child: Container(
                key: const Key('risk-result-yellow-handoff-unavailable'),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _secondaryContainerAlt,
                  borderRadius: BorderRadius.circular(24),
                  border: const Border(
                    left: BorderSide(color: _primaryContainer, width: 4),
                  ),
                ),
                child: const Text(
                  'Chưa thể chuyển ngữ cảnh cho chuyên gia vì kết quả chưa có đầy đủ liên kết hành trình. Hướng dẫn YELLOW vẫn được giữ an toàn; hãy thử tải lại từ nơi bắt đầu.',
                  style: TextStyle(
                    color: _onSurfaceVariant,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    height: 1.4,
                  ),
                ),
              ),
            ),
          const SizedBox(height: 8),
        ],
        if (result?.riskLevel != 'YELLOW')
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              key: const Key('risk-result-doctor-cta'),
              style: ElevatedButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                elevation: 4,
                shadowColor: _primaryContainer.withValues(alpha: 0.4),
              ),
              // TODO: navigate to Expert Directory (CB-018) once that screen is implemented
              onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text(
                    'Tính năng liên hệ bác sĩ tư vấn đang được phát triển',
                  ),
                ),
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.phone, size: 20),
                  SizedBox(width: 8),
                  Text(
                    'Liên hệ bác sĩ tư vấn',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 8),
        if (_isPostpartumRed) ...[
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton.icon(
              key: const Key('risk-result-postpartum-call-115'),
              onPressed: _dialing115 ? null : _call115,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFBA1A1A),
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                elevation: 4,
                shadowColor: const Color(0x33BA1A1A),
              ),
              icon: _dialing115
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.call),
              label: const Text(
                'Gọi cấp cứu 115 ngay',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
              ),
            ),
          ),
          const SizedBox(height: 8),
        ],
        if (result?.riskLevel != 'YELLOW')
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              key:
                  (_result?.emergencyActionRequired == true ||
                      _result?.riskLevel == 'RED')
                  ? const Key('risk-result-emergency-cta')
                  : const Key('risk-result-clinic-cta'),
              style: ElevatedButton.styleFrom(
                backgroundColor: _secondaryContainerAlt,
                foregroundColor: _secondary,
                shape: const StadiumBorder(),
                elevation: 0,
              ),
              onPressed: _openingEmergency
                  ? null
                  : (_result?.emergencyActionRequired == true ||
                        _result?.riskLevel == 'RED')
                  ? _openEmergencyFlow
                  : () => ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text(
                          'Tính năng tìm phòng khám gần nhất thuộc TV4 Map/Location',
                        ),
                      ),
                    ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    (_result?.emergencyActionRequired == true ||
                            _result?.riskLevel == 'RED')
                        ? Icons.emergency_outlined
                        : Icons.local_hospital_outlined,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    (_result?.emergencyActionRequired == true ||
                            _result?.riskLevel == 'RED')
                        ? _emergencyLoadFailed
                              ? 'Thử tải lại phiên hỗ trợ'
                              : 'Mở phiên hỗ trợ khẩn cấp'
                        : 'Tìm phòng khám gần nhất',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ),
        if ((_emergencyNotice ?? '').isNotEmpty ||
            (_dialerNotice ?? '').isNotEmpty ||
            (_returnNotice ?? '').isNotEmpty) ...[
          const SizedBox(height: 12),
          Semantics(
            liveRegion: true,
            container: true,
            child: Container(
              key: const Key('risk-result-emergency-status'),
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _secondaryContainerAlt,
                borderRadius: BorderRadius.circular(20),
                border: const Border(
                  left: BorderSide(color: _primaryContainer, width: 4),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if ((_emergencyNotice ?? '').isNotEmpty)
                    Text(
                      _emergencyNotice!,
                      style: const TextStyle(
                        color: _onSurfaceVariant,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        height: 1.4,
                      ),
                    ),
                  if ((_dialerNotice ?? '').isNotEmpty) ...[
                    if ((_emergencyNotice ?? '').isNotEmpty)
                      const SizedBox(height: 8),
                    Text(
                      _dialerNotice!,
                      style: const TextStyle(
                        color: _onSurfaceVariant,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        height: 1.4,
                      ),
                    ),
                  ],
                  if ((_returnNotice ?? '').isNotEmpty) ...[
                    if ((_emergencyNotice ?? '').isNotEmpty ||
                        (_dialerNotice ?? '').isNotEmpty)
                      const SizedBox(height: 8),
                    Text(
                      _returnNotice!,
                      style: const TextStyle(
                        color: _onSurfaceVariant,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        height: 1.4,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 16),
        if ((result?.ragAnswer ?? '').isNotEmpty) ...[
          _buildRagGuidance(result!),
          const SizedBox(height: 16),
        ],
        if (result?.citations.isNotEmpty == true) ...[
          _buildCitations(result!.citations),
          const SizedBox(height: 16),
        ] else if ((result?.warning ?? '').isNotEmpty) ...[
          _buildInfoSection(
            title: 'Nguồn tham khảo',
            icon: Icons.source_outlined,
            body: result!.warning!,
          ),
          const SizedBox(height: 16),
        ],
        if ((result?.evidence?.legalSafetyNote ?? '').isNotEmpty) ...[
          _buildInfoSection(
            title: 'Cơ sở phân loại',
            icon: Icons.verified_outlined,
            body: result!.evidence!.legalSafetyNote,
          ),
          const SizedBox(height: 16),
        ],
        // AI disclaimer (from API)
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: _surfaceContainerLow,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _outlineVariant.withValues(alpha: 0.5)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.info_outline, color: _tertiary, size: 20),
              const SizedBox(width: 12),
              Expanded(
                child: RichText(
                  text: TextSpan(
                    style: const TextStyle(
                      fontSize: 12,
                      color: _tertiary,
                      height: 1.4,
                    ),
                    children: [
                      const TextSpan(
                        text: 'Lưu ý quan trọng: ',
                        style: TextStyle(fontWeight: FontWeight.w600),
                      ),
                      TextSpan(
                        text:
                            _result?.disclaimer ??
                            'CareBridge AI cung cấp thông tin tham khảo dựa trên dữ liệu nhập vào, không thay thế chẩn đoán y khoa chuyên nghiệp. Nếu tình trạng của bé trở nặng, hãy liên hệ ngay với cơ sở y tế.',
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildInfoSection({
    required String title,
    required IconData icon,
    required String body,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  body,
                  style: const TextStyle(
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildListSection({
    required String title,
    required IconData icon,
    required List<String> items,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: _primary),
              const SizedBox(width: 8),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...items.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('• ', style: TextStyle(color: _onSurfaceVariant)),
                  Expanded(
                    child: Text(
                      item,
                      style: const TextStyle(
                        fontSize: 14,
                        color: _onSurfaceVariant,
                        height: 1.35,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRagGuidance(TriageResult result) {
    return Container(
      key: const Key('triage-rag-guidance'),
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainerLow,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _tertiary.withValues(alpha: 0.25)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Hướng dẫn tham khảo phù hợp với triệu chứng',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 8),
          Text(result.ragAnswer!),
          if ((result.ragDisclaimer ?? '').isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              result.ragDisclaimer!,
              style: const TextStyle(fontSize: 12, color: _tertiary),
            ),
          ],
          if (result.ragFallback == true) ...[
            const SizedBox(height: 8),
            const Text(
              'Nguồn tham khảo hiện chưa sẵn sàng; kết quả phân loại vẫn được giữ nguyên.',
              style: TextStyle(fontSize: 12, color: Colors.orange),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildCitations(List<TriageCitation> citations) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.source_outlined, color: _primary),
              SizedBox(width: 8),
              Text(
                'Nguồn tham khảo chính thống',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...citations.indexed.map((entry) {
            final index = entry.$1;
            final citation = entry.$2;
            final uri = Uri.tryParse(citation.url);
            final canOpen =
                uri != null && _isSafeCitationUri(uri, citation.domain);
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${citation.source} — ${citation.title}',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    citation.excerpt,
                    style: const TextStyle(
                      fontSize: 12,
                      color: _onSurfaceVariant,
                      height: 1.35,
                    ),
                  ),
                  if (citation.matchedSymptoms.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      'Triệu chứng khớp: ${citation.matchedSymptoms.join(', ')}',
                      style: const TextStyle(
                        fontSize: 12,
                        color: _onSurfaceVariant,
                        height: 1.35,
                      ),
                    ),
                  ],
                  if (citation.sourceStatus == 'PENDING_REVIEW') ...[
                    const SizedBox(height: 6),
                    Container(
                      key: Key(
                        'risk-citation-pending-${citation.id ?? citation.url}-$index',
                      ),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: _outlineVariant.withValues(alpha: 0.5),
                        ),
                      ),
                      child: const Text(
                        'Nguồn chính thống được truy xuất tự động, đang chờ kiểm duyệt nội bộ.',
                        style: TextStyle(
                          fontSize: 11,
                          color: _onSurfaceVariant,
                          height: 1.3,
                        ),
                      ),
                    ),
                  ],
                  if (citation.url.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    InkWell(
                      key: Key(
                        'risk-citation-link-${citation.id ?? citation.url}-$index',
                      ),
                      onTap: canOpen ? () => _openSourceUrl(citation) : null,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(
                            Icons.open_in_new,
                            size: 16,
                            color: _primary,
                          ),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              citation.url,
                              style: const TextStyle(
                                fontSize: 12,
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
          }),
        ],
      ),
    );
  }
}

class _RecommendedAction {
  final IconData icon;
  final String title;
  final String description;

  const _RecommendedAction(this.icon, this.title, this.description);
}

class _RiskPresentation {
  final String title;
  final String description;
  final IconData icon;
  final Color cardColor;
  final Color iconBg;
  final Color iconColor;
  final List<_RecommendedAction> actions;

  const _RiskPresentation({
    required this.title,
    required this.description,
    required this.icon,
    required this.cardColor,
    required this.iconBg,
    required this.iconColor,
    required this.actions,
  });

  static _RiskPresentation forLevel(String? riskLevel, {String? stage}) {
    if (stage == 'POSTPARTUM') {
      return _postpartum(riskLevel);
    }
    switch (riskLevel) {
      case 'GREEN':
        return const _RiskPresentation(
          title: 'Bình thường',
          description:
              'Hiện tại chưa thấy dấu hiệu đáng lo ngại. Mẹ hãy tiếp tục theo dõi bé như thường lệ.',
          icon: Icons.check_circle,
          cardColor: Color(0xFFE3EFE6),
          iconBg: Color(0x334CAF50),
          iconColor: Color(0xFF2E7D32),
          actions: [
            _RecommendedAction(
              Icons.monitor_heart,
              'Theo dõi tại nhà',
              'Tiếp tục quan sát các biểu hiện của bé trong 24 giờ tới.',
            ),
            _RecommendedAction(
              Icons.water_drop,
              'Bổ sung nước',
              'Cho bé bú hoặc uống nước thường xuyên.',
            ),
            _RecommendedAction(
              Icons.assignment_outlined,
              'Ghi chép triệu chứng',
              'Sử dụng tính năng nhật ký để theo dõi bất kỳ thay đổi nào.',
            ),
          ],
        );
      case 'RED':
        return const _RiskPresentation(
          title: 'Cần cấp cứu ngay',
          description:
              'Các dấu hiệu cho thấy bé cần được khám cấp cứu ngay lập tức. Vui lòng liên hệ cơ sở y tế gần nhất.',
          icon: Icons.emergency,
          cardColor: Color(0xFFFFDAD6),
          iconBg: Color(0x33BA1A1A),
          iconColor: Color(0xFFBA1A1A),
          actions: [
            _RecommendedAction(
              Icons.local_hospital,
              'Đến cơ sở y tế ngay',
              'Đưa bé đến phòng cấp cứu gần nhất hoặc gọi xe cứu thương.',
            ),
            _RecommendedAction(
              Icons.campaign,
              'Báo cho người thân',
              'Thông báo cho người thân để hỗ trợ kịp thời.',
            ),
            _RecommendedAction(
              Icons.monitor_heart,
              'Theo dõi sát',
              'Quan sát liên tục các dấu hiệu sinh tồn của bé trên đường di chuyển.',
            ),
          ],
        );
      case 'YELLOW':
      default:
        return const _RiskPresentation(
          title: 'Cần theo dõi',
          description:
              'Dựa trên thông tin bạn cung cấp, có một số dấu hiệu cần được quan sát thêm.',
          icon: Icons.warning,
          cardColor: Color(0xFFFFE2D9),
          iconBg: Color(0x33E8A87C),
          iconColor: Color(0xFFD97706),
          actions: [
            _RecommendedAction(
              Icons.monitor_heart,
              'Theo dõi nhiệt độ',
              'Kiểm tra nhiệt độ của bé mỗi 4 giờ một lần và ghi chú lại.',
            ),
            _RecommendedAction(
              Icons.water_drop,
              'Bổ sung nước',
              'Cho bé bú hoặc uống nước thường xuyên để tránh mất nước.',
            ),
            _RecommendedAction(
              Icons.assignment_outlined,
              'Ghi chép triệu chứng',
              'Sử dụng tính năng nhật ký để theo dõi bất kỳ thay đổi nào.',
            ),
          ],
        );
    }
  }

  static _RiskPresentation _postpartum(String? riskLevel) {
    switch (riskLevel) {
      case 'GREEN':
        return const _RiskPresentation(
          title: 'Tiếp tục theo dõi',
          description:
              'Chưa ghi nhận dấu hiệu cảnh báo từ thông tin hiện có. Hãy tiếp tục theo dõi quá trình hồi phục.',
          icon: Icons.check_circle,
          cardColor: Color(0xFFE3EFE6),
          iconBg: Color(0x334CAF50),
          iconColor: Color(0xFF2E7D32),
          actions: [
            _RecommendedAction(
              Icons.monitor_heart,
              'Theo dõi hồi phục',
              'Ghi nhận mọi thay đổi và đánh giá lại ngay nếu tình trạng xấu đi.',
            ),
            _RecommendedAction(
              Icons.support_agent,
              'Liên hệ nhân viên y tế',
              'Trao đổi với nhân viên y tế nếu dấu hiệu kéo dài hoặc gây lo lắng.',
            ),
          ],
        );
      case 'RED':
        return const _RiskPresentation(
          title: 'Cần hỗ trợ khẩn cấp ngay',
          description:
              'Có dấu hiệu cảnh báo sau sinh cần được hỗ trợ khẩn cấp. Hãy gọi 115 hoặc đến cơ sở y tế gần nhất.',
          icon: Icons.emergency,
          cardColor: Color(0xFFFFDAD6),
          iconBg: Color(0x33BA1A1A),
          iconColor: Color(0xFFBA1A1A),
          actions: [
            _RecommendedAction(
              Icons.local_hospital,
              'Đến cơ sở y tế ngay',
              'Gọi cấp cứu 115 hoặc đến cơ sở y tế gần nhất ngay.',
            ),
            _RecommendedAction(
              Icons.campaign,
              'Báo cho người thân',
              'Nhờ người thân ở bên cạnh và hỗ trợ di chuyển an toàn.',
            ),
            _RecommendedAction(
              Icons.people_outline,
              'Không ở một mình',
              'Ở cùng một người đáng tin cậy trong khi chờ hỗ trợ.',
            ),
          ],
        );
      case 'YELLOW':
      default:
        return const _RiskPresentation(
          title: 'Cần được đánh giá thêm',
          description:
              'Dấu hiệu hồi phục sau sinh cần được theo dõi và nhân viên y tế đánh giá thêm.',
          icon: Icons.warning,
          cardColor: Color(0xFFFFE2D9),
          iconBg: Color(0x33E8A87C),
          iconColor: Color(0xFFD97706),
          actions: [
            _RecommendedAction(
              Icons.monitor_heart,
              'Theo dõi diễn tiến',
              'Ghi nhận thời điểm, mức độ và mọi thay đổi của dấu hiệu.',
            ),
            _RecommendedAction(
              Icons.support_agent,
              'Liên hệ nhân viên y tế',
              'Sắp xếp đánh giá trực tiếp nếu dấu hiệu kéo dài hoặc nặng hơn.',
            ),
          ],
        );
    }
  }
}
