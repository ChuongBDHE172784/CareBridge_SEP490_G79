import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:universal_io/io.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/api_client.dart';
import '../../emergency/services/emergency_service.dart';
import '../../safety/services/safety_permission_service.dart';
import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';
import '../services/watch_metric_import_service.dart';
import 'epds_screen.dart';

class HealthMetricTrendScreen extends StatefulWidget {
  final String journeyId;
  final String? initialMetricType;

  const HealthMetricTrendScreen({
    super.key,
    required this.journeyId,
    this.initialMetricType,
  });

  @override
  State<HealthMetricTrendScreen> createState() =>
      _HealthMetricTrendScreenState();
}

class _HealthMetricTrendScreenState extends State<HealthMetricTrendScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _surfaceAccent = Color(0xFFF6F1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  static const _overviewOption = _MetricOption(
    apiValue: 'TOTAL_OVERVIEW',
    label: 'Đánh giá Sức khỏe Toàn diện AI',
    unit: '',
    icon: Icons.auto_awesome,
  );

  static const _fallbackMetricOptions = [
    _overviewOption,
    _MetricOption(
      apiValue: 'BMI',
      label: 'Chỉ số khối cơ thể (BMI)',
      unit: 'kg/m²',
      icon: Icons.monitor_weight_outlined,
    ),
    _MetricOption(
      apiValue: 'FETAL_MOVEMENT_SESSION',
      label: 'Cử động thai',
      unit: 'count',
      icon: Icons.child_friendly_rounded,
    ),
    _MetricOption(
      apiValue: 'BLOOD_PRESSURE',
      label: 'Huyết áp',
      unit: 'mmHg',
      icon: Icons.favorite_border_rounded,
    ),
    _MetricOption(
      apiValue: 'HYDRATION',
      label: 'Lượng nước uống',
      unit: 'ml',
      icon: Icons.local_drink_outlined,
    ),
    _MetricOption(
      apiValue: 'MATERNAL_HEART_RATE',
      label: 'Nhịp tim mẹ',
      unit: 'bpm',
      icon: Icons.monitor_heart_outlined,
    ),
    _MetricOption(
      apiValue: 'EPDS_SCORE',
      label: 'Điểm sàng lọc trầm cảm EPDS',
      unit: 'điểm',
      icon: Icons.psychology_outlined,
    ),
    _MetricOption(
      apiValue: 'BLOOD_GLUCOSE',
      label: 'Đường huyết',
      unit: 'mg/dL',
      icon: Icons.water_drop_outlined,
    ),
    _MetricOption(
      apiValue: 'TEMPERATURE',
      label: 'Nhiệt độ cơ thể',
      unit: '°C',
      icon: Icons.thermostat_outlined,
    ),
  ];

  final _service = HealthMetricService();
  final _watchImportService = WatchMetricImportService();
  final _historyScrollController = ScrollController();
  final _symptomNoteCtrl = TextEditingController();

  late _MetricOption _selectedMetric;
  List<_MetricOption> _metricOptions = _fallbackMetricOptions;
  bool _isLoading = false;
  bool _isLoadingCapabilities = true;
  bool _isEvaluatingAi = false;
  String? _errorMsg;
  MetricTrend? _trend;

  // Snapshot các chỉ số sức khỏe tổng quan
  MetricDataPoint? _latestBp;
  MetricDataPoint? _latestBmi;
  MetricDataPoint? _latestGlucose;
  MetricDataPoint? _latestKicks;
  MetricDataPoint? _latestHydration;
  MetricDataPoint? _latestEpds;
  MetricDataPoint? _latestHeartRate;
  MetricDataPoint? _latestTemp;
  int? _journeyGestationalWeeks;
  List<String> _surveyRiskConditions = [];

  List<String> get _pythonCandidates {
    final list = <String>[];
    try {
      final uri = Uri.parse(apiBaseUrl);
      if (uri.host.isNotEmpty) {
        list.add('${uri.scheme}://${uri.host}:8001');
      }
    } catch (_) {}
    if (kIsWeb) {
      list.add('http://127.0.0.1:8001');
    } else if (Platform.isAndroid) {
      list.addAll(['http://10.0.2.2:8001', 'http://127.0.0.1:8001']);
    } else {
      list.addAll(['http://127.0.0.1:8001', 'http://localhost:8001']);
    }
    return list;
  }

  List<MetricDataPoint> get _historyPoints {
    final points = [...?_trend?.dataPoints];
    points.sort((a, b) => b.measuredAt.compareTo(a.measuredAt));
    return points;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _selectedMetric = _fallbackMetricOptions.first;
    _watchImportService.start(
      journeyId: widget.journeyId,
      onImported: _handleWatchMetricImported,
      onError: _showWatchMetricError,
    );
    unawaited(_watchImportService.drainQueuedEvents());
    _loadCapabilities();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      SystemChrome.setSystemUIOverlayStyle(
        const SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: Brightness.dark,
        ),
      );
    });
  }

  Future<void> _loadCapabilities() async {
    try {
      final capabilities = await _service.getCapabilities(widget.journeyId);
      if (!mounted) return;
      final options = capabilities
          .where(
            (capability) =>
                capability.manualEntrySupported &&
                capability.metricCode != 'STRESS',
          )
          .map(_optionFromCapability)
          .toList();
      final resolvedOptions = [_overviewOption, ...options];
      final requested = _canonicalMetricType(widget.initialMetricType);
      setState(() {
        _metricOptions = resolvedOptions;
        _selectedMetric = resolvedOptions.firstWhere(
          (option) => option.apiValue == requested,
          orElse: () => resolvedOptions.first,
        );
        _isLoadingCapabilities = false;
      });
      await _loadTrend();
    } catch (_) {
      if (!mounted) return;
      final requested = _canonicalMetricType(widget.initialMetricType);
      setState(() {
        _metricOptions = _fallbackMetricOptions;
        _selectedMetric = _fallbackMetricOptions.firstWhere(
          (option) => option.apiValue == requested,
          orElse: () => _fallbackMetricOptions.first,
        );
        _isLoadingCapabilities = false;
      });
      await _loadTrend();
    }
  }

  _MetricOption _optionFromCapability(MetricCapability capability) {
    final code = _canonicalMetricType(capability.metricCode);
    final fallback = _fallbackMetricOptions.firstWhere(
      (option) => option.apiValue == code,
      orElse: () => const _MetricOption(
        apiValue: 'BMI',
        label: 'Chỉ số khối cơ thể (BMI)',
        unit: 'kg/m²',
        icon: Icons.monitor_weight_outlined,
      ),
    );
    return _MetricOption(
      apiValue: code,
      label: capability.displayName,
      unit: capability.canonicalUnit.isEmpty
          ? fallback.unit
          : capability.canonicalUnit,
      icon: fallback.icon,
    );
  }

  String _canonicalMetricType(String? value) {
    switch (value) {
      case 'TOTAL_OVERVIEW':
      case 'OVERVIEW':
        return 'TOTAL_OVERVIEW';
      case 'BLOOD_PRESSURE_SYSTOLIC':
      case 'BLOOD_PRESSURE_DIASTOLIC':
      case 'BLOOD_PRESSURE':
        return 'BLOOD_PRESSURE';
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_COUNT':
      case 'FETAL_MOVEMENT_SESSION':
        return 'FETAL_MOVEMENT_SESSION';
      case 'HEART_RATE':
      case 'MATERNAL_HEART_RATE':
        return 'MATERNAL_HEART_RATE';
      case 'HYDRATION':
        return 'HYDRATION';
      case 'EPDS':
      case 'EPDS_SCORE':
        return 'EPDS_SCORE';
      case 'BLOOD_SUGAR':
      case 'BLOOD_GLUCOSE':
        return 'BLOOD_GLUCOSE';
      case 'TEMPERATURE':
      case 'BODY_TEMPERATURE':
        return 'TEMPERATURE';
      case 'BMI':
      default:
        return value ?? 'TOTAL_OVERVIEW';
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _watchImportService.dispose();
    _historyScrollController.dispose();
    _symptomNoteCtrl.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_watchImportService.drainQueuedEvents());
    }
  }

  Future<void> _loadTrend() async {
    if (!_isSupportedMetric) return;
    setState(() {
      _isLoading = true;
      _errorMsg = null;
    });

    if (_selectedMetric.apiValue == 'TOTAL_OVERVIEW') {
      try {
        try {
          final journeyRes = await apiGet('/api/v1/journeys/me/dashboard');
          if (journeyRes is Map && mounted) {
            final data =
                (journeyRes['data'] is Map) ? journeyRes['data'] : journeyRes;
            final week =
                data['pregnancyWeek'] ??
                data['completedGestationalWeek'] ??
                data['effectivePregnancyWeek'] ??
                data['weekNumber'] ??
                data['currentWeek'];
            if (week != null) {
              final parsed = (week is int)
                  ? week
                  : int.tryParse(week.toString());
              if (parsed != null) {
                _journeyGestationalWeeks = parsed;
              }
            }
          }
        } catch (_) {}

        try {
          final profileRes = await apiGet('/api/v1/recommendations/profile');
          if (profileRes is Map && mounted) {
            final data = (profileRes['data'] is Map)
                ? profileRes['data']
                : profileRes;
            final profile = data['profile'] ?? data;
            final conditions = <String>[];
            if (profile is Map) {
              if (profile['underlyingConditions'] is Map &&
                  profile['underlyingConditions']['conditionCodes'] is List) {
                conditions.addAll(
                  (profile['underlyingConditions']['conditionCodes'] as List)
                      .map((e) => e.toString()),
                );
              }
              if (profile['reproductiveHistory'] is Map &&
                  profile['reproductiveHistory']['conditionCodes'] is List) {
                conditions.addAll(
                  (profile['reproductiveHistory']['conditionCodes'] as List)
                      .map((e) => e.toString()),
                );
              }
            }
            _surveyRiskConditions = conditions;
          }
        } catch (_) {}

        final results = await Future.wait([
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'BLOOD_PRESSURE',
              )
              .catchError(
                (_) => const MetricTrend(
                  metricType: 'BLOOD_PRESSURE',
                  dataPoints: [],
                ),
              ),
          _service
              .getMetricTrend(journeyId: widget.journeyId, metricType: 'BMI')
              .catchError(
                (_) => const MetricTrend(metricType: 'BMI', dataPoints: []),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'BLOOD_GLUCOSE',
              )
              .catchError(
                (_) => const MetricTrend(
                  metricType: 'BLOOD_GLUCOSE',
                  dataPoints: [],
                ),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'FETAL_MOVEMENT_SESSION',
              )
              .catchError(
                (_) => const MetricTrend(
                  metricType: 'FETAL_MOVEMENT_SESSION',
                  dataPoints: [],
                ),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'HYDRATION',
              )
              .catchError(
                (_) =>
                    const MetricTrend(metricType: 'HYDRATION', dataPoints: []),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'EPDS_SCORE',
              )
              .catchError(
                (_) =>
                    const MetricTrend(metricType: 'EPDS_SCORE', dataPoints: []),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'MATERNAL_HEART_RATE',
              )
              .catchError(
                (_) => const MetricTrend(
                  metricType: 'MATERNAL_HEART_RATE',
                  dataPoints: [],
                ),
              ),
          _service
              .getMetricTrend(
                journeyId: widget.journeyId,
                metricType: 'TEMPERATURE',
              )
              .catchError(
                (_) => const MetricTrend(
                  metricType: 'TEMPERATURE',
                  dataPoints: [],
                ),
              ),
        ]);

        if (mounted) {
          setState(() {
            _latestBp = results[0].dataPoints.isNotEmpty
                ? results[0].dataPoints.last
                : null;
            _latestBmi = results[1].dataPoints.isNotEmpty
                ? results[1].dataPoints.last
                : null;
            _latestGlucose = results[2].dataPoints.isNotEmpty
                ? results[2].dataPoints.last
                : null;
            _latestKicks = results[3].dataPoints.isNotEmpty
                ? results[3].dataPoints.last
                : null;
            _latestHydration = results[4].dataPoints.isNotEmpty
                ? results[4].dataPoints.last
                : null;
            _latestEpds = results[5].dataPoints.isNotEmpty
                ? results[5].dataPoints.last
                : null;
            _latestHeartRate = results[6].dataPoints.isNotEmpty
                ? results[6].dataPoints.last
                : null;
            _latestTemp = results[7].dataPoints.isNotEmpty
                ? results[7].dataPoints.last
                : null;
            _isLoading = false;
          });
        }
      } catch (_) {
        if (mounted) {
          setState(() {
            _isLoading = false;
            _errorMsg = 'Không thể tải bức tranh sức khỏe tổng quan.';
          });
        }
      }
      return;
    }

    try {
      final result = await _service.getMetricTrend(
        journeyId: widget.journeyId,
        metricType: _selectedMetric.apiValue,
        from: DateTime.now().subtract(const Duration(days: 7)),
      );
      if (mounted) {
        setState(() => _trend = result);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _trend = null;
          _errorMsg = 'Không thể tải dữ liệu. Vui lòng thử lại.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  bool get _isSupportedMetric => _metricOptions.any(
    (option) => option.apiValue == _selectedMetric.apiValue,
  );

  bool get _isBloodPressure => _selectedMetric.apiValue == 'BLOOD_PRESSURE';
  bool get _isGlucose => _selectedMetric.apiValue == 'BLOOD_GLUCOSE';
  bool get _isFetalMovement =>
      _selectedMetric.apiValue == 'FETAL_MOVEMENT_SESSION';
  bool get _isWatchMetric =>
      _selectedMetric.apiValue == 'MATERNAL_HEART_RATE' ||
      _selectedMetric.apiValue == 'STRESS';

  void _onMetricChanged(_MetricOption? opt) {
    if (opt == null || opt == _selectedMetric) return;
    setState(() => _selectedMetric = opt);
    _loadTrend();
  }

  Future<void> _openAddMetric() async {
    if (_selectedMetric.apiValue == 'TOTAL_OVERVIEW') {
      await _showMetricPickerModal();
      return;
    }

    if (_selectedMetric.apiValue == 'EPDS_SCORE') {
      await Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => EpdsScreen(journeyId: widget.journeyId),
        ),
      );
      if (mounted) {
        await _loadTrend();
      }
      return;
    }

    final changed = await context.push<bool>(
      '/journeys/${Uri.encodeComponent(widget.journeyId)}/metrics/add'
      '?metricType=${Uri.encodeQueryComponent(_selectedMetric.apiValue)}',
    );
    if (changed == true && mounted) {
      await _loadTrend();
    }
  }

  Future<void> _showMetricPickerModal() async {
    final selected = await showModalBottomSheet<_MetricOption>(
      context: context,
      backgroundColor: _canvas,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        final entryOptions = _fallbackMetricOptions
            .where(
              (o) => o.apiValue != 'TOTAL_OVERVIEW' && o.apiValue != 'STRESS',
            )
            .toList();
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: _onSurfaceVariant.withAlpha(50),
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                const Text(
                  'Chọn chỉ số muốn ghi nhận',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _primary,
                  ),
                ),
                const SizedBox(height: 12),
                Flexible(
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: entryOptions.length,
                    separatorBuilder: (_, __) =>
                        const Divider(height: 1, color: _surfaceContainer),
                    itemBuilder: (ctx, i) {
                      final opt = entryOptions[i];
                      return ListTile(
                        leading: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: _surfaceContainer,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Icon(opt.icon, color: _primary, size: 20),
                        ),
                        title: Text(
                          opt.label,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                        trailing: const Icon(
                          Icons.chevron_right,
                          color: _onSurfaceVariant,
                          size: 20,
                        ),
                        onTap: () => Navigator.of(ctx).pop(opt),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );

    if (selected != null && mounted) {
      if (selected.apiValue == 'EPDS_SCORE') {
        await Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => EpdsScreen(journeyId: widget.journeyId),
          ),
        );
      } else {
        await context.push<bool>(
          '/journeys/${Uri.encodeComponent(widget.journeyId)}/metrics/add'
          '?metricType=${Uri.encodeQueryComponent(selected.apiValue)}',
        );
      }
      if (mounted) {
        await _loadTrend();
      }
    }
  }

  Future<void> _openWatchMeasurement() async {
    final opened = await _watchImportService.openGadgetbridge();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          opened
              ? 'Đã mở Gadgetbridge. Hãy đồng bộ đồng hồ để gửi dữ liệu về CareBridge.'
              : 'Không tìm thấy Gadgetbridge trên thiết bị.',
        ),
      ),
    );
  }

  Future<void> _handleWatchMetricImported(
    WatchMetricImportResult result,
  ) async {
    if (!mounted) return;
    if (result.metricType == _selectedMetric.apiValue) {
      await _loadTrend();
    }
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Đã lưu ${result.metricType == 'STRESS' ? 'Stress' : 'Nhịp tim'} từ đồng hồ.',
        ),
      ),
    );
  }

  void _showWatchMetricError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _openMetricDetail(MetricDataPoint point) async {
    if (_selectedMetric.apiValue == 'EPDS_SCORE') {
      final answers = parseEpdsAnswers(point.note);
      if (answers != null) {
        await Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => EpdsHistoryDetailScreen(
              completedAt: point.measuredAt,
              totalScore: point.valueNumeric.round(),
              question10Score: point.valueSecondary?.round() ?? answers[9],
              answers: answers,
            ),
          ),
        );
        return;
      }
    }

    final metricId = point.metricId;
    if (metricId != null && metricId.isNotEmpty) {
      final changed = await context.push<bool>(
        '/health-metrics/${Uri.encodeComponent(metricId)}',
      );
      if (changed == true && mounted) {
        await _loadTrend();
      }
      return;
    }

    await context.push(
      '/health-metrics/${Uri.encodeComponent(_syntheticMetricId(point))}',
      extra: {'metric': _fallbackMetricDetail(point)},
    );
  }

  HealthMetricDetail _fallbackMetricDetail(MetricDataPoint point) {
    return HealthMetricDetail(
      id: _syntheticMetricId(point),
      journeyId: widget.journeyId,
      metricType: _metricTypeFor(_selectedMetric.apiValue),
      metricCode: _selectedMetric.apiValue,
      valueNumeric: point.valueNumeric,
      valueSecondary: point.valueSecondary,
      unit: _trend?.unit ?? _selectedMetric.unit,
      measuredAt: point.measuredAt,
      sourceType: point.sourceType,
      note: point.note,
      createdAt: point.measuredAt,
      context: point.context,
      periodStart: point.periodStart,
      periodEnd: point.periodEnd,
      qualityLabel: point.qualityLabel,
      disclaimer: _trend?.disclaimer,
    );
  }

  String _syntheticMetricId(MetricDataPoint point) {
    return '${_selectedMetric.apiValue}-${point.measuredAt.microsecondsSinceEpoch}';
  }

  MetricType _metricTypeFor(String apiValue) {
    return MetricTypeExtension.fromApi(apiValue);
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        statusBarBrightness: Brightness.light,
      ),
      child: Scaffold(
        backgroundColor: _canvas,
        appBar: AppBar(
          backgroundColor: _canvas,
          elevation: 0,
          systemOverlayStyle: const SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness: Brightness.dark,
            statusBarBrightness: Brightness.light,
          ),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_rounded, color: _primary),
            onPressed: () => Navigator.of(context).pop(),
          ),
          title: const Text(
            'Chỉ số sức khỏe',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _primary,
              letterSpacing: -0.3,
            ),
          ),
          actions: [
            IconButton(
              tooltip: 'Nhập chỉ số',
              icon: const Icon(
                Icons.add_circle_outline,
                color: _primary,
                size: 26,
              ),
              onPressed: _openAddMetric,
            ),
          ],
        ),
        body: RefreshIndicator(
          color: _primary,
          onRefresh: _loadTrend,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildMetricSelector(),
                const SizedBox(height: 16),
                if (_isLoadingCapabilities || _isLoading)
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.all(48),
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    ),
                  )
                else if (!_isSupportedMetric)
                  _buildUnsupportedMetricCard()
                else if (_errorMsg != null)
                  _buildErrorCard()
                else if (_selectedMetric.apiValue == 'TOTAL_OVERVIEW')
                  _buildTotalOverviewSection()
                else ...[
                  _buildChartCard(),
                  const SizedBox(height: 16),
                  _buildHistoryCard(),
                  const SizedBox(height: 20),
                  _buildActionButtons(),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMetricSelector() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _surfaceContainer, width: 1.5),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<_MetricOption>(
          value: _selectedMetric,
          isExpanded: true,
          icon: const Icon(Icons.expand_more_rounded, color: _onSurfaceVariant),
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurface,
          ),
          items: _metricOptions
              .map(
                (o) => DropdownMenuItem(
                  value: o,
                  child: Row(
                    children: [
                      Icon(o.icon, size: 18, color: _primary),
                      const SizedBox(width: 8),
                      Text(o.label),
                    ],
                  ),
                ),
              )
              .toList(),
          onChanged: _onMetricChanged,
          dropdownColor: _canvas,
          borderRadius: BorderRadius.circular(20),
        ),
      ),
    );
  }

  Widget _buildUnsupportedMetricCard() {
    return _Card(
      child: Column(
        children: [
          const Icon(Icons.info_outline_rounded, color: _error, size: 44),
          const SizedBox(height: 12),
          const Text(
            'Chỉ số này chưa được hỗ trợ trong hành trình hiện tại.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard() {
    return _Card(
      child: Column(
        children: [
          const Icon(Icons.error_outline_rounded, color: _error, size: 44),
          const SizedBox(height: 12),
          Text(
            _errorMsg!,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: _loadTrend,
            child: const Text(
              'Thử lại',
              style: TextStyle(
                fontFamily: 'Lexend',
                color: _primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChartCard() {
    final points = _historyPoints;
    final avg = _trend?.average;
    final unit = _trend?.unit ?? _selectedMetric.unit;
    final trendPct = _trend?.trend;
    final isDown = (trendPct ?? 0) < 0;
    final hasScalarSummary =
        avg != null && !_isBloodPressure && !_isFetalMovement;

    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      points.isEmpty
                          ? '-- $unit'
                          : hasScalarSummary
                          ? '${_formatNumber(avg)} $unit'
                          : _summaryLabel(unit),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _summaryCaption,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (trendPct != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: _primary.withAlpha(25),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        isDown
                            ? Icons.trending_down_rounded
                            : Icons.trending_up_rounded,
                        color: _primary,
                        size: 16,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${trendPct.abs().toStringAsFixed(1)}%',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _primary,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          const SizedBox(height: 20),
          if (points.isEmpty)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Text(
                  'Chưa có dữ liệu trong 7 ngày gần đây',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else
            _buildBarChart(points),
        ],
      ),
    );
  }

  Widget _buildBarChart(List<MetricDataPoint> points) {
    final bars = _buildSevenDayBars(points);
    final maxValue = bars.fold<double>(
      0,
      (max, bar) => math.max(max, bar.value ?? 0),
    );
    final effectiveMax = maxValue <= 0 ? 1.0 : maxValue;

    return SizedBox(
      height: 210,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: bars.map((bar) {
          final ratio = ((bar.value ?? 0) / effectiveMax)
              .clamp(0.0, 1.0)
              .toDouble();
          final barHeight = 120.0 * ratio;
          return Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  SizedBox(
                    height: 22,
                    child: Text(
                      bar.value == null ? '' : _formatNumber(bar.value!),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Container(
                    height: math.max(4.0, barHeight),
                    decoration: BoxDecoration(
                      color: bar.value == null
                          ? _surfaceContainer
                          : _primaryContainer,
                      borderRadius: const BorderRadius.vertical(
                        top: Radius.circular(8),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    height: 32,
                    child: Text(
                      _formatShortDate(bar.date),
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  String _summaryLabel(String unit) {
    if (_isBloodPressure) return 'Xem theo cặp tâm thu/tâm trương';
    if (_isFetalMovement) return 'Phiên đo trong 7 ngày';
    if (_isGlucose) return 'Chọn cùng bối cảnh để so sánh';
    return '-- $unit';
  }

  String get _summaryCaption {
    if (_isBloodPressure) return 'Không tính trung bình vô hướng';
    if (_isFetalMovement) return 'Lịch sử phiên cử động thai';
    if (_isGlucose) return 'Đường huyết được phân nhóm theo bối cảnh đo';
    return 'Trung bình 7 ngày gần đây';
  }

  List<_ChartBar> _buildSevenDayBars(List<MetricDataPoint> points) {
    final now = DateTime.now();
    final days = List.generate(7, (i) {
      final date = DateTime(
        now.year,
        now.month,
        now.day,
      ).subtract(Duration(days: 6 - i));
      return date;
    });
    return days.map((date) {
      final sameDay = points.where((point) {
        final measuredAt = point.measuredAt;
        return measuredAt.year == date.year &&
            measuredAt.month == date.month &&
            measuredAt.day == date.day;
      }).toList();
      if (sameDay.isEmpty) {
        return _ChartBar(date: date, value: null);
      }
      if (_isBloodPressure) {
        return _ChartBar(date: date, value: null);
      }
      if (_isGlucose) {
        final contexts = sameDay
            .map((point) => point.context['measurementContext'])
            .whereType<String>()
            .toSet();
        if (contexts.length > 1) return _ChartBar(date: date, value: null);
      }
      final sum = sameDay.fold<double>(
        0,
        (total, point) => total + point.valueNumeric,
      );
      return _ChartBar(date: date, value: sum / sameDay.length);
    }).toList();
  }

  Widget _buildHistoryCard() {
    final points = _historyPoints;
    final rowHeight = _selectedMetric.apiValue == 'BMI' ? 94.0 : 74.0;
    final height = math.min(470.0, math.max(96.0, points.length * rowHeight));

    return _Card(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Lịch sử đo',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          if (points.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text(
                  'Chưa có bản ghi chỉ số.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else
            SizedBox(
              height: height,
              child: ListView.separated(
                controller: _historyScrollController,
                padding: EdgeInsets.zero,
                itemCount: points.length,
                itemBuilder: (context, index) {
                  return _HistoryTile(
                    point: points[index],
                    metric: _selectedMetric,
                    unit: _trend?.unit ?? _selectedMetric.unit,
                    onTap: () => _openMetricDetail(points[index]),
                  );
                },
                separatorBuilder: (_, _) =>
                    const Divider(height: 1, color: _surfaceContainer),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    if (!_isWatchMetric) return _buildAddButton();
    return Row(
      children: [
        Expanded(child: _buildAddButton()),
        const SizedBox(width: 12),
        Expanded(child: _buildWatchButton()),
      ],
    );
  }

  Widget _buildAddButton() {
    return ElevatedButton.icon(
      onPressed: _openAddMetric,
      icon: const Icon(Icons.add_circle_outline_rounded, size: 18),
      label: const Text(
        'Thêm chỉ số',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
      style: ElevatedButton.styleFrom(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
    );
  }

  Widget _buildWatchButton() {
    return OutlinedButton.icon(
      onPressed: _openWatchMeasurement,
      icon: const Icon(Icons.watch_outlined, size: 18),
      label: const Text(
        'Đo từ đồng hồ',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
      style: OutlinedButton.styleFrom(
        foregroundColor: _primary,
        side: const BorderSide(color: _primary, width: 1.4),
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: const StadiumBorder(),
      ),
    );
  }

  String _formatNumber(double value) {
    return value % 1 == 0 ? value.toStringAsFixed(0) : value.toStringAsFixed(1);
  }

  String _formatShortDate(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  }

  Widget _buildTotalOverviewSection() {
    final gestationalAge = _journeyGestationalWeeks ?? 20;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 1. Header Profile Card (Gestational week + Survey Risks)
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFFF9F3EF), Color(0xFFF2EAE4)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: const Color(0xFFE2D4CC), width: 1.2),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _primary.withAlpha(25),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(
                      Icons.favorite_rounded,
                      color: _primary,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Hồ sơ Thai kỳ & Sàng lọc Y tế',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Tuần thai hiện tại: Tuần $gestationalAge',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: _primary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              if (_surveyRiskConditions.isNotEmpty) ...[
                const SizedBox(height: 12),
                const Divider(height: 1, color: Color(0xFFE2D4CC)),
                const SizedBox(height: 10),
                const Text(
                  'Yếu tố tiền sử / Bệnh nền (từ Survey):',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: _surveyRiskConditions.map((c) {
                    var label = c;
                    if (c == 'PRIOR_PREECLAMPSIA')
                      label = 'Tiền sử Tiền sản giật';
                    if (c == 'CHRONIC_HYPERTENSION')
                      label = 'Tăng huyết áp mạn';
                    if (c == 'PREGESTATIONAL_DIABETES' || c == 'PRIOR_GDM')
                      label = 'Tiền sử ĐTĐ thai kỳ';
                    return Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFAF1ED),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: const Color(0xFFD6C2BD)),
                      ),
                      child: Text(
                        label,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: _primary,
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 2. Multi-Metric Snapshot Cards
        _Card(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Chỉ số sinh hiệu mới nhất',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _primary,
                    ),
                  ),
                  TextButton.icon(
                    onPressed: _showMetricPickerModal,
                    icon: const Icon(
                      Icons.add_rounded,
                      size: 18,
                      color: _primary,
                    ),
                    label: const Text(
                      'Ghi nhận',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                        color: _primary,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),

              // Huyết áp
              _buildOverviewMetricTile(
                icon: Icons.favorite_border_rounded,
                title: 'Huyết áp',
                value: _latestBp != null
                    ? '${_latestBp!.valueNumeric.round()}/${_latestBp!.valueSecondary?.round() ?? 0} mmHg'
                    : null,
                measuredAt: _latestBp?.measuredAt,
                metricCode: 'BLOOD_PRESSURE',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // BMI & Thể trạng
              _buildOverviewMetricTile(
                icon: Icons.monitor_weight_outlined,
                title: 'BMI & Thể trạng',
                value: _latestBmi != null
                    ? '${_latestBmi!.valueNumeric.toStringAsFixed(1)} kg/m²'
                    : null,
                measuredAt: _latestBmi?.measuredAt,
                metricCode: 'BMI',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Đường huyết
              _buildOverviewMetricTile(
                icon: Icons.water_drop_outlined,
                title: 'Đường huyết',
                value: _latestGlucose != null
                    ? '${_latestGlucose!.valueNumeric} mg/dL'
                    : null,
                measuredAt: _latestGlucose?.measuredAt,
                metricCode: 'BLOOD_GLUCOSE',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Cử động thai
              _buildOverviewMetricTile(
                icon: Icons.child_friendly_rounded,
                title: 'Cử động thai (2h)',
                value: _latestKicks != null
                    ? '${_latestKicks!.valueNumeric.round()} cử động'
                    : null,
                measuredAt: _latestKicks?.measuredAt,
                metricCode: 'FETAL_MOVEMENT_SESSION',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Lượng nước uống
              _buildOverviewMetricTile(
                icon: Icons.local_drink_outlined,
                title: 'Lượng nước uống',
                value: _latestHydration != null
                    ? '${_latestHydration!.valueNumeric.round()} ml'
                    : null,
                measuredAt: _latestHydration?.measuredAt,
                metricCode: 'HYDRATION',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Nhịp tim mẹ
              _buildOverviewMetricTile(
                icon: Icons.monitor_heart_outlined,
                title: 'Nhịp tim mẹ',
                value: _latestHeartRate != null
                    ? '${_latestHeartRate!.valueNumeric.round()} bpm'
                    : null,
                measuredAt: _latestHeartRate?.measuredAt,
                metricCode: 'MATERNAL_HEART_RATE',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Điểm EPDS
              _buildOverviewMetricTile(
                icon: Icons.psychology_outlined,
                title: 'Sàng lọc tâm trạng EPDS',
                value: _latestEpds != null
                    ? '${_latestEpds!.valueNumeric.round()}/30 điểm'
                    : null,
                measuredAt: _latestEpds?.measuredAt,
                metricCode: 'EPDS_SCORE',
              ),
              const Divider(height: 1, color: _surfaceContainer),

              // Nhiệt độ cơ thể
              _buildOverviewMetricTile(
                icon: Icons.thermostat_outlined,
                title: 'Nhiệt độ cơ thể',
                value: _latestTemp != null
                    ? '${_latestTemp!.valueNumeric.toStringAsFixed(1)} °C'
                    : null,
                measuredAt: _latestTemp?.measuredAt,
                metricCode: 'TEMPERATURE',
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // 3. Ghi chú & Triệu chứng bổ sung
        _Card(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Triệu chứng hoặc cảm giác cơ thể bổ sung',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Ghi nhận các biểu hiện như: đau đầu, hoa mắt, nhìn mờ, phù nề, mệt mỏi, đau bụng...',
                style: TextStyle(fontSize: 12, color: _onSurfaceVariant),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _symptomNoteCtrl,
                maxLines: 3,
                decoration: InputDecoration(
                  hintText: 'Ví dụ: Em hơi đau đầu và thấy người mệt...',
                  hintStyle: const TextStyle(
                    fontSize: 13,
                    color: Color(0xFFA0938E),
                  ),
                  filled: true,
                  fillColor: _canvas,
                  contentPadding: const EdgeInsets.all(12),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: const BorderSide(color: _surfaceContainer),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: const BorderSide(color: _surfaceContainer),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: const BorderSide(color: _primary, width: 1.5),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),

        // 4. CTA Button: GỬI AI ĐÁNH GIÁ SỨC KHỎE TOÀN DIỆN
        ElevatedButton(
          onPressed: _isEvaluatingAi ? null : _evaluateTotalOverviewWithAi,
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            minimumSize: const Size(double.infinity, 54),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            elevation: 3,
          ),
          child: _isEvaluatingAi
              ? const SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: Colors.white,
                  ),
                )
              : const Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.auto_awesome,
                      size: 20,
                      color: Colors.amberAccent,
                    ),
                    SizedBox(width: 8),
                    Text(
                      'GỬI AI ĐÁNH GIÁ SỨC KHỎE TOÀN DIỆN',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                        fontSize: 14,
                        letterSpacing: 0.2,
                      ),
                    ),
                  ],
                ),
        ),
      ],
    );
  }

  Widget _buildOverviewMetricTile({
    required IconData icon,
    required String title,
    required String? value,
    required DateTime? measuredAt,
    required String metricCode,
  }) {
    final hasValue = value != null && value.isNotEmpty;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: hasValue ? _surfaceAccent : _canvas,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              icon,
              color: hasValue ? _primary : _onSurfaceVariant,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  hasValue ? value : 'Chưa có dữ liệu',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: hasValue ? FontWeight.w700 : FontWeight.w400,
                    color: hasValue ? _onSurface : const Color(0xFFA0938E),
                  ),
                ),
                if (measuredAt != null)
                  Text(
                    'Đo lúc: ${_formatHistoryDateTime(measuredAt)}',
                    style: const TextStyle(
                      fontSize: 11,
                      color: _onSurfaceVariant,
                    ),
                  ),
              ],
            ),
          ),
          OutlinedButton(
            onPressed: () async {
              if (metricCode == 'EPDS_SCORE') {
                await Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => EpdsScreen(journeyId: widget.journeyId),
                  ),
                );
              } else {
                await context.push<bool>(
                  '/journeys/${Uri.encodeComponent(widget.journeyId)}/metrics/add'
                  '?metricType=${Uri.encodeQueryComponent(metricCode)}',
                );
              }
              if (mounted) {
                await _loadTrend();
              }
            },
            style: OutlinedButton.styleFrom(
              foregroundColor: _primary,
              side: const BorderSide(color: Color(0xFFD6C2BD)),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              minimumSize: const Size(64, 32),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            ),
            child: Text(
              hasValue ? 'Sửa' : '+ Nhập',
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _evaluateTotalOverviewWithAi() async {
    setState(() => _isEvaluatingAi = true);

    final sbp = _latestBp?.valueNumeric.round();
    final dbp = _latestBp?.valueSecondary?.round();
    final glucose = _latestGlucose?.valueNumeric;
    final kicks = _latestKicks?.valueNumeric.round();
    final heartRate = _latestHeartRate?.valueNumeric.round();
    final waterIntake = _latestHydration?.valueNumeric.round();
    final epds = _latestEpds?.valueNumeric.round();
    final bmi = _latestBmi?.valueNumeric;
    final temp = _latestTemp?.valueNumeric;

    final symptoms = <String>[];
    final noteText = _symptomNoteCtrl.text.toLowerCase();
    if (noteText.contains('đau đầu') || noteText.contains('nhức đầu'))
      symptoms.add('Đau đầu dữ dội');
    if (noteText.contains('hoa mắt') || noteText.contains('nhìn mờ'))
      symptoms.add('Hoa mắt nhìn mờ');
    if (noteText.contains('phù')) symptoms.add('Phù mặt/chân');
    if (noteText.contains('buồn nôn') || noteText.contains('nghén'))
      symptoms.add('Ốm nghén / Buồn nôn');
    if (noteText.contains('đau lưng')) symptoms.add('Đau mỏi lưng hông');
    if (noteText.contains('ra máu') || noteText.contains('chảy máu'))
      symptoms.add('Ra máu âm đạo');
    if (noteText.contains('rỉ ối') || noteText.contains('vỡ ối'))
      symptoms.add('Rỉ ối / Vỡ ối');

    for (final c in _surveyRiskConditions) {
      if (c == 'PRIOR_PREECLAMPSIA') symptoms.add('Tiền sử Tiền sản giật');
      if (c == 'CHRONIC_HYPERTENSION')
        symptoms.add('Bệnh nền: Tăng huyết áp mạn');
      if (c == 'PREGESTATIONAL_DIABETES' || c == 'PRIOR_GDM')
        symptoms.add('Tiền sử Đái tháo đường');
    }

    final gestationalAge = _journeyGestationalWeeks ?? 20;

    final payload = {
      'stage': 'PREGNANCY',
      'gestational_age_weeks': gestationalAge,
      if (sbp != null) 'systolic_bp': sbp,
      if (dbp != null) 'diastolic_bp': dbp,
      if (glucose != null) ...{
        'blood_glucose': glucose,
        'is_fasting_glucose': true,
      },
      if (kicks != null) ...{
        'fetal_movements_count': kicks,
        'fetal_movements_duration_hours': 2,
      },
      if (bmi != null) 'bmi': bmi,
      if (heartRate != null) 'heart_rate': heartRate,
      if (waterIntake != null) 'water_intake_ml': waterIntake,
      if (epds != null) 'epds_score': epds,
      if (temp != null) 'temperature': temp,
      if (_symptomNoteCtrl.text.isNotEmpty)
        'free_text_notes': _symptomNoteCtrl.text.trim(),
      'symptoms': symptoms,
    };

    Map<String, dynamic>? aiResult;
    for (final base in _pythonCandidates) {
      try {
        final res = await http
            .post(
              Uri.parse('$base/api/v1/metrics/evaluate'),
              headers: {'Content-Type': 'application/json'},
              body: jsonEncode(payload),
            )
            .timeout(const Duration(seconds: 5));

        if (res.statusCode == 200) {
          final data = jsonDecode(utf8.decode(res.bodyBytes));
          if (data is Map<String, dynamic>) {
            aiResult = data;
            break;
          }
        }
      } catch (_) {}
    }

    // Dự phòng thuật toán lâm sàng nếu offline
    if (aiResult == null) {
      final extraFactors = <String>[];
      if (sbp != null && (sbp >= 160 || (dbp != null && dbp >= 110))) {
        extraFactors.add(
          'Huyết áp rất cao ($sbp/$dbp mmHg) - Nguy cơ Tiền sản giật nặng / Đột quỵ thai kỳ',
        );
      }
      if (bmi != null && bmi >= 30.0) {
        extraFactors.add('Béo phì (BMI: ${bmi.toStringAsFixed(1)} kg/m²)');
      }
      if (waterIntake != null && waterIntake < 1200) {
        extraFactors.add('Lượng nước uống quá ít ($waterIntake ml/ngày)');
      }
      if (epds != null && epds >= 13) {
        extraFactors.add('Điểm trầm cảm EPDS cao ($epds/30 điểm)');
      }
      if (heartRate != null && heartRate >= 120) {
        extraFactors.add('Nhịp tim mẹ rất nhanh ($heartRate bpm)');
      }
      if (temp != null && temp >= 38.5) {
        extraFactors.add('Sốt cao (${temp.toStringAsFixed(1)}°C) - Nguy cơ nhiễm trùng ối / nhiễm khuẩn toàn thân');
      } else if (temp != null && temp >= 37.5) {
        extraFactors.add('Sốt nhẹ (${temp.toStringAsFixed(1)}°C) cần theo dõi');
      } else if (temp != null && temp < 35.5) {
        extraFactors.add('Thân nhiệt hạ thấp (${temp.toStringAsFixed(1)}°C)');
      }

      final isCritical =
          (sbp != null && (sbp >= 160 || (dbp != null && dbp >= 110))) ||
          (heartRate != null && heartRate >= 120) ||
          (temp != null && temp >= 38.5);

      aiResult = {
        'triage_status': isCritical
            ? 'CRITICAL_EMERGENCY'
            : (extraFactors.isNotEmpty ? 'ANOMALY_MONITOR' : 'NORMAL'),
        'risk_factors': extraFactors,
        'clinical_rationale': isCritical
            ? 'Phát hiện chỉ số sinh hiệu ở ngưỡng nguy hiểm khẩn cấp.'
            : (extraFactors.isNotEmpty
                  ? 'Các chỉ số có dấu hiệu bất thường cần tư vấn chuyên khoa.'
                  : 'Bức tranh sức khỏe toàn diện của mẹ hoàn toàn ổn định.'),
      };
    }

    if (!mounted) return;
    setState(() => _isEvaluatingAi = false);

    final status = aiResult['triage_status']?.toString() ?? 'NORMAL';
    final reasons =
        (aiResult['risk_factors'] as List?)
            ?.map((e) => e.toString())
            .toList() ??
        [];
    final advice =
        aiResult['clinical_rationale']?.toString() ??
        'Bức tranh sức khỏe toàn diện của mẹ hoàn toàn ổn định.';

    if (status == 'CRITICAL_EMERGENCY') {
      await _showCriticalEmergencyDialog(
        reasons: reasons.isEmpty ? ['Phát hiện chỉ số nguy cấp y tế'] : reasons,
        advice: advice,
      );
    } else if (status == 'ANOMALY_MONITOR') {
      await _showAnomalyDialog(
        reasons: reasons.isEmpty
            ? ['Chỉ số có dấu hiệu bất thường cần theo dõi']
            : reasons,
        advice: advice,
        attachedPayload: payload,
      );
    } else {
      await _showNormalSummaryDialog(gestationalAge, advice);
    }
  }

  Future<void> _showCriticalEmergencyDialog({
    required List<String> reasons,
    required String advice,
  }) async {
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.warning_amber_rounded,
                color: Color(0xFFBA1A1A),
                size: 26,
              ),
            ),
            const SizedBox(width: 10),
            const Expanded(
              child: Text(
                'CẢNH BÁO NGUY CẤP Y TẾ!',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFBA1A1A),
                ),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Hệ thống AI phát hiện các chỉ số sức khỏe của mẹ đang ở ngưỡng nguy hiểm:',
              style: TextStyle(fontSize: 13, color: Color(0xFF444444)),
            ),
            const SizedBox(height: 8),
            ...reasons.map(
              (r) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '• ',
                      style: TextStyle(
                        color: Color(0xFFBA1A1A),
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Expanded(
                      child: Text(
                        r,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFFBA1A1A),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF8F6),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFFFCDD2)),
              ),
              child: Text(
                advice,
                style: const TextStyle(
                  fontSize: 12,
                  color: Color(0xFF5A463F),
                  height: 1.3,
                ),
              ),
            ),
          ],
        ),
        actions: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFBA1A1A),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                icon: const Icon(Icons.emergency_outlined, size: 20),
                label: const Text(
                  'MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                ),
                onPressed: () async {
                  Navigator.of(dialogCtx).pop();
                  try {
                    final pos = await SafetyPermissionService()
                        .readConsentedLocation();
                    final lat = pos != null
                        ? double.parse(pos.latitude.toStringAsFixed(7))
                        : null;
                    final lng = pos != null
                        ? double.parse(pos.longitude.toStringAsFixed(7))
                        : null;
                    await EmergencyService().openFlow(
                      triggerSource: 'AI_TRIAGE',
                      latitude: lat,
                      longitude: lng,
                    );
                  } catch (_) {
                    try {
                      await EmergencyService().openFlow(
                        triggerSource: 'AI_TRIAGE',
                      );
                    } catch (_) {}
                  }
                  if (context.mounted) {
                    context.push('/emergency/map?mode=triage&stage=PREGNANCY');
                  }
                },
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFFBA1A1A),
                  side: const BorderSide(color: Color(0xFFBA1A1A), width: 1.5),
                  padding: const EdgeInsets.symmetric(vertical: 11),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                icon: const Icon(Icons.phone_in_talk, size: 18),
                label: const Text(
                  'GỌI CẤP CỨU 115 NGAY',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                ),
                onPressed: () async {
                  Navigator.of(dialogCtx).pop();
                  final url = Uri.parse('tel:115');
                  if (await canLaunchUrl(url)) await launchUrl(url);
                },
              ),
              TextButton(
                onPressed: () => Navigator.of(dialogCtx).pop(),
                child: const Text(
                  'Đóng',
                  style: TextStyle(color: Color(0xFF757575)),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _showAnomalyDialog({
    required List<String> reasons,
    required String advice,
    required Map<String, dynamic> attachedPayload,
  }) async {
    await showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF8E1),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.info_outline_rounded,
                color: Color(0xFFE65100),
                size: 26,
              ),
            ),
            const SizedBox(width: 10),
            const Expanded(
              child: Text(
                'LƯU Ý THEO DÕI SỨC KHỎE',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFE65100),
                ),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'AI phát hiện một số chỉ số cần theo dõi kỹ:',
              style: TextStyle(fontSize: 13, color: Color(0xFF444444)),
            ),
            const SizedBox(height: 8),
            ...reasons.map(
              (r) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '• ',
                      style: TextStyle(
                        color: Color(0xFFE65100),
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Expanded(
                      child: Text(
                        r,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFFE65100),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFFFFBEA),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFFFE082)),
              ),
              child: Text(
                advice,
                style: const TextStyle(
                  fontSize: 12,
                  color: Color(0xFF5A463F),
                  height: 1.3,
                ),
              ),
            ),
          ],
        ),
        actions: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                icon: const Icon(Icons.support_agent, size: 20),
                label: const Text(
                  'HỎI TRỢ LÝ AI NURSE (BƯỚC 10)',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                ),
                onPressed: () {
                  Navigator.of(dialogCtx).pop();
                  final vitals = <String>[];
                  if (_latestBp != null)
                    vitals.add(
                      'Huyết áp: ${_latestBp!.valueNumeric.round()}/${_latestBp!.valueSecondary?.round() ?? 0} mmHg',
                    );
                  if (_latestBmi != null)
                    vitals.add(
                      'BMI: ${_latestBmi!.valueNumeric.toStringAsFixed(1)} kg/m²',
                    );
                  if (_latestGlucose != null)
                    vitals.add(
                      'Đường huyết: ${_latestGlucose!.valueNumeric} mg/dL',
                    );
                  if (_latestKicks != null)
                    vitals.add(
                      'Cử động thai: ${_latestKicks!.valueNumeric.round()} lần/2h',
                    );
                  if (_latestHydration != null)
                    vitals.add(
                      'Lượng nước: ${_latestHydration!.valueNumeric.round()} ml',
                    );
                  if (_latestHeartRate != null)
                    vitals.add(
                      'Nhịp tim: ${_latestHeartRate!.valueNumeric.round()} bpm',
                    );
                  if (_latestEpds != null)
                    vitals.add('EPDS: ${_latestEpds!.valueNumeric.round()}/30');
                  if (_latestTemp != null)
                    vitals.add(
                      'Thân nhiệt: ${_latestTemp!.valueNumeric.toStringAsFixed(1)} °C',
                    );

                  context.push(
                    '/rag/chat',
                    extra: {
                      'attachedContext': {
                        'metricType': 'TOTAL_OVERVIEW',
                        'value': 'Tổng quan sức khỏe',
                        'unit': '',
                        'gestationalWeeks': _journeyGestationalWeeks ?? 20,
                        'reasons': reasons,
                        'latestVitals': vitals.join(' • '),
                      },
                      'initialMessage':
                          'Bức tranh sức khỏe toàn diện của em ở tuần thai ${_journeyGestationalWeeks ?? 20} có các dấu hiệu (${reasons.join(', ')}). Em cần có chế độ dinh dưỡng, nghỉ ngơi và theo dõi như thế nào?',
                    },
                  );
                },
              ),
              const SizedBox(height: 6),
              TextButton(
                onPressed: () => Navigator.of(dialogCtx).pop(),
                child: const Text(
                  'Đã hiểu',
                  style: TextStyle(color: Color(0xFF757575)),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _showNormalSummaryDialog(int week, String advice) async {
    await showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFFE8F5E9),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.check_circle_outline_rounded,
                color: Color(0xFF2E7D32),
                size: 26,
              ),
            ),
            const SizedBox(width: 10),
            const Expanded(
              child: Text(
                'SỨC KHỎE ỔN ĐỊNH & AN TOÀN',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2E7D32),
                ),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Bức tranh sức khỏe toàn diện của mẹ ở tuần thai thứ $week đang rất tốt!',
              style: const TextStyle(
                fontSize: 13,
                color: Color(0xFF444444),
                height: 1.4,
              ),
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFF1F8E9),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFC8E6C9)),
              ),
              child: Text(
                advice,
                style: const TextStyle(
                  fontSize: 12,
                  color: Color(0xFF2E7D32),
                  height: 1.3,
                ),
              ),
            ),
          ],
        ),
        actions: [
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF2E7D32),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            onPressed: () => Navigator.of(dialogCtx).pop(),
            child: const Text(
              'Tuyệt vời',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }
}

