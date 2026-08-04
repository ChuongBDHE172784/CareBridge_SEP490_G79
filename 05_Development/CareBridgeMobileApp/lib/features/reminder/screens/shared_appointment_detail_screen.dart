import 'package:flutter/material.dart';

import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

/// Read-only FAMILY appointment detail reached from the shared calendar or a
/// care-group appointment notification.
class SharedAppointmentDetailScreen extends StatefulWidget {
  const SharedAppointmentDetailScreen({
    super.key,
    required this.careGroupId,
    required this.appointmentId,
    this.loader,
  });

  final String careGroupId;
  final String appointmentId;
  final Future<Reminder> Function(String groupId, String appointmentId)? loader;

  @override
  State<SharedAppointmentDetailScreen> createState() =>
      _SharedAppointmentDetailScreenState();
}

class _SharedAppointmentDetailScreenState
    extends State<SharedAppointmentDetailScreen> {
  Reminder? _appointment;
  String? _error;
  bool _loading = true;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(covariant SharedAppointmentDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.careGroupId != widget.careGroupId ||
        oldWidget.appointmentId != widget.appointmentId) {
      _load();
    }
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final appointment =
          await (widget.loader?.call(
                widget.careGroupId,
                widget.appointmentId,
              ) ??
              ReminderService.instance.getSharedAppointment(
                widget.careGroupId,
                widget.appointmentId,
              ));
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _appointment = appointment;
        _loading = false;
      });
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải lịch hẹn được chia sẻ.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Chi tiết lịch hẹn')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _buildError()
          : _buildDetail(_appointment!),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.icon(
              key: const Key('shared-appointment-retry'),
              onPressed: _load,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetail(Reminder appointment) {
    final localTime = appointment.scheduledAt.toLocal();
    final date =
        '${localTime.day.toString().padLeft(2, '0')}/'
        '${localTime.month.toString().padLeft(2, '0')}/'
        '${localTime.year} ${localTime.hour.toString().padLeft(2, '0')}:'
        '${localTime.minute.toString().padLeft(2, '0')}';
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const Icon(Icons.event_available_rounded, size: 52),
        const SizedBox(height: 16),
        Text(
          appointment.title,
          key: const Key('shared-appointment-title'),
          style: Theme.of(context).textTheme.headlineSmall,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 20),
        ListTile(
          leading: const Icon(Icons.schedule),
          title: const Text('Thời gian'),
          subtitle: Text(date),
        ),
        ListTile(
          leading: const Icon(Icons.info_outline),
          title: const Text('Trạng thái'),
          subtitle: Text(appointment.status.displayLabel),
        ),
        if (appointment.notificationOffsetsMinutes.isNotEmpty)
          ListTile(
            leading: const Icon(Icons.notifications_none),
            title: const Text('Nhắc trước'),
            subtitle: Text(
              appointment.notificationOffsetsMinutes
                  .map(_formatOffset)
                  .join(', '),
            ),
          ),
        const SizedBox(height: 12),
        const Text(
          'Bạn đang xem dữ liệu được chia sẻ ở chế độ chỉ xem.',
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  String _formatOffset(int minutes) {
    if (minutes == 0) return 'đúng giờ';
    final absolute = minutes.abs();
    final unit = absolute == 1 ? 'phút' : 'phút';
    return minutes < 0 ? '$absolute $unit trước' : '$absolute $unit sau';
  }
}
