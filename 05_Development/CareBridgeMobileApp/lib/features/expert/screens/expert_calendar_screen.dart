import 'package:flutter/material.dart';

import '../../../core/network/api_client.dart';
import '../services/expert_home_service.dart';

abstract class ExpertCalendarApi {
  Future<dynamic> get(String path);

  Future<dynamic> post(String path, Map<String, dynamic> body);

  Future<dynamic> put(String path, Map<String, dynamic> body);

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
  Future<dynamic> put(String path, Map<String, dynamic> body) =>
      apiPut(path, body);

  @override
  Future<dynamic> delete(String path) => apiDelete(path);
}

enum AvailabilityApplyScope {
  selectedDay,
  week,
  month,
  selectedWeekdays,
  selectedMonthDays,
}

class AvailabilityTimeRange {
  const AvailabilityTimeRange(this.start, this.end);

  final DateTime start;
  final DateTime end;
}

class ExpertCalendarScreen extends StatefulWidget {
  final ExpertCalendarApi? api;

  const ExpertCalendarScreen({super.key, this.api});

  @override
  State<ExpertCalendarScreen> createState() => _ExpertCalendarScreenState();
}

class _ExpertCalendarScreenState extends State<ExpertCalendarScreen> {
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  static const _outlineVariant = Color(0xFFE5D3CA);

  ExpertCalendarApi get _api => widget.api ?? const _DefaultExpertCalendarApi();

  List<Map<String, dynamic>> _slots = const [];
  late DateTime _visibleMonth;
  late DateTime _selectedDate;
  bool _loading = true;
  bool _saving = false;
  String? _error;
  int _fetchGeneration = 0;

  @override
  void initState() {
    super.initState();
    final today = _dateOnly(DateTime.now());
    _visibleMonth = DateTime(today.year, today.month);
    _selectedDate = today;
    _fetchAvailability();
  }

