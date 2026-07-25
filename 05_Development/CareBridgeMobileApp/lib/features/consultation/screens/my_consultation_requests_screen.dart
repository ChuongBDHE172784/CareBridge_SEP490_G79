import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/consultation_request.dart';
import '../services/consultation_request_service.dart';

class MyConsultationRequestsScreen extends StatefulWidget {
  const MyConsultationRequestsScreen({super.key});

  @override
  State<MyConsultationRequestsScreen> createState() =>
      _MyConsultationRequestsScreenState();
}

class _MyConsultationRequestsScreenState
    extends State<MyConsultationRequestsScreen> {
  static const _filters = <String, String?>{
    'Tất cả': null,
    'Đang chờ': 'PENDING',
    'Đã nhận': 'ACCEPTED',
    'Bị từ chối': 'REJECTED',
    'Đã hủy': 'CANCELLED',
    'Hết hạn': 'EXPIRED',
  };

  String? _status;
  ConsultationRequestPage? _page;
  Object? _error;
  bool _loading = false;
  int _generation = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final generation = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await ConsultationRequestService.instance.listMine(
        status: _status,
      );
      if (!mounted || generation != _generation) return;
      setState(() => _page = page);
    } catch (error) {
      if (!mounted || generation != _generation) return;
      setState(() => _error = error);
    } finally {
      if (mounted && generation == _generation) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _page?.items ?? const <ConsultationRequestSummary>[];
    return Scaffold(
      appBar: AppBar(title: const Text('Yêu cầu tư vấn của tôi')),
      body: Column(
        children: [
          SizedBox(
            height: 54,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              children: _filters.entries
                  .map(
                    (entry) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(entry.key),
                        selected: _status == entry.value,
                        onSelected: (_) {
                          setState(() => _status = entry.value);
                          _load();
                        },
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
          Expanded(
            child: _loading && _page == null
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                ? _ErrorState(onRetry: _load)
                : items.isEmpty
                ? const Center(child: Text('Bạn chưa có yêu cầu tư vấn nào'))
                : RefreshIndicator(
                    onRefresh: _load,
                    child: ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: items.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final item = items[index];
                        return Card(
                          child: ListTile(
                            key: Key('consultation-${item.id}'),
                            title: Text(item.topic),
                            subtitle: Text(
                              '${item.counterpartDisplayName ?? 'Chuyên gia'} • ${_statusLabel(item.status)}',
                            ),
                            trailing: const Icon(Icons.chevron_right),
                            onTap: () => context.push(
                              '/consultation-requests/${item.id}',
                            ),
                          ),
                        );
                      },
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  final VoidCallback onRetry;
  const _ErrorState({required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Không thể tải danh sách yêu cầu'),
          const SizedBox(height: 8),
          OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
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
