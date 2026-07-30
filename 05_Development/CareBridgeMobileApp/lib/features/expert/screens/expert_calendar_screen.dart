import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/network/api_client.dart';
import '../services/expert_home_service.dart';

abstract class ExpertCalendarApi {
  Future<dynamic> get(String path);

  Future<dynamic> post(String path, Map<String, dynamic> body);

  Future<dynamic> delete(String path);
}

class _DefaultExpertCalendarApi implements ExpertCalendarApi {
  const _DefaultExpertCalendarApi();

  @override
  Future<dynamic> get(String path) => apiGet(path);

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) =>
      apiPost(path, body);

  @override
  Future<dynamic> delete(String path) => apiDelete(path);
}

class ExpertCalendarScreen extends StatefulWidget {
  final ExpertCalendarApi? api;

  const ExpertCalendarScreen({super.key, this.api});

  @override
  State<ExpertCalendarScreen> createState() => _ExpertCalendarScreenState();
}

class _ExpertCalendarScreenState extends State<ExpertCalendarScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _outline = Color(0xFF84736F);

  ExpertCalendarApi get _api => widget.api ?? const _DefaultExpertCalendarApi();

  List<Map<String, dynamic>> _slots = const [];
  bool _loading = true;
  String? _error;
  int _fetchGeneration = 0;
  final Set<String> _deletingIds = {};

  @override
  void initState() {
    super.initState();
    _fetchAvailability();
  }

  Future<void> _fetchAvailability() async {
    final generation = ++_fetchGeneration;
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final response = await _api.get('/api/v1/expert/availability/me');
      final rawRows = response is Map
          ? (response['data'] ?? response['content'])
          : null;
      final rows = rawRows is List
          ? rawRows.whereType<Map>().map(Map<String, dynamic>.from).toList()
          : <Map<String, dynamic>>[];
      if (!mounted || generation != _fetchGeneration) return;
      setState(() {
        _slots = rows;
        _loading = false;
      });
    } catch (error) {
      if (!mounted || generation != _fetchGeneration) return;
      setState(() {
        _error = friendlyApiError(
          error,
          fallback: 'Không thể tải lịch rảnh. Kiểm tra kết nối và thử lại.',
        );
        _loading = false;
      });
    }
  }

  Future<void> _deleteSlot(String id) async {
    if (_deletingIds.contains(id)) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Xóa khung giờ'),
        content: const Text('Bạn có chắc muốn xóa khung giờ rảnh này?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (confirm != true || !mounted) return;

    setState(() => _deletingIds.add(id));
    try {
      await _api
          .delete('/api/v1/expert/availability/$id')
          .timeout(const Duration(seconds: 15));
      if (!mounted) return;
      await _fetchAvailability();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Đã xóa khung giờ rảnh')));
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            friendlyApiError(
              error,
              fallback: 'Không thể xóa khung giờ. Vui lòng thử lại.',
            ),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _deletingIds.remove(id));
    }
  }

  Future<void> _showAddSlotDialog() async {
    final today = _dateOnly(DateTime.now());
    var selectedDate = today.add(const Duration(days: 1));
    var startTime = const TimeOfDay(hour: 8, minute: 0);
    var endTime = const TimeOfDay(hour: 12, minute: 0);
    String? selectedPreset = 'morning';
    String? validationError;
    var submitting = false;

    final created = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: _surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            final start = combineLocalDateAndTime(selectedDate, startTime);
            final end = combineLocalDateAndTime(selectedDate, endTime);

            void choosePreset(_AvailabilityPreset preset) {
              setModalState(() {
                selectedPreset = preset.id;
                startTime = preset.start;
                endTime = preset.end;
                validationError = null;
              });
            }

            Future<void> chooseDate() async {
              final picked = await showDatePicker(
                context: context,
                initialDate: selectedDate,
                firstDate: today,
                lastDate: today.add(const Duration(days: 90)),
              );
              if (picked == null || !context.mounted) return;
              setModalState(() {
                selectedDate = _dateOnly(picked);
                validationError = null;
              });
            }

            Future<void> chooseTime({required bool isStart}) async {
              final picked = await showTimePicker(
                context: context,
                initialTime: isStart ? startTime : endTime,
              );
              if (picked == null || !context.mounted) return;
              setModalState(() {
                selectedPreset = null;
                if (isStart) {
                  startTime = picked;
                } else {
                  endTime = picked;
                }
                validationError = null;
              });
            }

            Future<void> submit() async {
              final error = validateAvailabilityRange(
                start,
                end,
                now: DateTime.now(),
              );
              if (error != null) {
                setModalState(() => validationError = error);
                return;
              }
              setModalState(() {
                submitting = true;
                validationError = null;
              });
              try {
                await _api
                    .post('/api/v1/expert/availability', {
                      'startAt': start.toUtc().toIso8601String(),
                      'endAt': end.toUtc().toIso8601String(),
                      'channelType': 'ONLINE_CHAT',
                      'status': 'AVAILABLE',
                    })
                    .timeout(const Duration(seconds: 15));
                if (!context.mounted) return;
                Navigator.pop(context, true);
              } catch (error) {
                if (!context.mounted) return;
                setModalState(() {
                  submitting = false;
                  validationError = friendlyApiError(
                    error,
                    fallback: 'Không thể tạo khung giờ. Vui lòng thử lại.',
                  );
                });
              }
            }

            return SafeArea(
              top: false,
              child: SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(
                  24,
                  20,
                  24,
                  MediaQuery.viewInsetsOf(context).bottom + 24,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Expanded(
                          child: Text(
                            'Thêm khung giờ rảnh',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                              color: _onSurface,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: 'Đóng',
                          onPressed: submitting
                              ? null
                              : () => Navigator.pop(context, false),
                          icon: const Icon(Icons.close),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Chọn ngày nhanh',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: _primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: [
                        _QuickDateChip(
                          key: const Key('availability-date-today'),
                          label: 'Hôm nay',
                          date: today,
                          selectedDate: selectedDate,
                          onSelected: (date) => setModalState(() {
                            selectedDate = date;
                            validationError = null;
                          }),
                        ),
                        _QuickDateChip(
                          key: const Key('availability-date-tomorrow'),
                          label: 'Ngày mai',
                          date: today.add(const Duration(days: 1)),
                          selectedDate: selectedDate,
                          onSelected: (date) => setModalState(() {
                            selectedDate = date;
                            validationError = null;
                          }),
                        ),
                        _QuickDateChip(
                          key: const Key('availability-date-day-after'),
                          label: 'Ngày kia',
                          date: today.add(const Duration(days: 2)),
                          selectedDate: selectedDate,
                          onSelected: (date) => setModalState(() {
                            selectedDate = date;
                            validationError = null;
                          }),
                        ),
                        ActionChip(
                          key: const Key('availability-date-custom'),
                          avatar: const Icon(Icons.calendar_month, size: 18),
                          label: Text(
                            DateFormat('dd/MM/yyyy').format(selectedDate),
                          ),
                          onPressed: submitting ? null : chooseDate,
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    const Text(
                      'Ca gợi ý',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: _primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: _presets
                          .map(
                            (preset) => ChoiceChip(
                              key: Key('availability-preset-${preset.id}'),
                              label: Text(
                                '${preset.label} '
                                '(${_formatTime(preset.start)}–${_formatTime(preset.end)})',
                              ),
                              selected: selectedPreset == preset.id,
                              onSelected: submitting
                                  ? null
                                  : (_) => choosePreset(preset),
                            ),
                          )
                          .toList(),
                    ),
                    const SizedBox(height: 18),
                    const Text(
                      'Giờ tùy chỉnh',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: _primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            key: const Key('availability-start-time'),
                            onPressed: submitting
                                ? null
                                : () => chooseTime(isStart: true),
                            icon: const Icon(Icons.schedule),
                            label: Text('Từ ${_formatTime(startTime)}'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: OutlinedButton.icon(
                            key: const Key('availability-end-time'),
                            onPressed: submitting
                                ? null
                                : () => chooseTime(isStart: false),
                            icon: const Icon(Icons.schedule),
                            label: Text('Đến ${_formatTime(endTime)}'),
                          ),
                        ),
                      ],
                    ),
                    if (validationError != null) ...[
                      const SizedBox(height: 10),
                      Text(
                        validationError!,
                        key: const Key('availability-validation-error'),
                        style: const TextStyle(
                          color: Colors.red,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: FilledButton(
                        key: const Key('availability-save'),
                        onPressed: submitting ? null : submit,
                        style: FilledButton.styleFrom(
                          backgroundColor: _primary,
                        ),
                        child: submitting
                            ? const SizedBox.square(
                                dimension: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text(
                                'Lưu khung giờ',
                                style: TextStyle(fontWeight: FontWeight.w700),
                              ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );

    if (created != true || !mounted) return;
    await _fetchAvailability();
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Đã thêm khung giờ rảnh')));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        title: const Text(
          'Lịch rảnh của tôi',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.bold,
            color: _primary,
          ),
        ),
        backgroundColor: _surface,
        elevation: 0.5,
        centerTitle: true,
        actions: [
          IconButton(
            key: const Key('calendar-refresh'),
            tooltip: 'Làm mới lịch rảnh',
            onPressed: _loading ? null : _fetchAvailability,
            icon: const Icon(Icons.refresh, color: _primary),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        key: const Key('calendar-add-slot'),
        backgroundColor: _primary,
        onPressed: _showAddSlotDialog,
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text(
          'Thêm khung giờ',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_outlined, size: 48, color: _outline),
              const SizedBox(height: 12),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _outline),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: _fetchAvailability,
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }
    if (_slots.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.event_available, size: 64, color: _primaryContainer),
              SizedBox(height: 16),
              Text(
                'Chưa có khung giờ rảnh nào',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              SizedBox(height: 8),
              Text(
                'Thêm thời gian bạn có thể nhận tư vấn.',
                style: TextStyle(color: _outline),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _fetchAvailability,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
        itemCount: _slots.length,
        itemBuilder: (context, index) {
          final slot = _slots[index];
          final start = DateTime.tryParse('${slot['startAt']}')?.toLocal();
          final end = DateTime.tryParse('${slot['endAt']}')?.toLocal();
          final rawId = slot['availabilityId'] ?? slot['id'];
          if (start == null || end == null) {
            return const SizedBox.shrink();
          }
          final expired = end.isBefore(DateTime.now());
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(18),
            ),
            elevation: 1,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: expired
                          ? Colors.grey.shade200
                          : _primaryContainer.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      expired ? Icons.history : Icons.access_time_filled,
                      color: expired ? Colors.grey : _primary,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          DateFormat('dd/MM/yyyy').format(start),
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 15,
                            color: expired ? Colors.grey : _onSurface,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${DateFormat('HH:mm').format(start)} – '
                          '${DateFormat('HH:mm').format(end)}',
                          style: TextStyle(
                            color: expired ? Colors.grey : _primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          expired ? 'Đã kết thúc' : 'Sẵn sàng đặt lịch',
                          style: TextStyle(
                            color: expired ? Colors.grey : _outline,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (rawId != null)
                    _deletingIds.contains('$rawId')
                        ? const Padding(
                            padding: EdgeInsets.all(12),
                            child: SizedBox.square(
                              dimension: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          )
                        : IconButton(
                            tooltip: 'Xóa khung giờ',
                            icon: const Icon(
                              Icons.delete_outline,
                              color: Colors.redAccent,
                            ),
                            onPressed: () => _deleteSlot('$rawId'),
                          ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _QuickDateChip extends StatelessWidget {
  final String label;
  final DateTime date;
  final DateTime selectedDate;
  final ValueChanged<DateTime> onSelected;

  const _QuickDateChip({
    super.key,
    required this.label,
    required this.date,
    required this.selectedDate,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(label),
      selected: _sameDate(date, selectedDate),
      onSelected: (_) => onSelected(date),
    );
  }
}

class _AvailabilityPreset {
  final String id;
  final String label;
  final TimeOfDay start;
  final TimeOfDay end;

  const _AvailabilityPreset(this.id, this.label, this.start, this.end);
}

const _presets = [
  _AvailabilityPreset(
    'morning',
    'Ca sáng',
    TimeOfDay(hour: 8, minute: 0),
    TimeOfDay(hour: 11, minute: 30),
  ),
  _AvailabilityPreset(
    'afternoon',
    'Ca chiều',
    TimeOfDay(hour: 13, minute: 30),
    TimeOfDay(hour: 17, minute: 0),
  ),
  _AvailabilityPreset(
    'evening',
    'Ca tối',
    TimeOfDay(hour: 18, minute: 0),
    TimeOfDay(hour: 20, minute: 30),
  ),
];

DateTime combineLocalDateAndTime(DateTime date, TimeOfDay time) {
  return DateTime(date.year, date.month, date.day, time.hour, time.minute);
}

String? validateAvailabilityRange(
  DateTime start,
  DateTime end, {
  required DateTime now,
}) {
  if (!end.isAfter(start)) {
    return 'Thời gian kết thúc phải sau thời gian bắt đầu.';
  }
  if (!start.isAfter(now)) {
    return 'Khung giờ phải bắt đầu trong tương lai.';
  }
  return null;
}

DateTime _dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

bool _sameDate(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;

String _formatTime(TimeOfDay time) =>
    '${time.hour.toString().padLeft(2, '0')}:'
    '${time.minute.toString().padLeft(2, '0')}';