  Future<void> _fetchAvailability({bool showLoader = true}) async {
    final generation = ++_fetchGeneration;
    if (mounted && showLoader) {
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
        _error = null;
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

  Future<void> _openDayEditor(DateTime date) async {
    final today = _dateOnly(DateTime.now());
    if (date.isBefore(today) || _saving) return;
    setState(() => _selectedDate = date);

    final selected = availableHoursForDate(_slots, date).toSet();
    final busy = busyHoursForDate(_slots, date).toSet();
    var scope = AvailabilityApplyScope.selectedDay;
    final selectedWeekdays = <int>{date.weekday};
    final selectedMonthDays = <int>{date.day};
    String? validationError;

    final command = await showModalBottomSheet<_AvailabilityEditCommand>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => StatefulBuilder(
        builder: (context, setModalState) {
          final now = DateTime.now();
          final targets = resolveAvailabilityDates(
            anchor: date,
            scope: scope,
            weekdays: selectedWeekdays,
            monthDays: selectedMonthDays,
            today: today,
          );
          final canSave =
              (scope != AvailabilityApplyScope.selectedWeekdays ||
                  selectedWeekdays.isNotEmpty) &&
              (scope != AvailabilityApplyScope.selectedMonthDays ||
                  selectedMonthDays.isNotEmpty);

          return DraggableScrollableSheet(
            initialChildSize: 0.88,
            minChildSize: 0.62,
            maxChildSize: 0.96,
            expand: false,
            builder: (context, scrollController) => Material(
              color: _surface,
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(28),
              ),
              clipBehavior: Clip.antiAlias,
              child: Column(
                children: [
                  const SizedBox(height: 10),
                  Container(
                    width: 42,
                    height: 4,
                    decoration: BoxDecoration(
                      color: _outlineVariant,
                      borderRadius: BorderRadius.circular(99),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 12, 12, 10),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${_weekdayLongLabels[date.weekday - 1]}, '
                                '${_two(date.day)} tháng ${_two(date.month)}',
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 19,
                                  fontWeight: FontWeight.w700,
                                  color: _onSurface,
                                ),
                              ),
                              const SizedBox(height: 3),
                              const Text(
                                'Chọn các ca rảnh, mỗi ca kéo dài 1 giờ',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 12,
                                  color: _onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          tooltip: 'Đóng',
                          onPressed: () => Navigator.pop(context),
                          icon: const Icon(Icons.close_rounded),
                        ),
                      ],
                    ),
                  ),
                  const Divider(height: 1, color: _outlineVariant),
                  Expanded(
                    child: ListView(
                      controller: scrollController,
                      padding: const EdgeInsets.fromLTRB(20, 18, 20, 12),
                      children: [
                        Row(
                          children: [
                            const Expanded(
                              child: Text(
                                'Khung giờ làm việc',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontWeight: FontWeight.w700,
                                  color: _primary,
                                ),
                              ),
                            ),
                            TextButton(
                              key: const Key('availability-select-all'),
                              onPressed: () => setModalState(() {
                                for (var hour = 7; hour < 21; hour++) {
                                  if (isSelectableAvailabilityHour(
                                        date,
                                        hour,
                                        now: now,
                                      ) &&
                                      !busy.contains(hour)) {
                                    selected.add(hour);
                                  }
                                }
                                validationError = null;
                              }),
                              child: const Text('Chọn tất cả'),
                            ),
                            TextButton(
                              key: const Key('availability-clear-all'),
                              onPressed: () => setModalState(() {
                                selected.removeWhere(
                                  (hour) => isSelectableAvailabilityHour(
                                    date,
                                    hour,
                                    now: now,
                                  ),
                                );
                              }),
                              child: const Text('Bỏ chọn'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        GridView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: 14,
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                childAspectRatio: 3.15,
                                crossAxisSpacing: 10,
                                mainAxisSpacing: 10,
                              ),
                          itemBuilder: (context, index) {
                            final hour = index + 7;
                            final isBusy = busy.contains(hour);
                            final enabled =
                                !isBusy &&
                                isSelectableAvailabilityHour(
                                  date,
                                  hour,
                                  now: now,
                                );
                            final isSelected = selected.contains(hour);
                            return InkWell(
                              key: Key('availability-hour-$hour'),
                              borderRadius: BorderRadius.circular(15),
                              onTap: enabled
                                  ? () => setModalState(() {
                                      if (!selected.add(hour)) {
                                        selected.remove(hour);
                                      }
                                      validationError = null;
                                    })
                                  : null,
                              child: AnimatedContainer(
                                duration: const Duration(milliseconds: 160),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                ),
                                decoration: BoxDecoration(
                                  color: isBusy
                                      ? _surfaceContainerLow
                                      : isSelected
                                      ? _primary
                                      : _surface,
                                  borderRadius: BorderRadius.circular(15),
                                  border: Border.all(
                                    color: isSelected
                                        ? _primary
                                        : _outlineVariant,
                                  ),
                                ),
                                child: Row(
                                  children: [
                                    Icon(
                                      isBusy
                                          ? Icons.lock_clock_rounded
                                          : isSelected
                                          ? Icons.check_circle_rounded
                                          : Icons.schedule_rounded,
                                      size: 18,
                                      color: isSelected
                                          ? Colors.white
                                          : enabled
                                          ? _primary
                                          : _onSurfaceVariant.withValues(
                                              alpha: 0.45,
                                            ),
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        '${_two(hour)}:00–${_two(hour + 1)}:00',
                                        style: TextStyle(
                                          fontFamily: 'Lexend',
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                          color: isSelected
                                              ? Colors.white
                                              : enabled
                                              ? _onSurface
                                              : _onSurfaceVariant.withValues(
                                                  alpha: 0.55,
                                                ),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                        if (busy.isNotEmpty) ...[
                          const SizedBox(height: 10),
                          const Text(
                            'Ca có biểu tượng khóa đã có lịch tư vấn.',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              color: _onSurfaceVariant,
                            ),
                          ),
                        ],
                        const SizedBox(height: 22),
                        const Text(
                          'Áp dụng lịch này cho',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w700,
                            color: _primary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        DropdownButtonFormField<AvailabilityApplyScope>(
                          key: const Key('availability-apply-scope'),
                          initialValue: scope,
                          decoration: InputDecoration(
                            filled: true,
                            fillColor: _surfaceContainerLow,
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(16),
                              borderSide: BorderSide.none,
                            ),
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: AvailabilityApplyScope.selectedDay,
                              child: Text('Chỉ ngày này'),
                            ),
                            DropdownMenuItem(
                              value: AvailabilityApplyScope.week,
                              child: Text('Tất cả ngày trong tuần'),
                            ),
                            DropdownMenuItem(
                              value: AvailabilityApplyScope.month,
                              child: Text('Tất cả ngày trong tháng'),
                            ),
                            DropdownMenuItem(
                              value: AvailabilityApplyScope.selectedWeekdays,
                              child: Text('Các thứ trong tuần tùy chọn'),
                            ),
                            DropdownMenuItem(
                              value: AvailabilityApplyScope.selectedMonthDays,
                              child: Text('Các ngày trong tháng tùy chọn'),
                            ),
                          ],
                          onChanged: (value) => setModalState(() {
                            scope = value ?? AvailabilityApplyScope.selectedDay;
                            validationError = null;
                          }),
                        ),
                        if (scope ==
                            AvailabilityApplyScope.selectedWeekdays) ...[
                          const SizedBox(height: 12),
                          Wrap(
                            spacing: 7,
                            runSpacing: 7,
                            children: List.generate(7, (index) {
                              final weekday = index + 1;
                              return FilterChip(
                                key: Key('availability-weekday-$weekday'),
                                label: Text(_weekdayLabels[index]),
                                selected: selectedWeekdays.contains(weekday),
                                onSelected: (value) => setModalState(() {
                                  value
                                      ? selectedWeekdays.add(weekday)
                                      : selectedWeekdays.remove(weekday);
                                }),
                              );
                            }),
                          ),
                        ],
                        if (scope ==
                            AvailabilityApplyScope.selectedMonthDays) ...[
                          const SizedBox(height: 12),
                          Wrap(
                            spacing: 6,
                            runSpacing: 6,
                            children: List.generate(
                              DateUtils.getDaysInMonth(date.year, date.month),
                              (index) {
                                final day = index + 1;
                                return FilterChip(
                                  key: Key('availability-month-day-$day'),
                                  label: Text('$day'),
                                  selected: selectedMonthDays.contains(day),
                                  onSelected: (value) => setModalState(() {
                                    value
                                        ? selectedMonthDays.add(day)
                                        : selectedMonthDays.remove(day);
                                  }),
                                );
                              },
                            ),
                          ),
                        ],
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: _surfaceContainerLow,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.auto_awesome_rounded,
                                color: _primary,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  '${selected.length} ca/ngày • ${targets.length} ngày sẽ được cập nhật',
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                    color: _onSurfaceVariant,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        if (validationError != null) ...[
                          const SizedBox(height: 10),
                          Text(
                            validationError!,
                            key: const Key('availability-validation-error'),
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              color: Color(0xFFBA1A1A),
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  SafeArea(
                    top: false,
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 10, 20, 14),
                      child: SizedBox(
                        width: double.infinity,
                        height: 52,
                        child: FilledButton.icon(
                          key: const Key('availability-save'),
                          onPressed: canSave
                              ? () => Navigator.pop(
                                  context,
                                  _AvailabilityEditCommand(
                                    date: date,
                                    hours: selected,
                                    scope: scope,
                                    weekdays: selectedWeekdays,
                                    monthDays: selectedMonthDays,
                                  ),
                                )
                              : () => setModalState(() {
                                  validationError =
                                      'Hãy chọn ít nhất một ngày áp dụng.';
                                }),
                          style: FilledButton.styleFrom(
                            backgroundColor: _primary,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(18),
                            ),
                          ),
                          icon: const Icon(Icons.save_rounded),
                          label: Text(
                            targets.length == 1
                                ? 'Lưu lịch rảnh'
                                : 'Lưu cho ${targets.length} ngày',
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );

    if (command == null || !mounted) return;
    await _applyAvailability(command);
  }

  Future<void> _applyAvailability(_AvailabilityEditCommand command) async {
    final dates = resolveAvailabilityDates(
      anchor: command.date,
      scope: command.scope,
      weekdays: command.weekdays,
      monthDays: command.monthDays,
      today: _dateOnly(DateTime.now()),
    );
    if (dates.isEmpty) return;

    setState(() => _saving = true);
    try {
      final sortedHours = command.hours.toList()..sort();
      await _api
          .put('/api/v1/expert/availability/batch', {
            'targetDates': dates.map(_routeDate).toList(),
            'timeZone': _localUtcOffset(),
            'channelType': 'ONLINE_CHAT',
            'slots': sortedHours
                .map((hour) => {'startTime': '${_two(hour)}:00'})
                .toList(),
          })
          .timeout(const Duration(seconds: 30));
      await _fetchAvailability(showLoader: false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            dates.length == 1
                ? 'Đã cập nhật lịch rảnh'
                : 'Đã đồng bộ lịch rảnh cho ${dates.length} ngày',
          ),
        ),
      );
    } catch (error) {
      await _fetchAvailability(showLoader: false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            friendlyApiError(
              error,
              fallback:
                  'Chưa thể đồng bộ toàn bộ lịch. Dữ liệu đã được tải lại.',
            ),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _changeMonth(int delta) {
    setState(() {
      _visibleMonth = DateTime(_visibleMonth.year, _visibleMonth.month + delta);
      final today = _dateOnly(DateTime.now());
      final first = DateTime(_visibleMonth.year, _visibleMonth.month);
      _selectedDate = _sameMonth(today, _visibleMonth) ? today : first;
    });
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = ModalRoute.of(context)?.canPop ?? false;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Container(
              width: double.infinity,
              decoration: const BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.vertical(
                  bottom: Radius.circular(24),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Color(0x0A845143),
                    blurRadius: 16,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              padding: EdgeInsets.fromLTRB(12, topInset + 8, 12, 12),
              child: Row(
                children: [
                  if (canPop)
                    IconButton(
                      tooltip: 'Quay lại',
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(
                        Icons.arrow_back_ios_new_rounded,
                        color: _primary,
                      ),
                    )
                  else
                    const SizedBox(width: 8),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Lịch rảnh của tôi',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        Text(
                          'Chạm vào một ngày để chỉnh sửa',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    key: const Key('calendar-refresh'),
                    tooltip: 'Làm mới lịch rảnh',
                    onPressed: _loading || _saving ? null : _fetchAvailability,
                    icon: const Icon(Icons.refresh_rounded, color: _primary),
                  ),
                ],
              ),
            ),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
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
              const Icon(
                Icons.cloud_off_rounded,
                size: 48,
                color: _onSurfaceVariant,
              ),
              const SizedBox(height: 12),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                ),
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

    final dates = calendarDatesForMonth(_visibleMonth);
    final today = _dateOnly(DateTime.now());
    return Stack(
      children: [
        RefreshIndicator(
          color: _primary,
          onRefresh: _fetchAvailability,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(14, 16, 14, 28),
            children: [
              Container(
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: _outlineVariant),
                ),
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(8, 8, 8, 4),
                      child: Row(
                        children: [
                          IconButton(
                            key: const Key('calendar-previous-month'),
                            tooltip: 'Tháng trước',
                            onPressed: _saving ? null : () => _changeMonth(-1),
                            icon: const Icon(
                              Icons.chevron_left_rounded,
                              color: _primary,
                            ),
                          ),
                          Expanded(
                            child: Text(
                              'Tháng ${_two(_visibleMonth.month)}, '
                              '${_visibleMonth.year}',
                              key: const Key('calendar-month-label'),
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 17,
                                fontWeight: FontWeight.w700,
                                color: _onSurface,
                              ),
                            ),
                          ),
                          IconButton(
                            key: const Key('calendar-next-month'),
                            tooltip: 'Tháng sau',
                            onPressed: _saving ? null : () => _changeMonth(1),
                            icon: const Icon(
                              Icons.chevron_right_rounded,
                              color: _primary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 9),
                      child: Row(
                        children: _weekdayLabels
                            .map(
                              (label) => Expanded(
                                child: Center(
                                  child: Text(
                                    label,
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 11,
                                      fontWeight: FontWeight.w700,
                                      color: _onSurfaceVariant,
                                    ),
                                  ),
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    ),
                    const SizedBox(height: 5),
                    GridView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(8, 0, 8, 10),
                      itemCount: dates.length,
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 7,
                            childAspectRatio: 0.77,
                            crossAxisSpacing: 3,
                            mainAxisSpacing: 3,
                          ),
                      itemBuilder: (context, index) {
                        final date = dates[index];
                        final inMonth = _sameMonth(date, _visibleMonth);
                        final inPast = date.isBefore(today);
                        final selected = _sameDate(date, _selectedDate);
                        final hours = availableHoursForDate(_slots, date);
                        final busy = busyHoursForDate(_slots, date);
                        return InkWell(
                          key: Key('calendar-day-${_routeDate(date)}'),
                          borderRadius: BorderRadius.circular(13),
                          onTap: !inMonth || inPast || _saving
                              ? null
                              : () => _openDayEditor(date),
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 160),
                            padding: const EdgeInsets.symmetric(
                              vertical: 6,
                              horizontal: 2,
                            ),
                            decoration: BoxDecoration(
                              color: selected
                                  ? _primary.withValues(alpha: 0.11)
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(13),
                              border: Border.all(
                                color: selected ? _primary : Colors.transparent,
                              ),
                            ),
                            child: Column(
                              children: [
                                Container(
                                  width: 28,
                                  height: 28,
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: _sameDate(date, today)
                                        ? _primary
                                        : Colors.transparent,
                                  ),
                                  child: Text(
                                    '${date.day}',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      fontWeight: FontWeight.w700,
                                      color: !inMonth || inPast
                                          ? _onSurfaceVariant.withValues(
                                              alpha: 0.35,
                                            )
                                          : _sameDate(date, today)
                                          ? Colors.white
                                          : _onSurface,
                                    ),
                                  ),
                                ),
                                const Spacer(),
                                if (hours.isNotEmpty || busy.isNotEmpty)
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      if (hours.isNotEmpty)
                                        Container(
                                          width: 6,
                                          height: 6,
                                          decoration: const BoxDecoration(
                                            color: _primaryContainer,
                                            shape: BoxShape.circle,
                                          ),
                                        ),
                                      if (busy.isNotEmpty) ...[
                                        if (hours.isNotEmpty)
                                          const SizedBox(width: 3),
                                        Container(
                                          width: 6,
                                          height: 6,
                                          decoration: const BoxDecoration(
                                            color: Color(0xFF5B8A72),
                                            shape: BoxShape.circle,
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                const SizedBox(height: 2),
                                Text(
                                  hours.isEmpty ? '' : '${hours.length} ca',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 8,
                                    fontWeight: FontWeight.w600,
                                    color: inMonth
                                        ? _primary
                                        : _onSurfaceVariant.withValues(
                                            alpha: 0.25,
                                          ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _surfaceContainerLow,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.touch_app_rounded, color: _primary),
                    SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Chọn một ngày trên lịch, sau đó tích các ca từ 07:00 đến 21:00. Lịch có thể áp dụng nhanh cho tuần, tháng hoặc nhóm ngày tùy chọn.',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          height: 1.45,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        if (_saving)
          Positioned.fill(
            child: ColoredBox(
              color: _canvas.withValues(alpha: 0.78),
              child: const Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: _primary),
                    SizedBox(height: 14),
                    Text(
                      'Đang đồng bộ lịch rảnh…',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _AvailabilityEditCommand {
  const _AvailabilityEditCommand({
    required this.date,
    required this.hours,
    required this.scope,
    required this.weekdays,
    required this.monthDays,
  });

  final DateTime date;
  final Set<int> hours;
  final AvailabilityApplyScope scope;
  final Set<int> weekdays;
  final Set<int> monthDays;
}

const _weekdayLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const _weekdayLongLabels = [
  'Thứ Hai',
  'Thứ Ba',
  'Thứ Tư',
  'Thứ Năm',
  'Thứ Sáu',
  'Thứ Bảy',
  'Chủ Nhật',
];

List<DateTime> calendarDatesForMonth(DateTime month) {
  final first = DateTime(month.year, month.month);
  final gridStart = first.subtract(Duration(days: first.weekday - 1));
  return List.generate(42, (index) => gridStart.add(Duration(days: index)));
}

List<DateTime> resolveAvailabilityDates({
  required DateTime anchor,
  required AvailabilityApplyScope scope,
  Set<int> weekdays = const {},
  Set<int> monthDays = const {},
  DateTime? today,
}) {
  final normalizedAnchor = _dateOnly(anchor);
  final lowerBound = _dateOnly(today ?? DateTime.now());
  late Iterable<DateTime> dates;
  switch (scope) {
    case AvailabilityApplyScope.selectedDay:
      dates = [normalizedAnchor];
    case AvailabilityApplyScope.week:
      final monday = normalizedAnchor.subtract(
        Duration(days: normalizedAnchor.weekday - 1),
      );
      dates = List.generate(7, (index) => monday.add(Duration(days: index)));
    case AvailabilityApplyScope.month:
      dates = _allDatesInMonth(normalizedAnchor);
    case AvailabilityApplyScope.selectedWeekdays:
      dates = _allDatesInMonth(
        normalizedAnchor,
      ).where((date) => weekdays.contains(date.weekday));
    case AvailabilityApplyScope.selectedMonthDays:
      dates = _allDatesInMonth(
        normalizedAnchor,
      ).where((date) => monthDays.contains(date.day));
  }
  final result =
      dates.where((date) => !date.isBefore(lowerBound)).toSet().toList()
        ..sort();
  return result;
}

List<int> availableHoursForDate(
  List<Map<String, dynamic>> slots,
  DateTime date,
) => _hoursForDate(slots, date, available: true);

List<int> busyHoursForDate(List<Map<String, dynamic>> slots, DateTime date) =>
    _hoursForDate(slots, date, available: false);

List<int> _hoursForDate(
  List<Map<String, dynamic>> slots,
  DateTime date, {
  required bool available,
}) {
  final result = <int>[];
  for (var hour = 7; hour < 21; hour++) {
    final hourStart = DateTime(date.year, date.month, date.day, hour);
    final hourEnd = hourStart.add(const Duration(hours: 1));
    final covered = slots.any((row) {
      final status = '${row['status'] ?? 'AVAILABLE'}'.toUpperCase();
      if ((status == 'AVAILABLE') != available) return false;
      final start = DateTime.tryParse('${row['startAt']}')?.toLocal();
      final end = DateTime.tryParse('${row['endAt']}')?.toLocal();
      return start != null &&
          end != null &&
          !start.isAfter(hourStart) &&
          !end.isBefore(hourEnd);
    });
    if (covered) result.add(hour);
  }
  return result;
}

List<AvailabilityTimeRange> mergeAvailabilityHours(
  DateTime date,
  Set<int> hours,
) {
  final sorted = hours.where((hour) => hour >= 7 && hour < 21).toList()..sort();
  if (sorted.isEmpty) return const [];
  final ranges = <AvailabilityTimeRange>[];
  var start = sorted.first;
  var previous = start;
  for (final hour in sorted.skip(1)) {
    if (hour == previous + 1) {
      previous = hour;
      continue;
    }
    ranges.add(
      AvailabilityTimeRange(
        DateTime(date.year, date.month, date.day, start),
        DateTime(date.year, date.month, date.day, previous + 1),
      ),
    );
    start = hour;
    previous = hour;
  }
  ranges.add(
    AvailabilityTimeRange(
      DateTime(date.year, date.month, date.day, start),
      DateTime(date.year, date.month, date.day, previous + 1),
    ),
  );
  return ranges;
}

bool isSelectableAvailabilityHour(
  DateTime date,
  int hour, {
  required DateTime now,
}) {
  if (hour < 7 || hour >= 21) return false;
  return DateTime(date.year, date.month, date.day, hour).isAfter(now);
}

DateTime combineLocalDateAndTime(DateTime date, TimeOfDay time) =>
    DateTime(date.year, date.month, date.day, time.hour, time.minute);

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

Iterable<DateTime> _allDatesInMonth(DateTime date) sync* {
  final count = DateUtils.getDaysInMonth(date.year, date.month);
  for (var day = 1; day <= count; day++) {
    yield DateTime(date.year, date.month, day);
  }
}

DateTime _dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

bool _sameDate(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month && a.day == b.day;

bool _sameMonth(DateTime a, DateTime b) =>
    a.year == b.year && a.month == b.month;

String _routeDate(DateTime date) =>
    '${date.year}-${_two(date.month)}-${_two(date.day)}';

String _two(int value) => value.toString().padLeft(2, '0');

String _localUtcOffset() {
  final offset = DateTime.now().timeZoneOffset;
  final sign = offset.isNegative ? '-' : '+';
  final absolute = offset.abs();
  return '$sign${_two(absolute.inHours)}:${_two(absolute.inMinutes.remainder(60))}';
}
