import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';
import '../../emergency/services/emergency_service.dart';

/// CB-016 — Risk Triage Result (UC-61)
/// Shows the AI risk classification (GREEN/YELLOW/RED) for a completed
/// intake session. Data: GET /api/v1/triage/intake/{sessionId}.
class RiskTriageResultScreen extends StatefulWidget {
  final String sessionId;
  final TriageService? triageService;
  final EmergencyService? emergencyService;

  const RiskTriageResultScreen({
    super.key,
    required this.sessionId,
    this.triageService,
    this.emergencyService,
  });

  @override
  State<RiskTriageResultScreen> createState() => _RiskTriageResultScreenState();
}

class _RiskTriageResultScreenState extends State<RiskTriageResultScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _tertiary = Color(0xFF625D59);
  static const _secondaryContainerAlt = Color(0xFFF2EAE4);
  static const _secondary = Color(0xFF6E5A52);

  late final TriageService _triageService;
  late final EmergencyService _emergencyService;
  TriageResult? _result;
  bool _loading = true;
  bool _openingEmergency = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _triageService = widget.triageService ?? TriageService();
    _emergencyService = widget.emergencyService ?? EmergencyService();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await _triageService.getResult(widget.sessionId);
      if (mounted) setState(() => _result = result);
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể tải kết quả. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  _RiskPresentation get _presentation =>
      _RiskPresentation.forLevel(_result?.riskLevel);

  Future<void> _openEmergencyFlow() async {
    if (_openingEmergency) return;
    setState(() => _openingEmergency = true);
    try {
      await _emergencyService
          .openFlow(triggerSource: 'AI_TRIAGE')
          .timeout(const Duration(seconds: 8));
      if (mounted) {
        context.push('/emergency/map');
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã kích hoạt hỗ trợ khẩn cấp và gửi cảnh báo người thân',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Không thể kích hoạt hỗ trợ khẩn cấp. Vui lòng gọi cấp cứu hoặc thử lại.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _openingEmergency = false);
    }
  }

  Future<void> _openSourceUrl(String url) async {
    final uri = Uri.tryParse(url);
    if (uri == null || !_isAllowedOfficialUri(uri)) return;
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  bool _isAllowedOfficialUri(Uri uri) {
    const allowed = {
      'who.int',
      'moh.gov.vn',
      'mch.moh.gov.vn',
      'cdc.gov',
      'unicef.org',
      'benhviennhitrunguong.gov.vn',
      'nhidong.org.vn',
      'bvndtp.org.vn',
    };
    final host = uri.host.toLowerCase();
    return uri.scheme == 'https' &&
        allowed.any((domain) => host == domain || host.endsWith('.$domain'));
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
            const SizedBox(width: 48, height: 48),
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
            ElevatedButton(onPressed: _load, child: const Text('Thử lại')),
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
        // Action buttons
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
                      ? 'Kích hoạt hỗ trợ khẩn cấp'
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
        const SizedBox(height: 16),
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
          ...citations.map((citation) {
            final uri = Uri.tryParse(citation.url);
            final canOpen = uri != null && _isAllowedOfficialUri(uri);
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
                        'risk-citation-pending-${citation.id ?? citation.url}',
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
                        'risk-citation-link-${citation.id ?? citation.url}',
                      ),
                      onTap: canOpen
                          ? () => _openSourceUrl(citation.url)
                          : null,
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

  static _RiskPresentation forLevel(String? riskLevel) {
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
}