String _formatHistoryDateTime(DateTime dt) {
  final date =
      '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  final time =
      '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  return '$date, $time';
}

String _displayValue(MetricDataPoint point, _MetricOption metric) {
  if (metric.apiValue == 'EPDS_SCORE') {
    return '${point.valueNumeric.toStringAsFixed(0)}/30';
  }
  if (metric.apiValue == 'FETAL_MOVEMENT_SESSION') {
    final count = point.valueNumeric.toStringAsFixed(0);
    return '$count cử động';
  }
  if (point.valueSecondary != null) {
    if (metric.apiValue == 'BLOOD_PRESSURE_DIASTOLIC') {
      return '${point.valueSecondary!.toStringAsFixed(0)}/${point.valueNumeric.toStringAsFixed(0)}';
    }
    return '${point.valueNumeric.toStringAsFixed(0)}/${point.valueSecondary!.toStringAsFixed(0)}';
  }
  return point.valueDisplay;
}

class _MetricOption {
  final String apiValue;
  final String label;
  final String unit;
  final IconData icon;

  const _MetricOption({
    required this.apiValue,
    required this.label,
    required this.unit,
    required this.icon,
  });

  @override
  bool operator ==(Object other) {
    return other is _MetricOption && other.apiValue == apiValue;
  }

