import 'package:flutter/material.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';
import '../../emergency/screens/emergency_map_screen.dart';

/// CB-016 — Risk Triage Result (UC-61)
/// Shows the AI risk classification (GREEN/YELLOW/RED) for a completed
/// intake session. Data: GET /api/v1/triage/intake/{sessionId}.
class RiskTriageResultScreen extends StatefulWidget {
  final String sessionId;

  const RiskTriageResultScreen({super.key, required this.sessionId});

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

  final _triageService = TriageService();
  TriageResult? _result;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
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
    } catch (e) {
      if (mounted) setState(() => _error = 'Không thể tải kết quả: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  _RiskPresentation get _presentation => _RiskPresentation.forLevel(_result?.riskLevel);

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
                  ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
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
              child: Text('Kết quả',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
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
            Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: _onSurfaceVariant)),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _load, child: const Text('Thử lại')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    final p = _presentation;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Risk Result Card
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: p.cardColor,
            borderRadius: BorderRadius.circular(16),
            boxShadow: const [BoxShadow(color: Color(0x0F5A463F), blurRadius: 20, offset: Offset(0, 4))],
          ),
          child: Column(
            children: [
              Container(
                width: 64,
                height: 64,
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(color: p.iconBg, shape: BoxShape.circle),
                child: Icon(p.icon, size: 32, color: p.iconColor),
              ),
              Text(p.title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w600, color: _onSurface)),
              const SizedBox(height: 8),
              Text(p.description,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 14, color: _onSurfaceVariant, height: 1.4)),
            ],
          ),
        ),
        const SizedBox(height: 24),
        // Recommended actions
        const Text('Hành động khuyến nghị',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
        const SizedBox(height: 16),
        ...p.actions.map((a) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
                  boxShadow: const [BoxShadow(color: Color(0x0F5A463F), blurRadius: 20, offset: Offset(0, 4))],
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
                          Text(a.title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
                          const SizedBox(height: 4),
                          Text(a.description, style: const TextStyle(fontSize: 14, color: _onSurfaceVariant)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            )),
        const SizedBox(height: 8),
        // Action buttons
        SizedBox(
          width: double.infinity,
          height: 52,
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _primaryContainer,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              elevation: 4,
              shadowColor: _primaryContainer.withValues(alpha: 0.4),
            ),
            // TODO: navigate to Expert Directory (CB-018) once that screen is implemented
            onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Tính năng liên hệ bác sĩ tư vấn đang được phát triển')),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.phone, size: 20),
                SizedBox(width: 8),
                Text('Liên hệ bác sĩ tư vấn', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _secondaryContainerAlt,
              foregroundColor: _secondary,
              shape: const StadiumBorder(),
              elevation: 0,
            ),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const EmergencyMapScreen()),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.local_hospital_outlined, size: 20),
                SizedBox(width: 8),
                Text('Tìm phòng khám gần nhất', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        // AI disclaimer (from API)
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: _surfaceContainerLow,
            borderRadius: BorderRadius.circular(16),
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
                    style: const TextStyle(fontSize: 12, color: _tertiary, height: 1.4),
                    children: [
                      const TextSpan(text: 'Lưu ý quan trọng: ', style: TextStyle(fontWeight: FontWeight.w600)),
                      TextSpan(
                        text: _result?.disclaimer ??
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
          description: 'Hiện tại chưa thấy dấu hiệu đáng lo ngại. Mẹ hãy tiếp tục theo dõi bé như thường lệ.',
          icon: Icons.check_circle,
          cardColor: Color(0xFFE3EFE6),
          iconBg: Color(0x334CAF50),
          iconColor: Color(0xFF2E7D32),
          actions: [
            _RecommendedAction(Icons.monitor_heart, 'Theo dõi tại nhà', 'Tiếp tục quan sát các biểu hiện của bé trong 24 giờ tới.'),
            _RecommendedAction(Icons.water_drop, 'Bổ sung nước', 'Cho bé bú hoặc uống nước thường xuyên.'),
            _RecommendedAction(Icons.assignment_outlined, 'Ghi chép triệu chứng', 'Sử dụng tính năng nhật ký để theo dõi bất kỳ thay đổi nào.'),
          ],
        );
      case 'RED':
        return const _RiskPresentation(
          title: 'Cần cấp cứu ngay',
          description: 'Các dấu hiệu cho thấy bé cần được khám cấp cứu ngay lập tức. Vui lòng liên hệ cơ sở y tế gần nhất.',
          icon: Icons.emergency,
          cardColor: Color(0xFFFFDAD6),
          iconBg: Color(0x33BA1A1A),
          iconColor: Color(0xFFBA1A1A),
          actions: [
            _RecommendedAction(Icons.local_hospital, 'Đến cơ sở y tế ngay', 'Đưa bé đến phòng cấp cứu gần nhất hoặc gọi xe cứu thương.'),
            _RecommendedAction(Icons.campaign, 'Báo cho người thân', 'Thông báo cho người thân để hỗ trợ kịp thời.'),
            _RecommendedAction(Icons.monitor_heart, 'Theo dõi sát', 'Quan sát liên tục các dấu hiệu sinh tồn của bé trên đường di chuyển.'),
          ],
        );
      case 'YELLOW':
      default:
        return const _RiskPresentation(
          title: 'Cần theo dõi',
          description: 'Dựa trên thông tin bạn cung cấp, có một số dấu hiệu cần được quan sát thêm.',
          icon: Icons.warning,
          cardColor: Color(0xFFFFE2D9),
          iconBg: Color(0x33E8A87C),
          iconColor: Color(0xFFD97706),
          actions: [
            _RecommendedAction(Icons.monitor_heart, 'Theo dõi nhiệt độ', 'Kiểm tra nhiệt độ của bé mỗi 4 giờ một lần và ghi chú lại.'),
            _RecommendedAction(Icons.water_drop, 'Bổ sung nước', 'Cho bé bú hoặc uống nước thường xuyên để tránh mất nước.'),
            _RecommendedAction(Icons.assignment_outlined, 'Ghi chép triệu chứng', 'Sử dụng tính năng nhật ký để theo dõi bất kỳ thay đổi nào.'),
          ],
        );
    }
  }
}
