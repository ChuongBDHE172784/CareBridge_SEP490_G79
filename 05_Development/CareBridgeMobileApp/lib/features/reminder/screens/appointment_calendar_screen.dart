import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class AppointmentCalendarScreen extends StatefulWidget {
  const AppointmentCalendarScreen({
    super.key,
    this.reminderLoader,
    this.initialMonth,
    this.nowProvider,
  });

  final Future<List<Reminder>> Function()? reminderLoader;
  final DateTime? initialMonth;
  final DateTime Function()? nowProvider;

  @override
  State<AppointmentCalendarScreen> createState() =>
      _AppointmentCalendarScreenState();
}

class _AppointmentCalendarScreenState extends State<AppointmentCalendarScreen>
    with WidgetsBindingObserver {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _mutedText = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  List<Reminder> _appointments = const [];
  late DateTime _visibleMonth;
  DateTime? _selectedDate;
  bool _loading = true;
  String? _errorText;
  int _loadGeneration = 0;

  DateTime get _now => (widget.nowProvider?.call() ?? DateTime.now()).toLocal();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final initial = (widget.initialMonth ?? _now).toLocal();
    _visibleMonth = DateTime(initial.year, initial.month);
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _load(showLoading: false);
    }
  }

  Future<void> _load({bool showLoading = true}) async {
    final generation = ++_loadGeneration;
    if (showLoading && mounted) {
      setState(() {
        _loading = true;
        _errorText = null;
      });
    }

    try {
      final reminders =
          await (widget.reminderLoader?.call() ??
              _service.listAllRemindersOrThrow());
      final appointments =
          reminders
              .where(
                (reminder) =>
                    reminder.reminderType == ReminderType.appointment &&
                    reminder.status != ReminderStatus.cancelled,
              )
              .toList()
            ..sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _appointments = appointments;
        _loading = false;
        _errorText = null;
      });
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _loading = false;
        _errorText = 'Không thể tải lịch hẹn. Vui lòng thử lại.';
      });
    }
  }

  Map<DateTime, List<Reminder>> get _appointmentsByDate {
    final grouped = <DateTime, List<Reminder>>{};
    for (final appointment in _appointments) {
      final date = _dateOnly(appointment.scheduledAt.toLocal());
      grouped.putIfAbsent(date, () => <Reminder>[]).add(appointment);
    }
    for (final appointments in grouped.values) {
      appointments.sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
    }
    return grouped;
  }

  DateTime _dateOnly(DateTime value) =>
      DateTime(value.year, value.month, value.day);

  bool _isSameDate(DateTime? a, DateTime? b) {
    if (a == null || b == null) return false;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  bool _isBeforeToday(DateTime value) =>
      _dateOnly(value).isBefore(_dateOnly(_now));

  void _changeMonth(int offset) {
    setState(() {
      _visibleMonth = DateTime(
        _visibleMonth.year,
        _visibleMonth.month + offset,
      );
      _selectedDate = null;
    });
  }

  void _goToToday() {
    final today = _dateOnly(_now);
    setState(() {
      _visibleMonth = DateTime(today.year, today.month);
      _selectedDate = today;
    });
  }

  List<DateTime> _calendarDates() {
    final first = DateTime(_visibleMonth.year, _visibleMonth.month);
    final gridStart = first.subtract(Duration(days: first.weekday - 1));
    return List.generate(42, (index) => gridStart.add(Duration(days: index)));
  }

  Future<void> _selectDate(DateTime date) async {
    final selected = _dateOnly(date);
    setState(() {
      _selectedDate = selected;
      if (selected.month != _visibleMonth.month ||
          selected.year != _visibleMonth.year) {
        _visibleMonth = DateTime(selected.year, selected.month);
      }
    });

    final action = await showModalBottomSheet<_CalendarAction>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: _text.withAlpha(102),
      builder: (context) => _AppointmentDaySheet(
        date: selected,
        appointments: _appointmentsByDate[selected] ?? const [],
        canCreate: !_isBeforeToday(selected),
      ),
    );
    if (!mounted || action == null) return;

    Object? result;
    switch (action.type) {
      case _CalendarActionType.add:
        result = await context.push(
          '/reminders/add?date=${_routeDate(selected)}',
        );
      case _CalendarActionType.detail:
        result = await context.push('/reminders/detail/${action.reminderId}');
    }
    if (!mounted) return;
    if (result == true || result == 'deleted') {
      await _load(showLoading: false);
    }
  }

  String _routeDate(DateTime date) {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
  }

  String _monthLabel(DateTime month) {
    const labels = [
      'Tháng 1',
      'Tháng 2',
      'Tháng 3',
      'Tháng 4',
      'Tháng 5',
      'Tháng 6',
      'Tháng 7',
      'Tháng 8',
      'Tháng 9',
      'Tháng 10',
      'Tháng 11',
      'Tháng 12',
    ];
    return '${labels[month.month - 1]} ${month.year}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        elevation: 0,
        leading: IconButton(
          tooltip: 'Quay lại',
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.arrow_back_rounded, color: _text),
        ),
        title: const Text(
          'Lịch hẹn',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w800,
            color: _text,
          ),
        ),
      ),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(
                key: Key('appointment-calendar-loading'),
                color: _accent,
              ),
            )
          : _errorText != null
          ? _buildError()
          : RefreshIndicator(
              color: _accent,
              onRefresh: () => _load(showLoading: false),
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                children: [
                  _buildSummary(),
                  const SizedBox(height: 16),
                  _buildCalendarCard(),
                  const SizedBox(height: 16),
                  _buildMonthHint(),
                ],
              ),
            ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.event_busy_rounded, color: _error, size: 48),
            const SizedBox(height: 14),
            Text(
              _errorText!,
              key: const Key('appointment-calendar-error'),
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                height: 1.45,
                color: _text,
              ),
            ),
            const SizedBox(height: 18),
            FilledButton.icon(
              key: const Key('appointment-calendar-retry'),
              onPressed: _load,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Thử lại'),
              style: FilledButton.styleFrom(
                backgroundColor: _accent,
                foregroundColor: Colors.white,
                minimumSize: const Size(140, 48),
                shape: const StadiumBorder(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSummary() {
    final count = _appointments.where((appointment) {
      final local = appointment.scheduledAt.toLocal();
      return local.year == _visibleMonth.year &&
          local.month == _visibleMonth.month;
    }).length;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _nestedSurface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _border.withAlpha(160)),
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: Color(0xFFFFE2D9),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.calendar_month_rounded, color: _accentDark),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$count lịch hẹn trong tháng',
                  key: const Key('appointment-calendar-month-count'),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Chạm vào một ngày để xem hoặc thêm lịch hẹn.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    height: 1.4,
                    color: _mutedText,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCalendarCard() {
    final grouped = _appointmentsByDate;
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 18, 12, 16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: _border.withAlpha(150)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 32,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            children: [
              _MonthButton(
                key: const Key('appointment-calendar-previous-month'),
                tooltip: 'Tháng trước',
                icon: Icons.chevron_left_rounded,
                onPressed: () => _changeMonth(-1),
              ),
              Expanded(
                child: Text(
                  _monthLabel(_visibleMonth),
                  key: const Key('appointment-calendar-month-label'),
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 19,
                    fontWeight: FontWeight.w800,
                    color: _text,
                  ),
                ),
              ),
              _MonthButton(
                key: const Key('appointment-calendar-next-month'),
                tooltip: 'Tháng sau',
                icon: Icons.chevron_right_rounded,
                onPressed: () => _changeMonth(1),
              ),
            ],
          ),
          const SizedBox(height: 8),
          TextButton.icon(
            key: const Key('appointment-calendar-today'),
            onPressed: _goToToday,
            icon: const Icon(Icons.today_rounded, size: 18),
            label: const Text('Hôm nay'),
            style: TextButton.styleFrom(
              foregroundColor: _accentDark,
              shape: const StadiumBorder(),
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              for (final label in const [
                'T2',
                'T3',
                'T4',
                'T5',
                'T6',
                'T7',
                'CN',
              ])
                Expanded(
                  child: Center(
                    child: Text(
                      label,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: _mutedText,
                      ),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          GridView.count(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisCount: 7,
            childAspectRatio: 0.82,
            children: [
              for (final date in _calendarDates())
                _CalendarDay(
                  key: Key('appointment-calendar-day-${_routeDate(date)}'),
                  date: date,
                  inVisibleMonth:
                      date.month == _visibleMonth.month &&
                      date.year == _visibleMonth.year,
                  isToday: _isSameDate(date, _now),
                  isSelected: _isSameDate(date, _selectedDate),
                  appointmentCount: grouped[_dateOnly(date)]?.length ?? 0,
                  onTap: () => _selectDate(date),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMonthHint() {
    final hasAppointments = _appointments.any((appointment) {
      final local = appointment.scheduledAt.toLocal();
      return local.year == _visibleMonth.year &&
          local.month == _visibleMonth.month;
    });
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: hasAppointments ? _nestedSurface : _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            hasAppointments
                ? Icons.touch_app_rounded
                : Icons.add_circle_outline_rounded,
            color: _accentDark,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              hasAppointments
                  ? 'Các ngày có nền màu đất nung là ngày có lịch hẹn. Badge cho biết số lịch trong ngày.'
                  : 'Tháng này chưa có lịch hẹn. Chạm một ngày trong tương lai để tạo lịch mới.',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                height: 1.45,
                color: _text,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MonthButton extends StatelessWidget {
  const _MonthButton({
    super.key,
    required this.tooltip,
    required this.icon,
    required this.onPressed,
  });

  final String tooltip;
  final IconData icon;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: tooltip,
      onPressed: onPressed,
      style: IconButton.styleFrom(
        backgroundColor: const Color(0xFFF2EAE4),
        foregroundColor: const Color(0xFF845143),
        minimumSize: const Size(44, 44),
      ),
      icon: Icon(icon),
    );
  }
}

class _CalendarDay extends StatelessWidget {
  const _CalendarDay({
    super.key,
    required this.date,
    required this.inVisibleMonth,
    required this.isToday,
    required this.isSelected,
    required this.appointmentCount,
    required this.onTap,
  });

  final DateTime date;
  final bool inVisibleMonth;
  final bool isToday;
  final bool isSelected;
  final int appointmentCount;
  final VoidCallback onTap;

  String get _dateRoute {
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '${date.year}-$month-$day';
  }

  @override
  Widget build(BuildContext context) {
    final hasAppointments = appointmentCount > 0;
    final background = isSelected
        ? const Color(0xFF845143)
        : hasAppointments
        ? const Color(0xFFC98C7B)
        : Colors.transparent;
    final foreground = isSelected || hasAppointments
        ? Colors.white
        : inVisibleMonth
        ? const Color(0xFF5A463F)
        : const Color(0xFFB7A7A0);

    final semantics = StringBuffer(
      'Ngày ${date.day} tháng ${date.month} năm ${date.year}',
    );
    if (isToday) semantics.write(', hôm nay');
    if (appointmentCount > 0) {
      semantics.write(', có $appointmentCount lịch hẹn');
    } else {
      semantics.write(', chưa có lịch hẹn');
    }

    return Semantics(
      button: true,
      selected: isSelected,
      label: semantics.toString(),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          margin: const EdgeInsets.all(2),
          constraints: const BoxConstraints(minWidth: 44, minHeight: 48),
          decoration: BoxDecoration(
            color: background,
            borderRadius: BorderRadius.circular(16),
            border: isToday
                ? Border.all(
                    color: isSelected || hasAppointments
                        ? Colors.white
                        : const Color(0xFFC98C7B),
                    width: 2,
                  )
                : Border.all(color: Colors.transparent, width: 2),
          ),
          child: Stack(
            alignment: Alignment.center,
            children: [
              Text(
                '${date.day}',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: foreground,
                ),
              ),
              if (hasAppointments)
                Positioned(
                  right: 2,
                  top: 2,
                  child: Container(
                    key: Key('appointment-calendar-count-$_dateRoute'),
                    constraints: const BoxConstraints(
                      minWidth: 17,
                      minHeight: 17,
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: isSelected
                          ? const Color(0xFFC98C7B)
                          : const Color(0xFF5A463F),
                      borderRadius: BorderRadius.circular(99),
                      border: Border.all(color: Colors.white, width: 1.5),
                    ),
                    child: Text(
                      '$appointmentCount',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

enum _CalendarActionType { add, detail }

class _CalendarAction {
  const _CalendarAction.add()
    : type = _CalendarActionType.add,
      reminderId = null;

  const _CalendarAction.detail(this.reminderId)
    : type = _CalendarActionType.detail;

  final _CalendarActionType type;
  final String? reminderId;
}

class _AppointmentDaySheet extends StatelessWidget {
  const _AppointmentDaySheet({
    required this.date,
    required this.appointments,
    required this.canCreate,
  });

  final DateTime date;
  final List<Reminder> appointments;
  final bool canCreate;

  static const _surface = Colors.white;
  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _mutedText = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);

  String _dateLabel(DateTime value) {
    final day = value.day.toString().padLeft(2, '0');
    final month = value.month.toString().padLeft(2, '0');
    return '$day/$month/${value.year}';
  }

  String _timeLabel(DateTime value) {
    final local = value.toLocal();
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  @override
  Widget build(BuildContext context) {
    final maxHeight = MediaQuery.sizeOf(context).height * 0.86;
    return SafeArea(
      top: false,
      child: Container(
        key: const Key('appointment-day-sheet'),
        constraints: BoxConstraints(maxHeight: maxHeight),
        decoration: const BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
          boxShadow: [
            BoxShadow(
              color: Color(0x145A463F),
              blurRadius: 32,
              offset: Offset(0, -12),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 12),
            Container(
              width: 48,
              height: 5,
              decoration: BoxDecoration(
                color: _border,
                borderRadius: BorderRadius.circular(99),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 18, 16, 12),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: const BoxDecoration(
                      color: Color(0xFFFFE2D9),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.event_rounded, color: _accentDark),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _dateLabel(date),
                          key: const Key('appointment-day-sheet-date'),
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 20,
                            fontWeight: FontWeight.w800,
                            color: _text,
                          ),
                        ),
                        Text(
                          appointments.isEmpty
                              ? 'Chưa có lịch hẹn'
                              : '${appointments.length} lịch hẹn',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            color: _mutedText,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Đóng',
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close_rounded, color: _text),
                  ),
                ],
              ),
            ),
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 4, 24, 28),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (appointments.isEmpty)
                      _buildEmpty()
                    else
                      for (
                        var index = 0;
                        index < appointments.length;
                        index++
                      ) ...[
                        _AppointmentSheetCard(
                          appointment: appointments[index],
                          timeLabel: _timeLabel(
                            appointments[index].scheduledAt,
                          ),
                          onOpen: () => Navigator.pop(
                            context,
                            _CalendarAction.detail(appointments[index].id),
                          ),
                        ),
                        if (index != appointments.length - 1)
                          const SizedBox(height: 12),
                      ],
                    const SizedBox(height: 18),
                    if (canCreate)
                      FilledButton.icon(
                        key: const Key('appointment-day-add'),
                        onPressed: () =>
                            Navigator.pop(context, const _CalendarAction.add()),
                        icon: const Icon(Icons.add_rounded),
                        label: Text(
                          appointments.isEmpty
                              ? 'Thêm lịch hẹn'
                              : 'Thêm lịch hẹn khác',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        style: FilledButton.styleFrom(
                          backgroundColor: _accent,
                          foregroundColor: Colors.white,
                          minimumSize: const Size.fromHeight(52),
                          shape: const StadiumBorder(),
                        ),
                      )
                    else
                      Container(
                        key: const Key('appointment-day-past-notice'),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: _nestedSurface,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: _border),
                        ),
                        child: const Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(Icons.history_rounded, color: _accentDark),
                            SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                'Không thể thêm lịch hẹn mới vào ngày đã qua.',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 14,
                                  height: 1.4,
                                  color: _text,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _nestedSurface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _border),
      ),
      child: const Column(
        children: [
          Icon(Icons.event_available_rounded, size: 42, color: _accentDark),
          SizedBox(height: 12),
          Text(
            'Ngày này chưa có lịch hẹn.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: _text,
            ),
          ),
          SizedBox(height: 6),
          Text(
            'Mày có thể tạo lịch khám, tái khám hoặc cuộc hẹn mới.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              height: 1.4,
              color: _mutedText,
            ),
          ),
        ],
      ),
    );
  }
}

class _AppointmentSheetCard extends StatelessWidget {
  const _AppointmentSheetCard({
    required this.appointment,
    required this.timeLabel,
    required this.onOpen,
  });

  final Reminder appointment;
  final String timeLabel;
  final VoidCallback onOpen;

  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return Container(
      key: Key('appointment-day-item-${appointment.id}'),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _nestedSurface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 7,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Text(
                  timeLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w800,
                    color: _accentDark,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  appointment.title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    height: 1.35,
                    color: _text,
                  ),
                ),
              ),
            ],
          ),
          if (appointment.location != null &&
              appointment.location!.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            _SheetInfoLine(
              icon: Icons.place_rounded,
              text: appointment.location!,
            ),
          ],
          if (appointment.note != null &&
              appointment.note!.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            _SheetInfoLine(icon: Icons.notes_rounded, text: appointment.note!),
          ],
          const SizedBox(height: 8),
          _SheetInfoLine(
            icon: Icons.flag_rounded,
            text: appointment.status.displayLabel,
          ),
          const SizedBox(height: 8),
          _SheetInfoLine(
            icon: Icons.repeat_rounded,
            text: appointment.recurrenceType.displayLabel,
          ),
          const SizedBox(height: 14),
          OutlinedButton.icon(
            key: Key('appointment-day-open-${appointment.id}'),
            onPressed: onOpen,
            icon: const Icon(Icons.open_in_new_rounded, size: 18),
            label: const Text(
              'Xem / chỉnh sửa',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w800,
              ),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: _accentDark,
              side: const BorderSide(color: _accentDark),
              minimumSize: const Size.fromHeight(46),
              shape: const StadiumBorder(),
            ),
          ),
        ],
      ),
    );
  }
}

class _SheetInfoLine extends StatelessWidget {
  const _SheetInfoLine({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: const Color(0xFF845143)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              height: 1.4,
              color: Color(0xFF5A463F),
            ),
          ),
        ),
      ],
    );
  }
}