  @override
  int get hashCode => apiValue.hashCode;
}

class _ChartBar {
  final DateTime date;
  final double? value;

  const _ChartBar({required this.date, required this.value});
}

class _Card extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;

  const _Card({required this.child, this.padding = const EdgeInsets.all(20)});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: padding,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF845143).withAlpha(18),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _HistoryTile extends StatelessWidget {
  final MetricDataPoint point;
  final _MetricOption metric;
  final String unit;
  final VoidCallback onTap;

  const _HistoryTile({
    required this.point,
    required this.metric,
    required this.unit,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: _HealthMetricTrendScreenState._surfaceAccent,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(
                metric.icon,
                color: _HealthMetricTrendScreenState._primary,
                size: 22,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${_displayValue(point, metric)} $unit',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: _HealthMetricTrendScreenState._onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _formatHistoryDateTime(point.measuredAt),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _HealthMetricTrendScreenState._onSurfaceVariant,
                    ),
                  ),
                  if (metric.apiValue == 'BMI' &&
                      _bmiSourceMeasurements(point) != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      _bmiSourceMeasurements(point)!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: _HealthMetricTrendScreenState._primary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: _HealthMetricTrendScreenState._onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}

String? _bmiSourceMeasurements(MetricDataPoint point) {
  final weight = _contextNumber(point.context['weightKg']);
  final height = _contextNumber(point.context['heightCm']);
  if (weight == null || height == null) return null;
  return 'Cân nặng ${weight.toStringAsFixed(1)} kg • Chiều cao ${height.toStringAsFixed(1)} cm';
}

double? _contextNumber(Object? value) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '');
}
