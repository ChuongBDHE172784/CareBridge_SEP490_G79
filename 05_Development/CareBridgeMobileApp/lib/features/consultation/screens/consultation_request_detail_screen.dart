import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../../shared/components/app_user_avatar.dart';
import '../models/consultation_request.dart';
import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';
import '../services/triage_expert_handoff_service.dart';
import 'consultation_request_form_screen.dart';

class ConsultationRequestDetailScreen extends StatefulWidget {
  final String requestId;

  const ConsultationRequestDetailScreen({super.key, required this.requestId});

  @override
  State<ConsultationRequestDetailScreen> createState() =>
      _ConsultationRequestDetailScreenState();
}

class _ConsultationRequestDetailScreenState
    extends State<ConsultationRequestDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceLow = Color(0xFFF8EEE9);
  static const _surfaceHigh = Color(0xFFF1E6E0);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outline = Color(0xFFE5D3CA);
  static const _errColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  ConsultationRequestDetail? _request;
  TriageExpertHandoffParticipantContext? _triageContext;
  Object? _error;
  String? _contextNotice;
  bool _busy = false;
  int _generation = 0;
  late String? _accountId;

  @override
  void initState() {
    super.initState();
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
    if (!mounted) return;
    setState(() {
      _request = null;
      _triageContext = null;
      _busy = false;
      _contextNotice = null;
      _error =
          'Phiên đăng nhập đã thay đổi. Nội dung của tài khoản trước đã được xóa khỏi màn hình.';
    });
  }

  bool _isCurrent(int generation, String? accountId) =>
      mounted &&
      generation == _generation &&
      accountId != null &&
      accountId == AuthState.instance.userId;

  Future<void> _load() async {
    final generation = ++_generation;
    final accountId = AuthState.instance.userId;
    if (accountId == null || accountId.isEmpty) {
      setState(() => _error = 'Vui lòng đăng nhập lại để xem yêu cầu.');
      return;
    }
    setState(() {
      _error = null;
      _contextNotice = null;
    });
    try {
      final request = await ConsultationRequestService.instance.getById(
        widget.requestId,
      );
      if (!_isCurrent(generation, accountId)) return;
      TriageExpertHandoffParticipantContext? triageContext;
      String? contextNotice;
      if (_isTriageHandoffRequest(request)) {
        try {
          triageContext = await TriageExpertHandoffService.instance.getContext(
            widget.requestId,
          );
        } catch (error) {
          if (error is ApiException && error.statusCode == 404) {
            triageContext = null;
          } else if (error is ApiException && error.statusCode == 403) {
            contextNotice =
                'Ngữ cảnh đã chia sẻ hiện không còn hiệu lực hoặc chuyên gia không còn đủ điều kiện.';
          } else {
            contextNotice =
                'Chưa thể tải ngữ cảnh đã chia sẻ. Yêu cầu tư vấn vẫn được giữ nguyên.';
          }
        }
      }
      if (!_isCurrent(generation, accountId)) return;
      setState(() {
        _request = request;
        _triageContext = triageContext;
        _contextNotice = contextNotice;
      });
    } catch (error) {
      if (_isCurrent(generation, accountId)) {
        setState(() => _error = error);
      }
    }
  }

  bool _isTriageHandoffRequest(ConsultationRequestDetail request) =>
      request.topic == 'YELLOW triage expert support' &&
      request.description ==
          'Consented minimum YELLOW triage context is available in the protected context view.';

  Future<void> _cancel() async {
    final generation = _generation;
    final accountId = AuthState.instance.userId;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text(
          'Hủy yêu cầu tư vấn?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
        content: const Text(
          'Yêu cầu đang chờ sẽ được chuyển sang trạng thái đã hủy.',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 14),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Không', style: TextStyle(color: _outline)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _errColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
            ),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Hủy yêu cầu'),
          ),
        ],
      ),
    );
    if (confirmed != true || _busy) return;
    setState(() => _busy = true);
    try {
      final request = await ConsultationRequestService.instance.cancel(
        widget.requestId,
      );
      ConsultationRequestRefreshBus.notify();
      if (_isCurrent(generation, accountId)) {
        setState(() => _request = request);
      }
    } finally {
      if (_isCurrent(generation, accountId)) {
        setState(() => _busy = false);
      }
    }
  }

  Widget _buildTriageContext(TriageExpertHandoffContext context) {
    return Container(
      key: const Key('consultation-triage-context'),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFFFE082), width: 1.5),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF8E1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.verified_user_outlined,
                  color: Color(0xFFF57F17),
                  size: 20,
                ),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text(
                  'Ngữ cảnh YELLOW đã đồng ý chia sẻ',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    color: _onSurface,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFFFFF3E0),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(
              'Mức độ: ${context.riskLevel} • Giai đoạn: ${context.stage}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: Color(0xFFE65100),
                fontSize: 13,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(height: 10),
          Text(
            context.riskSummary,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _onSurfaceVariant,
              fontSize: 14,
              height: 1.45,
            ),
          ),
          if (context.citations.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Text(
              'Nguồn kiểm chứng y khoa',
              style: TextStyle(
                fontFamily: 'Lexend',
                color: _onSurface,
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 8),
            for (final citation in context.citations)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    const Icon(Icons.link_rounded, size: 16, color: _primary),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            citation.organization,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              color: _onSurface,
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            citation.baseUrl,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              color: _outline,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ],
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
            const Icon(
              Icons.error_outline_rounded,
              size: 44,
              color: _errColor,
            ),
            const SizedBox(height: 14),
            Text(
              _error is String
                  ? _error! as String
                  : 'Chưa thể tải yêu cầu tư vấn.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurface,
                fontSize: 15,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: AuthState.instance.userId == _accountId ? _load : null,
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _primary),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final request = _request;
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chi tiết yêu cầu',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
        centerTitle: true,
      ),
      body: _error != null
          ? _buildError()
          : request == null
          ? const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          : SafeArea(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                children: [
                  // Header User Card
                  _buildHeaderCard(request),
                  const SizedBox(height: 16),

                  // Topic & Description Card
                  _buildContentCard(request),
                  const SizedBox(height: 16),

                  // Reject Reason if any
                  if (request.rejectReason != null) ...[
                    _buildRejectReasonCard(request.rejectReason!),
                    const SizedBox(height: 16),
                  ],

                  // Triage Context if yellow handoff
                  if (_triageContext != null) ...[
                    _buildTriageContext(_triageContext!.context),
                    const SizedBox(height: 16),
                  ] else if ((_contextNotice ?? '').isNotEmpty) ...[
                    Semantics(
                      liveRegion: true,
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: _surfaceHigh,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: Text(
                          _contextNotice!,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            color: _primary,
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Het han la ngo cut: yeu cau tu dong, ma man hinh truoc day khong
                  // noi gi tiep theo. Mo lai duong di — dat lai chinh nguoi do, hoac
                  // sang danh sach xem ai dang ranh.
                  if (request.status == 'EXPIRED') ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: _surfaceHigh,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: const Text(
                        'Yêu cầu này đã hết hạn nên đã tự đóng. Bạn có thể gửi lại '
                        'yêu cầu cho chính chuyên gia này, hoặc chọn một chuyên gia '
                        'đang còn lịch trống.',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          height: 1.5,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: () => Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => ConsultationRequestFormScreen(
                              expertProfileId: request.expertProfileId,
                              expertDisplayName:
                                  request.counterpartDisplayName ?? 'Chuyên gia',
                            ),
                          ),
                        ),
                        icon: const Icon(Icons.refresh_rounded, size: 18),
                        label: const Text('Gửi lại yêu cầu'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: () => context.push('/experts'),
                        icon: const Icon(Icons.groups_outlined, size: 18),
                        label: const Text('Xem chuyên gia đang rảnh'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: _primary,
                          side: const BorderSide(color: _outline),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                        ),
                      ),
                    ),
                  ],

                  // Action Buttons Footer
                  if (request.status == 'PENDING') ...[
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: _busy ? null : _cancel,
                        icon: const Icon(Icons.close_rounded, size: 18),
                        label: const Text('Hủy yêu cầu'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: _errColor,
                          side: const BorderSide(color: _errorContainer),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                        ),
                      ),
                    ),
                  ],
                  if (request.status == 'ACCEPTED' &&
                      request.directConversationId != null) ...[
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: () => context.push(
                          '/direct-chat/${request.directConversationId}',
                        ),
                        icon: const Icon(
                          Icons.chat_bubble_outline_rounded,
                          size: 18,
                        ),
                        label: const Text('Mở hội thoại'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }

  Widget _buildHeaderCard(ConsultationRequestDetail request) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          AppUserAvatar(
            radius: 24,
            backgroundColor: _surfaceLow,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  request.counterpartDisplayName ?? 'Người dùng CareBridge',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    color: _onSurface,
                    fontWeight: FontWeight.w800,
                    fontSize: 17,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Yêu cầu tư vấn trực tuyến',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    color: _outline,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          _buildStatusBadge(request.status),
        ],
      ),
    );
  }

  Widget _buildContentCard(ConsultationRequestDetail request) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Topic Row
          Row(
            children: [
              const Icon(
                Icons.health_and_safety_outlined,
                color: _primary,
                size: 20,
              ),
              const SizedBox(width: 8),
              const Text(
                'Chủ đề tư vấn',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _outline,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            request.topic,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _onSurface,
              fontWeight: FontWeight.w800,
              fontSize: 18,
            ),
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 14),
            child: Divider(color: _surfaceLow, height: 1),
          ),

          // Description Section
          Row(
            children: [
              const Icon(
                Icons.description_outlined,
                color: _primary,
                size: 20,
              ),
              const SizedBox(width: 8),
              const Text(
                'Mô tả chi tiết',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _outline,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: _surfaceLow,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              request.description,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurface,
                fontSize: 14,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRejectReasonCard(String reason) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_outline, color: _errColor, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Lý do từ chối',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w700,
                    color: _errColor,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  reason,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    color: _errColor,
                    fontSize: 13,
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

  Widget _buildStatusBadge(String status) {
    late String label;
    late Color bg;
    late Color fg;
    late IconData icon;

    switch (status) {
      case 'PENDING':
        label = 'Đang chờ';
        bg = const Color(0xFFFFF3E0);
        fg = const Color(0xFFE65100);
        icon = Icons.schedule_rounded;
        break;
      case 'ACCEPTED':
        label = 'Đã nhận';
        bg = const Color(0xFFE6F4EA);
        fg = const Color(0xFF137333);
        icon = Icons.check_circle_rounded;
        break;
      case 'REJECTED':
        label = 'Bị từ chối';
        bg = _errorContainer;
        fg = _errColor;
        icon = Icons.highlight_off_rounded;
        break;
      case 'CANCELLED':
        label = 'Đã hủy';
        bg = const Color(0xFFF2EAE4);
        fg = _outline;
        icon = Icons.cancel_outlined;
        break;
      case 'EXPIRED':
        label = 'Hết hạn';
        bg = const Color(0xFFF2EAE4);
        fg = _outline;
        icon = Icons.timer_off_outlined;
        break;
      default:
        label = status;
        bg = _surfaceLow;
        fg = _onSurface;
        icon = Icons.info_outline;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}

