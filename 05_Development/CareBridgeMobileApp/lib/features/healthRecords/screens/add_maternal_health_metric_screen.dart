import 'dart:convert';
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

class AddMaternalHealthMetricScreen extends StatefulWidget {
  final String journeyId;
  final String initialMetricType;
  final HealthMetricService? service;

  const AddMaternalHealthMetricScreen({
    super.key,
    required this.journeyId,
    required this.initialMetricType,
    this.service,
  });

  @override
  State<AddMaternalHealthMetricScreen> createState() =>
      _AddMaternalHealthMetricScreenState();
}

class _AddMaternalHealthMetricScreenState
    extends State<AddMaternalHealthMetricScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  final _primaryCtrl = TextEditingController();
  final _secondaryCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  final _protocolCtrl = TextEditingController(text: 'COUNT_10_MINUTES');
  final _gestationalAgeCtrl = TextEditingController();
  late final HealthMetricService _service;

  static final _bmiDecimalFormatter = TextInputFormatter.withFunction((
    oldValue,
    newValue,
  ) {
    if (newValue.text.isEmpty ||
        RegExp(r'^\d{0,3}(?:\.\d?)?$').hasMatch(newValue.text)) {
      return newValue;
    }
    return oldValue;
  });

  DateTime _measuredDate = DateTime.now();
  TimeOfDay _measuredTime = TimeOfDay.now();
  DateTime _periodStartDate = DateTime.now();
  TimeOfDay _periodStartTime = TimeOfDay.fromDateTime(
    DateTime.now().subtract(const Duration(minutes: 10)),
  );
  DateTime _periodEndDate = DateTime.now();
  TimeOfDay _periodEndTime = TimeOfDay.now();
  String _glucoseContext = 'FASTING';
  String _completionStatus = 'COMPLETED';
  List<MetricCapability> _capabilities = const [];
  bool _isLoadingCapabilities = true;
  bool _isSaving = false;
  String? _loadError;

  int? _journeyGestationalWeeks;
  List<String> _surveyRiskConditions = [];
  MetricDataPoint? _latestBp;
  MetricDataPoint? _latestBmi;
  MetricDataPoint? _latestGlucose;
  MetricDataPoint? _latestKicks;
  MetricDataPoint? _latestHydration;
  MetricDataPoint? _latestEpds;
  MetricDataPoint? _latestHeartRate;

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

  static const _glucoseContexts = <String, String>{
    'FASTING': 'Lúc đói',
    'PRE_MEAL': 'Trước ăn',
    'POST_MEAL_1H': 'Sau ăn 1 giờ',
    'POST_MEAL_2H': 'Sau ăn 2 giờ',
    'RANDOM': 'Ngẫu nhiên',
    'OTHER_APPROVED': 'Khác (đã được duyệt)',
  };

  static const _completionStatuses = <String, String>{
    'COMPLETED': 'Đã hoàn thành',
    'PARTIAL': 'Chưa hoàn thành',
  };

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? HealthMetricService();
    _loadCapabilities();
    _loadJourneyAndSurveyContext();
  }

  Future<void> _loadJourneyAndSurveyContext() async {
    try {
      final journeyRes = await apiGet('/api/v1/journey/dashboard');
      if (journeyRes is Map && mounted) {
        final data = journeyRes['data'] ?? journeyRes;
        final week = data['effectivePregnancyWeek'] ?? data['weekNumber'] ?? data['currentWeek'];
        if (week != null && week is int) {
          setState(() {
            _journeyGestationalWeeks = week;
            if (_gestationalAgeCtrl.text.isEmpty) {
              _gestationalAgeCtrl.text = '$week';
            }
          });
        }
      }
    } catch (_) {}

    try {
      final profileRes = await apiGet('/api/v1/recommendations/profile');
      if (profileRes is Map && mounted) {
        final data = (profileRes['data'] is Map) ? profileRes['data'] : profileRes;
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
        if (conditions.isNotEmpty && mounted) {
          setState(() {
            _surveyRiskConditions = conditions;
          });
        }
      }
    } catch (_) {}

    // Tự động tải snapshot các chỉ số mới nhất (Huyết áp, cử động thai, BMI, nước, nhịp tim, đường huyết, EPDS)
    try {
      final results = await Future.wait([
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'BLOOD_PRESSURE').catchError((_) => const MetricTrend(metricType: 'BLOOD_PRESSURE', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'BMI').catchError((_) => const MetricTrend(metricType: 'BMI', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'BLOOD_GLUCOSE').catchError((_) => const MetricTrend(metricType: 'BLOOD_GLUCOSE', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'FETAL_MOVEMENT_SESSION').catchError((_) => const MetricTrend(metricType: 'FETAL_MOVEMENT_SESSION', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'HYDRATION').catchError((_) => const MetricTrend(metricType: 'HYDRATION', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'EPDS_SCORE').catchError((_) => const MetricTrend(metricType: 'EPDS_SCORE', dataPoints: [])),
        _service.getMetricTrend(journeyId: widget.journeyId, metricType: 'HEART_RATE').catchError((_) => const MetricTrend(metricType: 'HEART_RATE', dataPoints: [])),
      ]);

      if (mounted) {
        setState(() {
          if (results[0].dataPoints.isNotEmpty) _latestBp = results[0].dataPoints.last;
          if (results[1].dataPoints.isNotEmpty) _latestBmi = results[1].dataPoints.last;
          if (results[2].dataPoints.isNotEmpty) _latestGlucose = results[2].dataPoints.last;
          if (results[3].dataPoints.isNotEmpty) _latestKicks = results[3].dataPoints.last;
          if (results[4].dataPoints.isNotEmpty) _latestHydration = results[4].dataPoints.last;
          if (results[5].dataPoints.isNotEmpty) _latestEpds = results[5].dataPoints.last;
          if (results[6].dataPoints.isNotEmpty) _latestHeartRate = results[6].dataPoints.last;
        });
      }
    } catch (_) {}
  }

  String get _metricType => _canonicalMetricType(widget.initialMetricType);

  bool get _isBloodPressure => _metricType == 'BLOOD_PRESSURE';
  bool get _isBmi => _metricType == 'BMI';
  bool get _isGlucose => _metricType == 'BLOOD_GLUCOSE';
  bool get _isFetalMovement => _metricType == 'FETAL_MOVEMENT_SESSION';
  bool get _isHydration => _metricType == 'HYDRATION';

  MetricCapability? get _capability {
    for (final capability in _capabilities) {
      if (capability.metricCode == _metricType) return capability;
    }
    return null;
  }

  bool get _isSupported => _capability?.manualEntrySupported == true;

  String get _title {
    if (_isBloodPressure) return 'Thêm huyết áp';
    if (_isFetalMovement) return 'Thêm cử động thai';
    return 'Thêm $_metricLabel';
  }

  String get _primaryLabel {
    if (_isBloodPressure) return 'Tâm thu (mmHg)';
    if (_isBmi) return 'Cân nặng (kg)';
    if (_isFetalMovement) return 'Số cử động';
    return '$_metricLabel ($_unit)';
  }

  String get _unit {
    switch (_metricType) {
      case 'BLOOD_PRESSURE':
        return 'mmHg';
      case 'BLOOD_GLUCOSE':
        return 'mg/dL';
      case 'BMI':
        return 'kg/m²';
      case 'TEMPERATURE':
        return '°C';
      case 'FETAL_MOVEMENT_SESSION':
        return 'count';
      case 'HYDRATION':
        return 'ml';
      case 'MATERNAL_HEART_RATE':
        return 'bpm';
      case 'EPDS_SCORE':
        return 'điểm';
      default:
        return _capability?.canonicalUnit.isNotEmpty == true
            ? _capability!.canonicalUnit
            : '';
    }
  }

  String get _metricLabel {
    switch (_metricType) {
      case 'BLOOD_PRESSURE':
        return 'huyết áp';
      case 'BLOOD_GLUCOSE':
        return 'đường huyết';
      case 'BMI':
        return 'chỉ số BMI';
      case 'TEMPERATURE':
        return 'nhiệt độ';
      case 'FETAL_MOVEMENT_SESSION':
        return 'cử động thai';
      case 'HYDRATION':
        return 'lượng nước uống';
      case 'MATERNAL_HEART_RATE':
        return 'nhịp tim';
      case 'EPDS_SCORE':
        return 'điểm sàng lọc trầm cảm EPDS';
      default:
        return 'chỉ số sức khỏe';
    }
  }

  String _canonicalMetricType(String value) {
    switch (value) {
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
      case 'EPDS':
      case 'EPDS_SCORE':
        return 'EPDS_SCORE';
      case 'TEMPERATURE':
        return 'TEMPERATURE';
      default:
        return value;
    }
  }

  Future<void> _loadCapabilities() async {
    try {
      final capabilities = await _service.getCapabilities(widget.journeyId);
      if (!mounted) return;
      setState(() {
        _capabilities = capabilities;
        _isLoadingCapabilities = false;
        _loadError = capabilities.any((item) => item.metricCode == _metricType)
            ? null
            : 'Chỉ số này không được hỗ trợ nhập thủ công trong hành trình hiện tại.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoadingCapabilities = false;
        _loadError = 'Không thể tải cấu hình chỉ số. Vui lòng thử lại.';
      });
    }
  }

  DateTime get _resolvedMeasuredAt => DateTime(
    _measuredDate.year,
    _measuredDate.month,
    _measuredDate.day,
    _measuredTime.hour,
    _measuredTime.minute,
  );

  DateTime get _resolvedPeriodStart => DateTime(
    _periodStartDate.year,
    _periodStartDate.month,
    _periodStartDate.day,
    _periodStartTime.hour,
    _periodStartTime.minute,
  );

  DateTime get _resolvedPeriodEnd => DateTime(
    _periodEndDate.year,
    _periodEndDate.month,
    _periodEndDate.day,
    _periodEndTime.hour,
    _periodEndTime.minute,
  );

  @override
  void dispose() {
    _primaryCtrl.dispose();
    _secondaryCtrl.dispose();
    _noteCtrl.dispose();
    _protocolCtrl.dispose();
    _gestationalAgeCtrl.dispose();
    super.dispose();
  }

  Future<DateTime?> _pickDate(DateTime initialDate) async {
    final now = DateTime.now();
    return showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now,
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
  }

  Future<TimeOfDay?> _pickTime(TimeOfDay initialTime) async {
    return showTimePicker(
      context: context,
      initialTime: initialTime,
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
  }

  Future<void> _pickMeasuredDate() async {
    final picked = await _pickDate(_measuredDate);
    if (picked != null) setState(() => _measuredDate = picked);
  }

  Future<void> _pickMeasuredTime() async {
    final picked = await _pickTime(_measuredTime);
    if (picked != null) setState(() => _measuredTime = picked);
  }

  Future<void> _pickPeriodStartTime() async {
    final picked = await _pickTime(_periodStartTime);
    if (picked != null) setState(() => _periodStartTime = picked);
  }

  Future<void> _pickPeriodEndTime() async {
    final picked = await _pickTime(_periodEndTime);
    if (picked != null) setState(() => _periodEndTime = picked);
  }

  String? _requiredPositive(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || !parsed.isFinite || parsed <= 0) {
      return 'Nhập giá trị hợp lệ';
    }
    return null;
  }

  String? _requiredNonNegative(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || parsed < 0) return 'Nhập số hợp lệ';
    return null;
  }

  String? _requiredText(String? value) {
    if (value == null || value.trim().isEmpty) return 'Thông tin bắt buộc';
    return null;
  }

  Future<void> _save() async {
    if (!_isSupported) {
      _showError('Chỉ số này không được hỗ trợ nhập thủ công.');
      return;
    }
    if (!_formKey.currentState!.validate()) return;

    final measuredAt = _isFetalMovement
        ? _resolvedPeriodEnd
        : _resolvedMeasuredAt;
    if (measuredAt.isAfter(DateTime.now().add(const Duration(minutes: 5)))) {
      _showError('Thời điểm đo không được ở tương lai quá 5 phút.');
      return;
    }

    if (_isFetalMovement && !_resolvedPeriodEnd.isAfter(_resolvedPeriodStart)) {
      _showError('Thời điểm kết thúc phải sau thời điểm bắt đầu.');
      return;
    }

    if (_isBloodPressure) {
      final systolic = double.tryParse(_primaryCtrl.text.trim());
      final diastolic = double.tryParse(_secondaryCtrl.text.trim());
      if (systolic == null || diastolic == null || systolic <= diastolic) {
        _showError('Huyết áp tâm thu phải lớn hơn huyết áp tâm trương.');
        return;
      }
    }

    final rawWeight = _primaryCtrl.text.trim();
    final rawHeight = _secondaryCtrl.text.trim();
    final weightKg = _isBmi ? double.tryParse(rawWeight) : null;
    final heightCm = _isBmi ? double.tryParse(rawHeight) : null;
    if (_isBmi &&
        (weightKg == null ||
            weightKg < 20 ||
            weightKg > 300 ||
            heightCm == null ||
            heightCm < 100 ||
            heightCm > 250 ||
            !_hasAtMostOneFractionalDigit(rawWeight) ||
            !_hasAtMostOneFractionalDigit(rawHeight))) {
      _showError(
        'Nhập cân nặng 20–300 kg và chiều cao 100–250 cm, tối đa 1 chữ số thập phân.',
      );
      return;
    }

    setState(() => _isSaving = true);
    try {
      final contextPayload = <String, dynamic>{};
      if (_isGlucose) contextPayload['measurementContext'] = _glucoseContext;
      if (_isFetalMovement) {
        contextPayload['protocolCode'] = _protocolCtrl.text.trim();
        contextPayload['completionStatus'] = _completionStatus;
        contextPayload['gestationalAgeSnapshot'] = _gestationalAgeCtrl.text
            .trim();
      }
      if (_isBmi) {
        contextPayload['weightKg'] = weightKg;
        contextPayload['heightCm'] = heightCm;
        contextPayload['pregnancyBasis'] = 'CURRENT_MEASUREMENT';
      }

      final bmi = _isBmi
          ? weightKg! / ((heightCm! / 100) * (heightCm / 100))
          : null;

      final primaryVal = bmi ?? double.parse(_primaryCtrl.text.trim());
      final secondaryVal = _isBloodPressure
          ? double.parse(_secondaryCtrl.text.trim())
          : null;

      // 1. Lưu chỉ số vào Backend Database
      await _service.addMetric(
        widget.journeyId,
        AddMetricRequest(
          metricType: _metricType,
          valueNumeric: primaryVal,
          valueSecondary: secondaryVal,
          unit: _unit,
          measuredAt: measuredAt,
          note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
          context: contextPayload,
          periodStart: _isFetalMovement ? _resolvedPeriodStart : null,
          periodEnd: _isFetalMovement ? _resolvedPeriodEnd : null,
          definitionVersion: _capability?.version,
        ),
      );

      // 2. Chạy AI Triage Sàng lọc Rủi ro Lâm sàng
      final evalResult = await _evaluateMetricWithAi(
        primaryVal: primaryVal,
        secondaryVal: secondaryVal,
        note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
      );

      if (!mounted) return;

      final riskStatus = evalResult['status']?.toString();
      final isEmergency =
          evalResult['emergency_mode'] == true ||
          riskStatus == 'CRITICAL_EMERGENCY';
      final isAnomaly = riskStatus == 'ANOMALY_MONITOR';

      if (isEmergency) {
        await _showCriticalEmergencyDialog(context, evalResult);
      } else if (isAnomaly) {
        await _showAnomalyWarningDialog(context, evalResult);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã thêm chỉ số thành công. Tình trạng hoàn toàn bình thường!'),
            backgroundColor: Color(0xFF2E7D32),
          ),
        );
        Navigator.of(context).pop(true);
      }
    } on ApiException catch (error) {
      _showError(_metricSaveError(error));
    } catch (_) {
      _showError('Không thể lưu chỉ số. Vui lòng kiểm tra dữ liệu và thử lại.');
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  bool _hasAtMostOneFractionalDigit(String value) =>
      RegExp(r'^\d+(?:\.\d)?$').hasMatch(value);

  void _showError(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _error));
  }

  String _metricSaveError(ApiException error) {
    return switch (error.errorCode) {
      'METRIC-032' => 'Huyết áp tâm thu phải lớn hơn huyết áp tâm trương.',
      'METRIC-033' => 'Đơn vị đo không hợp lệ. Vui lòng thử lại.',
      'METRIC-038' => 'Giá trị chỉ số phải lớn hơn 0.',
      'METRIC-004' => 'Thời điểm đo không được ở tương lai quá 5 phút.',
      _ => 'Không thể lưu chỉ số. Vui lòng kiểm tra dữ liệu và thử lại.',
    };
  }

  Future<Map<String, dynamic>> _evaluateMetricWithAi({
    required double primaryVal,
    double? secondaryVal,
    String? note,
  }) async {
    final sbp = _isBloodPressure ? primaryVal.round() : null;
    final dbp = _isBloodPressure ? (secondaryVal?.round() ?? 0) : null;
    final glucose = _isGlucose ? primaryVal : null;
    final kicks = _isFetalMovement ? primaryVal.round() : null;
    final heartRate = (_metricType == 'HEART_RATE' || _metricType == 'MATERNAL_HEART_RATE') ? primaryVal.round() : null;
    final waterIntake = _isHydration ? primaryVal.round() : null;
    final epds = (_metricType == 'EPDS_SCORE' || _metricType == 'EPDS') ? primaryVal.round() : null;

    final rawWeight = _isBmi ? double.tryParse(_primaryCtrl.text.trim()) : null;
    final rawHeight = _isBmi ? double.tryParse(_secondaryCtrl.text.trim()) : null;
    final bmi = _isBmi
        ? (rawWeight != null && rawHeight != null && rawHeight > 0
            ? rawWeight / ((rawHeight / 100) * (rawHeight / 100))
            : primaryVal)
        : null;

    final symptoms = <String>[];
    final noteText = (note ?? '').toLowerCase();
    if (noteText.contains('đau đầu') || noteText.contains('nhức đầu')) {
      symptoms.add('Đau đầu dữ dội');
    }
    if (noteText.contains('hoa mắt') || noteText.contains('nhìn mờ')) {
      symptoms.add('Hoa mắt nhìn mờ');
    }
    if (noteText.contains('phù')) symptoms.add('Phù mặt/chân');
    if (noteText.contains('buồn nôn') || noteText.contains('nghén')) {
      symptoms.add('Ốm nghén / Buồn nôn');
    }
    if (noteText.contains('đau lưng')) symptoms.add('Đau mỏi lưng hông');
    if (noteText.contains('ra máu') || noteText.contains('chảy máu')) {
      symptoms.add('Ra máu âm đạo');
    }
    if (noteText.contains('rỉ ối') || noteText.contains('vỡ ối')) {
      symptoms.add('Rỉ ối / Vỡ ối');
    }

    // Kết hợp thông tin tiền sử/khảo sát survey đã lưu
    for (final c in _surveyRiskConditions) {
      if (c == 'PRIOR_PREECLAMPSIA') symptoms.add('Tiền sử Tiền sản giật');
      if (c == 'CHRONIC_HYPERTENSION') symptoms.add('Bệnh nền: Tăng huyết áp mạn');
      if (c == 'PREGESTATIONAL_DIABETES' || c == 'PRIOR_GDM') {
        symptoms.add('Tiền sử Đái tháo đường');
      }
    }

    final gestationalAge =
        _journeyGestationalWeeks ??
        int.tryParse(_gestationalAgeCtrl.text.replaceAll(RegExp(r'\D'), '')) ??
        20;

    final payload = {
      'stage': 'PREGNANCY',
      'gestational_age_weeks': gestationalAge,
      if (sbp != null) 'systolic_bp': sbp,
      if (dbp != null) 'diastolic_bp': dbp,
      if (glucose != null) ...{
        'blood_glucose': glucose,
        'is_fasting_glucose': _glucoseContext == 'FASTING',
      },
      if (kicks != null) ...{
        'fetal_movements_count': kicks,
        'fetal_movements_duration_hours': 2,
      },
      if (bmi != null) 'bmi': bmi,
      if (rawWeight != null) 'weight_kg': rawWeight,
      if (rawHeight != null) 'height_cm': rawHeight,
      if (heartRate != null) 'heart_rate': heartRate,
      if (waterIntake != null) 'water_intake_ml': waterIntake,
      if (epds != null) 'epds_score': epds,
      if (note != null && note.isNotEmpty) 'free_text_notes': note,
      'symptoms': symptoms,
    };

    // 1. Thử gọi Python FastAPI AI Service
    for (final base in _pythonCandidates) {
      try {
        final res = await http
            .post(
              Uri.parse('$base/api/v1/metrics/evaluate'),
              headers: {'Content-Type': 'application/json'},
              body: jsonEncode(payload),
            )
            .timeout(const Duration(seconds: 4));

        if (res.statusCode == 200) {
          final data = jsonDecode(utf8.decode(res.bodyBytes));
          if (data is Map<String, dynamic>) {
            return data;
          }
        }
      } catch (_) {}
    }

    // 2. Dự phòng thuật toán lâm sàng cục bộ
    final extraFactors = <String>[];
    if (bmi != null) {
      if (bmi >= 40.0) {
        extraFactors.add('Béo phì độ III rất nặng (BMI: ${bmi.toStringAsFixed(1)} kg/m²) - Nguy cơ cao Tiền sản giật, ĐTĐ thai kỳ');
      } else if (bmi >= 30.0) {
        extraFactors.add('Béo phì thai kỳ (BMI: ${bmi.toStringAsFixed(1)} kg/m²) - Cần khám dinh dưỡng và theo dõi sát huyết áp, đường huyết');
      } else if (bmi >= 25.0) {
        extraFactors.add('Thừa cân thai kỳ (BMI: ${bmi.toStringAsFixed(1)} kg/m²) - Cần kiểm soát mức tăng cân hợp lý');
      } else if (bmi < 18.5) {
        extraFactors.add('Thiếu cân thai kỳ (BMI: ${bmi.toStringAsFixed(1)} kg/m²) - Nguy cơ suy dinh dưỡng bào thai hoặc sinh non');
      }
    }
    if (waterIntake != null && waterIntake < 1500) {
      extraFactors.add('Lượng nước uống ít ($waterIntake ml/ngày, chuẩn 2000-2500ml)');
    }
    if (epds != null && epds >= 10) {
      extraFactors.add('Điểm trầm cảm/tâm trạng EPDS cao ($epds/30 điểm)');
    }
    if (heartRate != null && (heartRate > 100 || heartRate < 50)) {
      extraFactors.add('Nhịp tim bất thường ($heartRate bpm)');
    }

    final isCritical =
        (sbp != null && sbp >= 160) ||
        (dbp != null && dbp >= 110) ||
        ((sbp != null && sbp >= 140 || dbp != null && dbp >= 90) &&
            symptoms.any(
              (s) =>
                  s.contains('Đau đầu') ||
                  s.contains('Hoa mắt') ||
                  s.contains('Tiền sản giật'),
            )) ||
        (kicks != null && kicks == 0 && gestationalAge >= 28) ||
        (symptoms.any((s) => s.contains('Ra máu') || s.contains('Vỡ ối')));

    final isAnomaly =
        (sbp != null && sbp >= 130) ||
        (dbp != null && dbp >= 85) ||
        (glucose != null && glucose >= 5.1) ||
        (kicks != null && kicks < 4 && gestationalAge >= 28) ||
        extraFactors.isNotEmpty ||
        symptoms.isNotEmpty;

    if (isCritical) {
      return {
        'status': 'CRITICAL_EMERGENCY',
        'emergency_mode': true,
        'headline': 'CẢNH BÁO: Phát hiện chỉ số / dấu hiệu nguy hiểm khẩn cấp!',
        'summary':
            'Chỉ số sinh hiệu hoặc triệu chứng của mẹ đang ở ngưỡng báo động cao. Cần kích hoạt chế độ khẩn cấp, liên hệ ngay cơ sở y tế hoặc khoa Cấp cứu Sản gần nhất.',
        'risk_factors': [
          if (sbp != null && sbp >= 160)
            'Huyết áp rất cao ($sbp/$dbp mmHg) - Nguy cơ Tiền sản giật nặng / Đột quỵ thai kỳ',
          if (kicks != null && kicks == 0)
            'Mất cử động thai ở tuần thứ $gestationalAge',
          ...extraFactors,
          ...symptoms,
        ],
        'suggested_action':
            'KÍCH HOẠT CHẾ ĐỘ CẤP CỨU: Gọi 115 hoặc di chuyển ngay đến Bệnh viện chuyên khoa Sản gần nhất.',
      };
    } else if (isAnomaly) {
      return {
        'status': 'ANOMALY_MONITOR',
        'emergency_mode': false,
        'headline': 'Lưu ý: Chỉ số có dấu hiệu bất thường nhẹ cần theo dõi',
        'summary':
            'Phát hiện yếu tố cần lưu ý trong các chỉ số mẹ vừa nhập. Mẹ nên trao đổi thêm với AI Nurse Assistant để được hướng dẫn chi tiết hoặc đặt lịch khám Bác sĩ.',
        'risk_factors': [
          if (sbp != null && sbp >= 130)
            'Huyết áp hơi cao ($sbp/$dbp mmHg) so với bình thường',
          if (glucose != null && glucose >= 5.1)
            'Đường huyết cao ($glucose mg/dL) cần theo dõi',
          ...extraFactors,
          ...symptoms,
        ],
        'suggested_action':
            'Trò chuyện với AI Nurse Assistant để làm rõ triệu chứng hoặc Đặt lịch hẹn Bác sĩ tư vấn.',
      };
    } else {
      return {
        'status': 'NORMAL',
        'emergency_mode': false,
        'headline': 'Tuyệt vời: Các chỉ số sinh hiệu hoàn toàn bình thường',
        'summary':
            'Tất cả các chỉ số sinh hiệu của mẹ đều nằm trong giới hạn an toàn theo hướng dẫn y tế thai kỳ.',
        'risk_factors': <String>[],
        'suggested_action':
            'Tiếp tục theo dõi sức khỏe và thực hiện kế hoạch chăm sóc hàng ngày.',
      };
    }
  }

  Future<void> _showCriticalEmergencyDialog(
    BuildContext context,
    Map<String, dynamic> eval,
  ) async {
    final headline =
        eval['headline']?.toString() ??
        'CẢNH BÁO: Phát hiện chỉ số / dấu hiệu nguy hiểm khẩn cấp!';
    final summary =
        eval['summary']?.toString() ??
        'Chỉ số sinh hiệu đang ở ngưỡng báo động cao. Cần kích hoạt cấp cứu hoặc đến Bệnh viện ngay.';
    final riskFactors =
        (eval['risk_factors'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList() ??
        [];

    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder:
          (dialogCtx) => AlertDialog(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
            ),
            backgroundColor: const Color(0xFFFFF8F6),
            contentPadding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFDAD6),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: const Color(0xFFBA1A1A),
                      width: 1.5,
                    ),
                  ),
                  child: const Row(
                    children: [
                      Icon(
                        Icons.warning_amber_rounded,
                        color: Color(0xFFBA1A1A),
                        size: 28,
                      ),
                      SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'CẢNH BÁO NGUY CẤP Y TẾ',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF410002),
                            letterSpacing: 0.3,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                Text(
                  headline,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFBA1A1A),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  summary,
                  style: const TextStyle(
                    fontSize: 13,
                    color: Color(0xFF271812),
                    height: 1.4,
                  ),
                ),
                if (riskFactors.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: const Color(0xFFF1D5CF)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Yếu tố nguy cơ phát hiện:',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFBA1A1A),
                          ),
                        ),
                        const SizedBox(height: 4),
                        ...riskFactors.map(
                          (rf) => Padding(
                            padding: const EdgeInsets.only(top: 2),
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
                                    rf,
                                    style: const TextStyle(
                                      fontSize: 12,
                                      color: Color(0xFF524440),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                // Nút 1: Mở Emergency Map Screen (115 + Còi SOS + Chia sẻ GPS Gia đình + Bệnh viện gần nhất)
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFBA1A1A),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 13),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    elevation: 2,
                  ),
                  icon: const Icon(Icons.emergency_outlined, size: 20),
                  label: const Text(
                    'MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                  ),
                  onPressed: () async {
                    Navigator.of(dialogCtx).pop();
                    Navigator.of(context).pop(true);
                    try {
                      final pos = await SafetyPermissionService().readConsentedLocation();
                      final lat = pos != null ? double.parse(pos.latitude.toStringAsFixed(7)) : null;
                      final lng = pos != null ? double.parse(pos.longitude.toStringAsFixed(7)) : null;
                      await EmergencyService().openFlow(
                        triggerSource: 'AI_TRIAGE',
                        latitude: lat,
                        longitude: lng,
                      );
                    } catch (_) {
                      try {
                        await EmergencyService().openFlow(triggerSource: 'AI_TRIAGE');
                      } catch (_) {}
                    }
                    if (context.mounted) {
                      context.push('/emergency/map?mode=triage&stage=PREGNANCY');
                    }
                  },
                ),
                const SizedBox(height: 8),
                // Nút 2: Quay số 115
                OutlinedButton.icon(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFBA1A1A),
                    side: const BorderSide(
                      color: Color(0xFFBA1A1A),
                      width: 1.5,
                    ),
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
                  onPressed: () => launchUrl(Uri.parse('tel:115')),
                ),
                const SizedBox(height: 4),
                TextButton(
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    Navigator.of(context).pop(true);
                  },
                  child: const Text(
                    'Đã hiểu / Bỏ qua',
                    style: TextStyle(color: Colors.grey, fontSize: 12),
                  ),
                ),
              ],
            ),
          ),
    );
  }

  Future<void> _showAnomalyWarningDialog(
    BuildContext context,
    Map<String, dynamic> eval,
  ) async {
    final headline =
        eval['headline']?.toString() ??
        'Lưu ý: Chỉ số có dấu hiệu bất thường nhẹ cần theo dõi';
    final summary =
        eval['summary']?.toString() ??
        'Phát hiện yếu tố cần lưu ý trong các chỉ số mẹ vừa nhập.';
    final riskFactors =
        (eval['risk_factors'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList() ??
        [];

    final gestationalAge =
        _journeyGestationalWeeks ??
        int.tryParse(_gestationalAgeCtrl.text.replaceAll(RegExp(r'\D'), '')) ??
        20;
    final metricLabel = _metricLabel;
    String displayValue = '';
    if (_isBloodPressure) {
      displayValue = '${_primaryCtrl.text.trim()}/${_secondaryCtrl.text.trim()} mmHg';
    } else if (_isBmi) {
      final w = double.tryParse(_primaryCtrl.text.trim());
      final h = double.tryParse(_secondaryCtrl.text.trim());
      final bmiVal = (w != null && h != null && h > 0) ? (w / ((h / 100) * (h / 100))) : null;
      displayValue = '${_primaryCtrl.text.trim()}kg, ${_secondaryCtrl.text.trim()}cm' + (bmiVal != null ? ' (BMI: ${bmiVal.toStringAsFixed(1)})' : '');
    } else {
      displayValue = '${_primaryCtrl.text.trim()} $_unit';
    }

    final reasonList = riskFactors.isNotEmpty ? riskFactors.join(', ') : headline;
    final promptText = 'Tôi vừa đo $metricLabel là $displayValue ở tuần thai thứ $gestationalAge. AI phát hiện dấu hiệu cần lưu ý: $reasonList. Bác sĩ / AI Nurse có thể tư vấn giúp tôi chế độ ăn uống, nghỉ ngơi và các dấu hiệu cần theo dõi không?';

    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder:
          (dialogCtx) => AlertDialog(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
            ),
            backgroundColor: const Color(0xFFFFFBF8),
            contentPadding: const EdgeInsets.fromLTRB(20, 20, 20, 16),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF1EC),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: const Color(0xFFC98C7B),
                      width: 1.5,
                    ),
                  ),
                  child: const Row(
                    children: [
                      Icon(
                        Icons.health_and_safety_outlined,
                        color: Color(0xFF845143),
                        size: 26,
                      ),
                      SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'LƯU Ý THEO DÕI SỨC KHỎE',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF845143),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                Text(
                  headline,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF845143),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  summary,
                  style: const TextStyle(
                    fontSize: 13,
                    color: Color(0xFF271812),
                    height: 1.4,
                  ),
                ),
                if (riskFactors.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: const Color(0xFFF0E3DE)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Yếu tố cần lưu ý:',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF845143),
                          ),
                        ),
                        const SizedBox(height: 4),
                        ...riskFactors.map(
                          (rf) => Padding(
                            padding: const EdgeInsets.only(top: 2),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  '• ',
                                  style: TextStyle(
                                    color: Color(0xFF845143),
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Expanded(
                                  child: Text(
                                    rf,
                                    style: const TextStyle(
                                      fontSize: 12,
                                      color: Color(0xFF524440),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                // Nút 1: Chuyển sang chat AI Nurse Assistant kèm ngữ cảnh tự động
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 13),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  icon: const Icon(Icons.chat_bubble_outline, size: 18),
                  label: const Text(
                    'HỎI TRỢ LÝ AI NURSE (BƯỚC 10)',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    Navigator.of(context).pop(true);
                    final vitalsMap = <String, String>{};
                    if (_latestBp != null) vitalsMap['Huyết áp'] = '${_latestBp!.valueDisplay} mmHg';
                    if (_latestBmi != null) vitalsMap['BMI'] = _latestBmi!.valueDisplay;
                    if (_latestGlucose != null) vitalsMap['Đường huyết'] = '${_latestGlucose!.valueDisplay} mmol/L';
                    if (_latestKicks != null) vitalsMap['Cử động thai'] = '${_latestKicks!.valueDisplay} lần/2h';
                    if (_latestHydration != null) vitalsMap['Lượng nước'] = '${_latestHydration!.valueDisplay} ml';
                    if (_latestEpds != null) vitalsMap['Tâm trạng EPDS'] = '${_latestEpds!.valueDisplay}/30 đ';
                    if (_latestHeartRate != null) vitalsMap['Nhịp tim'] = '${_latestHeartRate!.valueDisplay} bpm';

                    context.push(
                      '/rag/chat',
                      extra: {
                        'prompt': promptText,
                        'attachedContext': {
                          'metricLabel': metricLabel,
                          'displayValue': displayValue,
                          'gestationalAge': gestationalAge,
                          'riskFactors': riskFactors,
                          'latestVitals': vitalsMap,
                        },
                        'autoSend': true,
                      },
                    );
                  },
                ),
                const SizedBox(height: 8),
                OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFF524440),
                    side: const BorderSide(color: Color(0xFFD6C2BD)),
                    padding: const EdgeInsets.symmetric(vertical: 11),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    Navigator.of(context).pop(true);
                  },
                  child: const Text(
                    'Đã hiểu, tôi sẽ tiếp tục theo dõi',
                    style: TextStyle(fontSize: 13),
                  ),
                ),
              ],
            ),
          ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        foregroundColor: _onSurface,
        title: Text(
          _title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: SafeArea(
        child: _isLoadingCapabilities
            ? const Center(child: CircularProgressIndicator(color: _primary))
            : _loadError != null
            ? _buildErrorState()
            : SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildMetricDescription(),
                      const SizedBox(height: 14),
                      TextFormField(
                        key: const Key('maternal-metric-primary'),
                        controller: _primaryCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                          if (_isBmi) _bmiDecimalFormatter,
                        ],
                        validator: _isFetalMovement
                            ? _requiredNonNegative
                            : _requiredPositive,
                        onChanged: _isBmi ? (_) => setState(() {}) : null,
                        decoration: _inputDecoration(_primaryLabel),
                      ),
                      if (_isBloodPressure || _isBmi) ...[
                        const SizedBox(height: 14),
                        TextFormField(
                          key: const Key('maternal-metric-secondary'),
                          controller: _secondaryCtrl,
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          inputFormatters: [
                            FilteringTextInputFormatter.allow(
                              RegExp(r'[0-9.]'),
                            ),
                            if (_isBmi) _bmiDecimalFormatter,
                          ],
                          validator: _requiredPositive,
                          onChanged: _isBmi ? (_) => setState(() {}) : null,
                          decoration: _inputDecoration(
                            _isBmi ? 'Chiều cao (cm)' : 'Tâm trương (mmHg)',
                          ),
                        ),
                      ],
                      if (_isBmi && _bmiPreview != null) ...[
                        const SizedBox(height: 14),
                        _buildBmiPreview(_bmiPreview!),
                      ],
                      if (_isGlucose) ...[
                        const SizedBox(height: 14),
                        _buildDropdown<String>(
                          label: 'Bối cảnh đo',
                          value: _glucoseContext,
                          items: _glucoseContexts,
                          onChanged: (value) {
                            if (value != null) {
                              setState(() => _glucoseContext = value);
                            }
                          },
                        ),
                      ],
                      if (_isFetalMovement) ..._buildFetalMovementFields(),
                      if (!_isFetalMovement) ...[
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            Expanded(
                              child: _PickerTile(
                                label: 'Ngày đo',
                                value: _formatDate(_measuredDate),
                                onTap: _pickMeasuredDate,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _PickerTile(
                                label: 'Giờ đo',
                                value: _measuredTime.format(context),
                                onTap: _pickMeasuredTime,
                              ),
                            ),
                          ],
                        ),
                      ],
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _noteCtrl,
                        minLines: 3,
                        maxLines: 5,
                        decoration: _inputDecoration('Ghi chú'),
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton(
                        key: const Key('maternal-metric-save'),
                        onPressed: _isSaving ? null : _save,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _primary,
                          foregroundColor: Colors.white,
                          minimumSize: const Size(double.infinity, 52),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        child: _isSaving
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.4,
                                  color: Colors.white,
                                ),
                              )
                            : const Text(
                                'Lưu chỉ số',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                      ),
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.info_outline_rounded, color: _error, size: 44),
            const SizedBox(height: 12),
            Text(
              _loadError!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: _loadCapabilities,
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricDescription() {
    return Text(
      _isBmi
          ? 'BMI được tính từ cân nặng và chiều cao bạn nhập. Trong thai kỳ, BMI trước khi mang thai mới là mốc chính để bác sĩ khuyến nghị mức tăng cân; kết quả hiện tại chỉ mang tính theo dõi tham khảo.'
          : _isHydration
          ? 'Ghi nhận từng lần uống để theo dõi tiến độ trong ngày. Mục tiêu nước cần được cá nhân hoá theo tư vấn của bác sĩ.'
          : 'Dữ liệu được lưu để theo dõi, không thay thế chẩn đoán y khoa.',
      style: TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        color: _onSurfaceVariant,
      ),
    );
  }

  double? get _bmiPreview {
    final weight = double.tryParse(_primaryCtrl.text.trim());
    final heightCm = double.tryParse(_secondaryCtrl.text.trim());
    if (weight == null || heightCm == null || weight <= 0 || heightCm <= 0) {
      return null;
    }
    final heightMeters = heightCm / 100;
    return weight / (heightMeters * heightMeters);
  }

  Widget _buildBmiPreview(double bmi) {
    final classification = switch (bmi) {
      < 18.5 => 'Thiếu cân',
      < 25 => 'Bình thường',
      < 30 => 'Thừa cân',
      _ => 'Béo phì',
    };
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(
        'BMI dự kiến: ${bmi.toStringAsFixed(1)} • $classification',
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontWeight: FontWeight.w700,
          color: _primary,
        ),
      ),
    );
  }

  List<Widget> _buildFetalMovementFields() {
    return [
      const SizedBox(height: 14),
      TextFormField(
        controller: _protocolCtrl,
        validator: _requiredText,
        decoration: _inputDecoration('Mã quy trình'),
      ),
      const SizedBox(height: 14),
      TextFormField(
        controller: _gestationalAgeCtrl,
        validator: _requiredText,
        decoration: _inputDecoration('Tuổi thai tại thời điểm đo'),
      ),
      const SizedBox(height: 14),
      _buildDropdown<String>(
        label: 'Trạng thái phiên đo',
        value: _completionStatus,
        items: _completionStatuses,
        onChanged: (value) {
          if (value != null) setState(() => _completionStatus = value);
        },
      ),
      const SizedBox(height: 14),
      Row(
        children: [
          Expanded(
            child: _PickerTile(
              label: 'Bắt đầu',
              value:
                  '${_formatDate(_periodStartDate)} ${_periodStartTime.format(context)}',
              onTap: () async {
                final picked = await _pickDate(_periodStartDate);
                if (picked != null) setState(() => _periodStartDate = picked);
              },
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _PickerTile(
              label: 'Kết thúc',
              value:
                  '${_formatDate(_periodEndDate)} ${_periodEndTime.format(context)}',
              onTap: () async {
                final picked = await _pickDate(_periodEndDate);
                if (picked != null) setState(() => _periodEndDate = picked);
              },
            ),
          ),
        ],
      ),
      const SizedBox(height: 8),
      Row(
        children: [
          Expanded(
            child: OutlinedButton(
              onPressed: _pickPeriodStartTime,
              child: Text('Giờ bắt đầu: ${_periodStartTime.format(context)}'),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: OutlinedButton(
              onPressed: _pickPeriodEndTime,
              child: Text('Giờ kết thúc: ${_periodEndTime.format(context)}'),
            ),
          ),
        ],
      ),
    ];
  }

  Widget _buildDropdown<T>({
    required String label,
    required T value,
    required Map<T, String> items,
    required ValueChanged<T?> onChanged,
  }) {
    return DropdownButtonFormField<T>(
      initialValue: value,
      decoration: _inputDecoration(label),
      items: items.entries
          .map(
            (entry) =>
                DropdownMenuItem<T>(value: entry.key, child: Text(entry.value)),
          )
          .toList(),
      onChanged: onChanged,
    );
  }

  String _formatDate(DateTime value) {
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
  }

  InputDecoration _inputDecoration(String label) {
    return InputDecoration(
      labelText: label,
      filled: true,
      fillColor: _surface,
      labelStyle: const TextStyle(color: _onSurfaceVariant),
      enabledBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _outline),
        borderRadius: BorderRadius.circular(14),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _primary, width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
      errorBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _error),
        borderRadius: BorderRadius.circular(14),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _error, width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
    );
  }
}

class _PickerTile extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback onTap;

  const _PickerTile({
    required this.label,
    required this.value,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: _AddMaternalHealthMetricScreenState._surfaceContainer,
          border: Border.all(
            color: _AddMaternalHealthMetricScreenState._outline,
          ),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _AddMaternalHealthMetricScreenState._onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: _AddMaternalHealthMetricScreenState._onSurface,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
