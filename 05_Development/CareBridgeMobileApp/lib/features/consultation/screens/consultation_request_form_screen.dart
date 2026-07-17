import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';

class ConsultationRequestFormScreen extends StatefulWidget {
  final String expertProfileId;
  final String expertDisplayName;

  const ConsultationRequestFormScreen({
    super.key,
    required this.expertProfileId,
    required this.expertDisplayName,
  });

  @override
  State<ConsultationRequestFormScreen> createState() =>
      _ConsultationRequestFormScreenState();
}

class _ConsultationRequestFormScreenState
    extends State<ConsultationRequestFormScreen> {
  static const _primary = Color(0xFF845143);
  final _formKey = GlobalKey<FormState>();
  final _topicController = TextEditingController();
  final _descriptionController = TextEditingController();
  late final String _clientRequestId;
  DateTime? _windowStart;
  DateTime? _windowEnd;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _clientRequestId = const Uuid().v4();
  }

  @override
  void dispose() {
    _topicController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _pickWindow({required bool start}) async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: start ? (_windowStart ?? now) : (_windowEnd ?? now),
      firstDate: now,
      lastDate: now.add(const Duration(days: 90)),
    );
    if (date == null || !mounted) return;
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(
        start ? (_windowStart ?? now) : (_windowEnd ?? now),
      ),
    );
    if (time == null || !mounted) return;
    setState(() {
      final value = DateTime(
        date.year,
        date.month,
        date.day,
        time.hour,
        time.minute,
      );
      if (start) {
        _windowStart = value;
      } else {
        _windowEnd = value;
      }
    });
  }

  String? _windowError() {
    if ((_windowStart == null) != (_windowEnd == null)) {
      return 'Vui lòng chọn cả thời gian bắt đầu và kết thúc';
    }
    if (_windowStart != null && !_windowEnd!.isAfter(_windowStart!)) {
      return 'Thời gian kết thúc phải sau thời gian bắt đầu';
    }
    return null;
  }

  Future<void> _submit() async {
    if (_submitting || !_formKey.currentState!.validate()) return;
    final windowError = _windowError();
    if (windowError != null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(windowError)));
      return;
    }
    setState(() => _submitting = true);
    try {
      await ConsultationRequestService.instance.create(
        clientRequestId: _clientRequestId,
        expertProfileId: widget.expertProfileId,
        topic: _topicController.text.trim(),
        description: _descriptionController.text.trim(),
        preferredWindowStart: _windowStart,
        preferredWindowEnd: _windowEnd,
      );
      ConsultationRequestRefreshBus.notify();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã gửi yêu cầu tư vấn')));
      if (Navigator.of(context).canPop()) Navigator.of(context).pop();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Không thể gửi yêu cầu: $error')));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Yêu cầu tư vấn')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              widget.expertDisplayName,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 20),
            TextFormField(
              key: const Key('consultation-topic'),
              controller: _topicController,
              maxLength: 200,
              decoration: const InputDecoration(
                labelText: 'Chủ đề',
                border: OutlineInputBorder(),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Vui lòng nhập chủ đề'
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              key: const Key('consultation-description'),
              controller: _descriptionController,
              maxLength: 2000,
              minLines: 4,
              maxLines: 7,
              decoration: const InputDecoration(
                labelText: 'Mô tả nhu cầu tư vấn',
                border: OutlineInputBorder(),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Vui lòng mô tả nhu cầu tư vấn'
                  : null,
            ),
            const SizedBox(height: 12),
            Text(
              'Khung thời gian mong muốn (không bắt buộc)',
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _submitting
                        ? null
                        : () => _pickWindow(start: true),
                    icon: const Icon(Icons.schedule),
                    label: Text(_formatWindow(_windowStart, 'Bắt đầu')),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _submitting
                        ? null
                        : () => _pickWindow(start: false),
                    icon: const Icon(Icons.schedule_outlined),
                    label: Text(_formatWindow(_windowEnd, 'Kết thúc')),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submitting ? null : _submit,
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                minimumSize: const Size.fromHeight(52),
              ),
              child: Text(_submitting ? 'Đang gửi...' : 'Gửi yêu cầu'),
            ),
          ],
        ),
      ),
    );
  }

  String _formatWindow(DateTime? value, String fallback) {
    if (value == null) return fallback;
    return '${value.day.toString().padLeft(2, '0')}/'
        '${value.month.toString().padLeft(2, '0')} '
        '${value.hour.toString().padLeft(2, '0')}:'
        '${value.minute.toString().padLeft(2, '0')}';
  }
}
