import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../expert/models/expert_availability_slot.dart';
import '../../expert/services/expert_availability_service.dart';
import '../services/consultation_request_refresh_bus.dart';
import '../services/consultation_request_service.dart';

class ConsultationRequestFormScreen extends StatefulWidget {
  final String expertProfileId;
  final String expertDisplayName;
  final ExpertAvailabilityService? availabilityService;

  const ConsultationRequestFormScreen({
    super.key,
    required this.expertProfileId,
    required this.expertDisplayName,
    this.availabilityService,
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
  late Future<List<ExpertAvailabilitySlot>> _availabilityFuture;
  ExpertAvailabilitySlot? _selectedSlot;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _clientRequestId = const Uuid().v4();
    _availabilityFuture =
        (widget.availabilityService ?? ExpertAvailabilityService.instance)
            .getPublicAvailability(widget.expertProfileId);
  }

  @override
  void dispose() {
    _topicController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_submitting || !_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      await ConsultationRequestService.instance.create(
        clientRequestId: _clientRequestId,
        expertProfileId: widget.expertProfileId,
        topic: _topicController.text.trim(),
        description: _descriptionController.text.trim(),
        preferredWindowStart: _selectedSlot?.startAt,
        preferredWindowEnd: _selectedSlot?.endAt,
      );
      ConsultationRequestRefreshBus.notify();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã gửi yêu cầu tư vấn thành công!')),
      );
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
      bottomNavigationBar: SafeArea(
        top: false,
        child: Container(
          color: _surface,
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 16),
          child: ElevatedButton(
            key: const Key('consultation-submit'),
            onPressed: _submitting ? null : _submit,
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              elevation: 0,
              minimumSize: const Size.fromHeight(52),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
            ),
            child: Text(
              _submitting ? 'Đang gửi...' : 'Gửi yêu cầu',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ),
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
          cacheExtent: 1200,
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
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              decoration: InputDecoration(
                labelText: 'Chủ đề tư vấn',
                labelStyle: const TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                ),
                hintText: 'Nhập ngắn gọn vấn đề sức khỏe mẹ/bé cần hỏi...',
                hintStyle: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant.withValues(alpha: 0.6),
                ),
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
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              decoration: InputDecoration(
                labelText: 'Mô tả chi tiết nhu cầu tư vấn',
                labelStyle: const TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                ),
                hintText:
                    'Mẹ hãy mô tả cụ thể các triệu chứng, thắc mắc hoặc thông tin cần bác sĩ hỗ trợ nhé...',
                hintStyle: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant.withValues(alpha: 0.6),
                ),
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
            _buildAvailabilityPicker(),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildAvailabilityPicker() {
    return Container(
      key: const Key('consultation-availability-picker'),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _outlineVariant),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 22,
            offset: Offset(0, 9),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const CircleAvatar(
                radius: 20,
                backgroundColor: _surfaceContainerLow,
                child: Icon(Icons.schedule_rounded, color: _primary),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Chọn ca tư vấn',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      'Không bắt buộc · Mỗi ca kéo dài 1 giờ',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (_selectedSlot != null)
                IconButton(
                  key: const Key('consultation-clear-slot'),
                  tooltip: 'Bỏ chọn ca',
                  onPressed: _submitting
                      ? null
                      : () => setState(() => _selectedSlot = null),
                  icon: const Icon(Icons.close_rounded, color: _primary),
                ),
            ],
          ),
          const SizedBox(height: 14),
          FutureBuilder<List<ExpertAvailabilitySlot>>(
            future: _availabilityFuture,
            builder: (context, snapshot) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const Center(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                );
              }
              if (snapshot.hasError) {
                return _availabilityNotice(
                  'Chưa thể tải lịch rảnh. Bạn vẫn có thể gửi yêu cầu mà không chọn ca.',
                  showRetry: true,
                );
              }
              final grouped = groupAvailabilityByLocalDate(
                snapshot.data ?? const [],
              );
              if (grouped.isEmpty) {
                return _availabilityNotice(
                  'Chuyên gia chưa có ca rảnh sắp tới. Việc chọn ca hiện là tùy chọn.',
                );
              }
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: grouped.entries.take(14).map((entry) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _formatDate(entry.key),
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: entry.value.map((slot) {
                            final selected =
                                _selectedSlot?.availabilityId ==
                                slot.availabilityId;
                            return ChoiceChip(
                              key: Key(
                                'consultation-slot-${slot.availabilityId}',
                              ),
                              selected: selected,
                              showCheckmark: false,
                              label: Text(_formatTime(slot)),
                              onSelected: _submitting
                                  ? null
                                  : (_) => setState(
                                      () => _selectedSlot = selected
                                          ? null
                                          : slot,
                                    ),
                              selectedColor: _primary,
                              backgroundColor: _surfaceContainerLow,
                              side: BorderSide(
                                color: selected ? _primary : _outlineVariant,
                              ),
                              shape: const StadiumBorder(),
                              labelStyle: TextStyle(
                                fontFamily: 'Lexend',
                                fontWeight: FontWeight.w700,
                                color: selected ? Colors.white : _onSurface,
                              ),
                            );
                          }).toList(),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _availabilityNotice(String text, {bool showRetry = false}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline_rounded, color: _primary),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
                height: 1.35,
              ),
            ),
          ),
          if (showRetry)
            IconButton(
              tooltip: 'Tải lại lịch',
              onPressed: () => setState(() {
                _availabilityFuture =
                    (widget.availabilityService ??
                            ExpertAvailabilityService.instance)
                        .getPublicAvailability(widget.expertProfileId);
              }),
              icon: const Icon(Icons.refresh_rounded, color: _primary),
            ),
        ],
      ),
    );
  }

  String _formatDate(DateTime date) {
    final weekdays = [
      'Thứ Hai',
      'Thứ Ba',
      'Thứ Tư',
      'Thứ Năm',
      'Thứ Sáu',
      'Thứ Bảy',
      'Chủ Nhật',
    ];
    return '${weekdays[date.weekday - 1]}, '
        '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}';
  }

  String _formatTime(ExpertAvailabilitySlot slot) =>
      '${slot.startAt.hour.toString().padLeft(2, '0')}:00–'
      '${slot.endAt.hour.toString().padLeft(2, '0')}:00';
}
