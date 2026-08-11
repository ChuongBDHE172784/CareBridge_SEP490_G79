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
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

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
      ).showSnackBar(const SnackBar(content: Text('Đã gửi yêu cầu tư vấn thành công!')));
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
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0,
        centerTitle: true,
        iconTheme: const IconThemeData(color: _onSurface),
        title: const Text(
          'Tạo yêu cầu tư vấn',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: _outlineVariant, width: 1),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0A845143),
                    blurRadius: 12,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: const BoxDecoration(
                      color: _surfaceContainerLow,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.person_rounded,
                      color: _primary,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Tư vấn cùng Chuyên gia',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                            color: _onSurfaceVariant.withValues(alpha: 0.8),
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          widget.expertDisplayName,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            TextFormField(
              key: const Key('consultation-topic'),
              controller: _topicController,
              maxLength: 200,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface),
              decoration: InputDecoration(
                labelText: 'Chủ đề tư vấn',
                labelStyle: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
                hintText: 'Nhập ngắn gọn vấn đề sức khỏe mẹ/bé cần hỏi...',
                hintStyle: TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant.withValues(alpha: 0.6)),
                filled: true,
                fillColor: _surface,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _outlineVariant),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _outlineVariant),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _primary, width: 1.5),
                ),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Vui lòng nhập chủ đề'
                  : null,
            ),
            const SizedBox(height: 14),
            TextFormField(
              key: const Key('consultation-description'),
              controller: _descriptionController,
              maxLength: 2000,
              minLines: 4,
              maxLines: 7,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface),
              decoration: InputDecoration(
                labelText: 'Mô tả chi tiết nhu cầu tư vấn',
                labelStyle: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
                hintText: 'Mẹ hãy mô tả cụ thể các triệu chứng, thắc mắc hoặc thông tin cần bác sĩ hỗ trợ nhé...',
                hintStyle: TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant.withValues(alpha: 0.6)),
                filled: true,
                fillColor: _surface,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _outlineVariant),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _outlineVariant),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: const BorderSide(color: _primary, width: 1.5),
                ),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Vui lòng mô tả nhu cầu tư vấn'
                  : null,
            ),
            const SizedBox(height: 16),
            const Text(
              'Khung thời gian mong muốn (Không bắt buộc)',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _submitting
                        ? null
                        : () => _pickWindow(start: true),
                    icon: const Icon(Icons.event_available_rounded, size: 18, color: _primary),
                    label: Text(
                      _formatWindow(_windowStart, 'Thời gian bắt đầu'),
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 12),
                    ),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: _onSurface,
                      backgroundColor: _surface,
                      side: const BorderSide(color: _outlineVariant),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _submitting
                        ? null
                        : () => _pickWindow(start: false),
                    icon: const Icon(Icons.event_busy_rounded, size: 18, color: _primary),
                    label: Text(
                      _formatWindow(_windowEnd, 'Thời gian kết thúc'),
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 12),
                    ),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: _onSurface,
                      backgroundColor: _surface,
                      side: const BorderSide(color: _outlineVariant),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 28),
            ElevatedButton(
              onPressed: _submitting ? null : _submit,
              style: ElevatedButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                elevation: 0,
                minimumSize: const Size.fromHeight(52),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: Text(
                _submitting ? 'Đang gửi...' : 'Gửi yêu cầu tư vấn',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
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

