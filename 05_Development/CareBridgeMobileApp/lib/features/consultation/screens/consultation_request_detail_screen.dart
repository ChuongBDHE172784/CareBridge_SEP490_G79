import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/consultation_request.dart';
import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';

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
  Object? _error;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final request = await ConsultationRequestService.instance.getById(
        widget.requestId,
      );
      if (mounted) setState(() => _request = request);
    } catch (error) {
      if (mounted) setState(() => _error = error);
    }
  }

  Future<void> _cancel() async {
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
      if (mounted) setState(() => _request = request);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final request = _request;
    return Scaffold(
      appBar: AppBar(title: const Text('Chi tiết yêu cầu')),
      body: _error != null
          ? Center(
              child: OutlinedButton(
                onPressed: _load,
                child: const Text('Thử lại'),
              ),
            )
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
