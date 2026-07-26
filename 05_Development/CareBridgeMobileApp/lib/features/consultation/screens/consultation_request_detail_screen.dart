import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/consultation_request.dart';
import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';
import '../services/triage_expert_handoff_service.dart';

class ConsultationRequestDetailScreen extends StatefulWidget {
  final String requestId;

  const ConsultationRequestDetailScreen({super.key, required this.requestId});

  @override
  State<ConsultationRequestDetailScreen> createState() =>
      _ConsultationRequestDetailScreenState();
}

class _ConsultationRequestDetailScreenState
    extends State<ConsultationRequestDetailScreen> {
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
        title: const Text('Hủy yêu cầu tư vấn?'),
        content: const Text('Yêu cầu đang chờ sẽ được chuyển sang đã hủy.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Không'),
          ),
          FilledButton(
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
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: const Color(0xFFE8DDD6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 28,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Ngữ cảnh YELLOW đã được đồng ý',
            style: TextStyle(
              color: Color(0xFF5A463F),
              fontSize: 18,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            '${context.riskLevel} • ${context.stage}',
            style: const TextStyle(
              color: Color(0xFF5A463F),
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            context.riskSummary,
            style: const TextStyle(
              color: Color(0xFF5A463F),
              fontSize: 16,
              fontWeight: FontWeight.w600,
              height: 1.45,
            ),
          ),
          if (context.citations.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Text(
              'Nguồn đã được duyệt',
              style: TextStyle(
                color: Color(0xFF5A463F),
                fontSize: 16,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            for (final citation in context.citations)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      citation.organization,
                      style: const TextStyle(
                        color: Color(0xFF5A463F),
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      citation.baseUrl,
                      style: const TextStyle(
                        color: Color(0xFF9C857C),
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
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
            Text(
              _error is String
                  ? _error! as String
                  : 'Chưa thể tải yêu cầu tư vấn.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Color(0xFF5A463F),
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 12),
            OutlinedButton(
              onPressed: AuthState.instance.userId == _accountId ? _load : null,
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
      appBar: AppBar(title: const Text('Chi tiết yêu cầu')),
      body: _error != null
          ? _buildError()
          : request == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Text(
                  request.topic,
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 8),
                Text(request.counterpartDisplayName ?? 'CareBridge'),
                const SizedBox(height: 20),
                Text(request.description),
                if (_triageContext != null) ...[
                  const SizedBox(height: 20),
                  _buildTriageContext(_triageContext!.context),
                ] else if ((_contextNotice ?? '').isNotEmpty) ...[
                  const SizedBox(height: 16),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      _contextNotice!,
                      style: const TextStyle(
                        color: Color(0xFF5A463F),
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 20),
                Chip(label: Text(_statusLabel(request.status))),
                if (request.rejectReason != null) ...[
                  const SizedBox(height: 12),
                  Text('Lý do: ${request.rejectReason}'),
                ],
                const SizedBox(height: 24),
                if (request.status == 'PENDING')
                  OutlinedButton(
                    onPressed: _busy ? null : _cancel,
                    child: const Text('Hủy yêu cầu'),
                  ),
                if (request.status == 'ACCEPTED' &&
                    request.directConversationId != null)
                  FilledButton(
                    onPressed: () => context.push(
                      '/direct-chat/${request.directConversationId}',
                    ),
                    child: const Text('Mở hội thoại'),
                  ),
              ],
            ),
    );
  }
}

String _statusLabel(String status) => switch (status) {
  'PENDING' => 'Đang chờ',
  'ACCEPTED' => 'Đã nhận',
  'REJECTED' => 'Bị từ chối',
  'CANCELLED' => 'Đã hủy',
  'EXPIRED' => 'Hết hạn',
  _ => status,
};
