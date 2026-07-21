import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../models/postpartum_log_model.dart';
import '../services/postpartum_log_draft_store.dart';
import '../services/postpartum_log_service.dart';

class PostpartumLogFormScreen extends StatefulWidget {
  const PostpartumLogFormScreen({
    super.key,
    required this.journeyId,
    this.logId,
    this.initialLog,
    this.service,
    this.draftStore,
  });

  final String journeyId;
  final String? logId;
  final PostpartumLog? initialLog;
  final PostpartumLogService? service;
  final PostpartumLogDraftStore? draftStore;

  @override
  State<PostpartumLogFormScreen> createState() =>
      _PostpartumLogFormScreenState();
}

class _PostpartumLogFormScreenState extends State<PostpartumLogFormScreen> {
  static const _primary = Color(0xFF845143);
  late final PostpartumLogService _service;
  late final PostpartumLogDraftStore _draftStore;
  String? _screenOwnerId;
  String? _draftOwnerId;
  late String _submissionId;
  late DateTime _logDate;
  late final TextEditingController _pain;
  late final TextEditingController _mood;
  late final TextEditingController _sleep;
  late final TextEditingController _symptom;
  late final TextEditingController _breastfeeding;
  final ScrollController _scrollController = ScrollController();
  String? _bleeding;
  String? _error;
  bool _saving = false;
  bool _attempted = false;
  int _intentGeneration = 0;
  int _saveOperationId = 0;
  PostpartumLog? _redFlagResult;

  bool get _editing => widget.logId != null;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? PostpartumLogService();
    _draftStore = widget.draftStore ?? const PostpartumLogDraftStore();
    _screenOwnerId = AuthState.instance.userId;
    final initial = widget.initialLog;
    _submissionId = initial?.submissionId ?? const Uuid().v4();
    _logDate = initial?.logDate ?? DateTime.now();
    _pain = TextEditingController(text: initial?.painLevel?.toString() ?? '');
    _mood = TextEditingController(text: initial?.moodLevel?.toString() ?? '');
    _sleep = TextEditingController(text: initial?.sleepHours?.toString() ?? '');
    _symptom = TextEditingController(text: initial?.symptomNote ?? '');
    _breastfeeding = TextEditingController(
      text: initial?.breastfeedingNote ?? '',
    );
    _bleeding = initial?.bleedingLevel;
    AuthState.instance.addListener(_handleAccountChanged);
    if (!_editing) {
      _draftOwnerId = _screenOwnerId;
      unawaited(_restoreDraft());
    }
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAccountChanged);
    _pain.dispose();
    _mood.dispose();
    _sleep.dispose();
    _symptom.dispose();
    _breastfeeding.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _intentChanged() {
    _intentGeneration++;
    if (_attempted && !_editing) {
      _submissionId = const Uuid().v4();
      _attempted = false;
    }
    if (_error != null) setState(() => _error = null);
    unawaited(_persistDraft());
  }

  Future<void> _restoreDraft() async {
    final ownerId = _draftOwnerId;
    if (ownerId == null) return;
    final restoreGeneration = _intentGeneration;
    final value = await _draftStore.read(ownerId, widget.journeyId);
    if (!mounted ||
        value == null ||
        AuthState.instance.userId != ownerId ||
        _intentGeneration != restoreGeneration) {
      return;
    }
    final date = DateTime.tryParse(value['logDate'] as String? ?? '');
    final submissionId = value['submissionId'] as String?;
    if (date == null || submissionId == null || submissionId.isEmpty) return;
    setState(() {
      _submissionId = submissionId;
      _logDate = date;
      _pain.text = value['pain'] as String? ?? '';
      _mood.text = value['mood'] as String? ?? '';
      _sleep.text = value['sleep'] as String? ?? '';
      _symptom.text = value['symptom'] as String? ?? '';
      _breastfeeding.text = value['breastfeeding'] as String? ?? '';
      _bleeding = value['bleeding'] as String?;
      _attempted = value['attempted'] as bool? ?? false;
    });
  }

  Future<void> _persistDraft() async {
    final ownerId = _draftOwnerId;
    if (_editing || ownerId == null || AuthState.instance.userId != ownerId) {
      return;
    }
    await _draftStore.write(ownerId, widget.journeyId, {
      'submissionId': _submissionId,
      'logDate': _logDate.toIso8601String(),
      'pain': _pain.text,
      'mood': _mood.text,
      'sleep': _sleep.text,
      'symptom': _symptom.text,
      'breastfeeding': _breastfeeding.text,
      'bleeding': _bleeding,
      'attempted': _attempted,
    });
  }

  Future<void> _clearDraft(String? ownerId) async {
    if (!_editing && ownerId != null) {
      await _draftStore.delete(ownerId, widget.journeyId);
    }
  }

  void _handleAccountChanged() {
    final previousScreenOwner = _screenOwnerId;
    final currentOwner = AuthState.instance.userId;
    if (previousScreenOwner == currentOwner) return;
    _intentGeneration++;
    _saveOperationId++;
    if (!_editing && previousScreenOwner != null) {
      unawaited(_draftStore.delete(previousScreenOwner, widget.journeyId));
    }
    _screenOwnerId = currentOwner;
    _draftOwnerId = currentOwner;
    if (!mounted) return;
    setState(() {
      _submissionId = const Uuid().v4();
      _logDate = DateTime.now();
      _pain.clear();
      _mood.clear();
      _sleep.clear();
      _symptom.clear();
      _breastfeeding.clear();
      _bleeding = null;
      _attempted = false;
      _saving = false;
      _redFlagResult = null;
      _error = null;
    });
  }

  bool _requestIsCurrent({
    required String? ownerId,
    required int intentGeneration,
    required int operationId,
  }) =>
      mounted &&
      _screenOwnerId == ownerId &&
      AuthState.instance.userId == ownerId &&
      _intentGeneration == intentGeneration &&
      _saveOperationId == operationId;

  int? _bounded(String value, String label) {
    if (value.trim().isEmpty) return null;
    final parsed = int.tryParse(value);
    if (parsed == null || parsed < 0 || parsed > 10) {
      throw FormatException('$label phải từ 0 đến 10.');
    }
    return parsed;
  }

  Future<void> _save() async {
    if (_saving) return;
    final sleepText = _sleep.text.trim();
    PostpartumLogDraft draft;
    try {
      final sleep = sleepText.isEmpty ? null : double.tryParse(sleepText);
      if (sleepText.isNotEmpty && (sleep == null || !sleep.isFinite)) {
        throw const FormatException('Giờ ngủ phải là một số hợp lệ.');
      }
      if (sleep != null && (sleep < 0 || sleep > 24)) {
        throw const FormatException('Giờ ngủ phải từ 0 đến 24.');
      }
      draft = PostpartumLogDraft(
        submissionId: _submissionId,
        logDate: _logDate,
        painLevel: _bounded(_pain.text, 'Mức đau'),
        bleedingLevel: _bleeding,
        moodLevel: _bounded(_mood.text, 'Tâm trạng'),
        sleepHours: sleep,
        symptomNote: _symptom.text,
        breastfeedingNote: _breastfeeding.text,
      );
    } on FormatException catch (error) {
      setState(() => _error = error.message);
      if (_scrollController.hasClients) {
        _scrollController.jumpTo(0);
      }
      return;
    }
    final requestOwnerId = _screenOwnerId;
    final requestGeneration = ++_intentGeneration;
    final operationId = ++_saveOperationId;
    setState(() {
      _saving = true;
      _attempted = true;
      _error = null;
    });
    await _persistDraft();
    if (!_requestIsCurrent(
      ownerId: requestOwnerId,
      intentGeneration: requestGeneration,
      operationId: operationId,
    )) {
      if (mounted && _saveOperationId == operationId) {
        setState(() => _saving = false);
      }
      return;
    }
    try {
      final result = _editing
          ? await _service.update(widget.logId!, draft)
          : await _service.create(widget.journeyId, draft);
      if (!_requestIsCurrent(
        ownerId: requestOwnerId,
        intentGeneration: requestGeneration,
        operationId: operationId,
      )) {
        return;
      }
      await _clearDraft(requestOwnerId);
      if (!_requestIsCurrent(
        ownerId: requestOwnerId,
        intentGeneration: requestGeneration,
        operationId: operationId,
      )) {
        return;
      }
      if (!mounted) return;
      if (result.redFlagAlert) {
        setState(() => _redFlagResult = result);
      } else {
        context.pop(true);
      }
    } catch (_) {
      if (_requestIsCurrent(
        ownerId: requestOwnerId,
        intentGeneration: requestGeneration,
        operationId: operationId,
      )) {
        setState(() {
          _error = 'Chưa thể lưu. Nội dung vẫn được giữ để bạn thử lại.';
        });
      }
    } finally {
      if (mounted && _saveOperationId == operationId) {
        setState(() => _saving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_redFlagResult != null) return _buildUrgentResult();
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFFF8F6),
        foregroundColor: _primary,
        title: Text(_editing ? 'Sửa nhật ký' : 'Thêm nhật ký'),
      ),
      body: ListView(
        controller: _scrollController,
        padding: const EdgeInsets.all(20),
        children: [
          if (_error != null)
            Semantics(
              liveRegion: true,
              child: Container(
                key: const Key('postpartum-log-error'),
                padding: const EdgeInsets.all(14),
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDAD6),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Text(_error!),
              ),
            ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Ngày ghi nhận'),
            subtitle: Text(_date(_logDate)),
            trailing: const Icon(Icons.calendar_today_outlined),
            onTap: _pickDate,
          ),
          _numberField(_pain, 'Mức đau (0–10)'),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            key: ValueKey(_bleeding),
            initialValue: _bleeding,
            decoration: const InputDecoration(labelText: 'Mức chảy máu'),
            items: const [
              DropdownMenuItem(value: 'NONE', child: Text('Không có')),
              DropdownMenuItem(value: 'LIGHT', child: Text('Nhẹ')),
              DropdownMenuItem(value: 'MODERATE', child: Text('Vừa')),
              DropdownMenuItem(value: 'HEAVY', child: Text('Nhiều')),
            ],
            onChanged: (value) {
              setState(() => _bleeding = value);
              _intentChanged();
            },
          ),
          const SizedBox(height: 12),
          _numberField(_mood, 'Tâm trạng (0–10)'),
          const SizedBox(height: 12),
          TextField(
            controller: _sleep,
            onChanged: (_) => _intentChanged(),
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'Giờ ngủ (0–24)'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _symptom,
            onChanged: (_) => _intentChanged(),
            maxLength: 2000,
            maxLines: 4,
            decoration: const InputDecoration(
              labelText: 'Ghi chú triệu chứng (không bắt buộc)',
            ),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _breastfeeding,
            onChanged: (_) => _intentChanged(),
            maxLength: 1000,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Ghi chú cho con bú (không bắt buộc)',
            ),
          ),
          const SizedBox(height: 20),
          FilledButton(
            key: const Key('postpartum-log-save'),
            onPressed: _saving ? null : _save,
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              backgroundColor: const Color(0xFFC98C7B),
            ),
            child: Text(_saving ? 'Đang lưu…' : 'Lưu nhật ký'),
          ),
        ],
      ),
    );
  }

  Widget _numberField(TextEditingController controller, String label) =>
      TextField(
        controller: controller,
        onChanged: (_) => _intentChanged(),
        keyboardType: TextInputType.number,
        decoration: InputDecoration(labelText: label),
      );

  Future<void> _pickDate() async {
    final today = DateTime.now();
    final value = await showDatePicker(
      context: context,
      initialDate: _logDate.isAfter(today) ? today : _logDate,
      firstDate: DateTime(1900),
      lastDate: today,
    );
    if (value != null && mounted) {
      setState(() => _logDate = value);
      _intentChanged();
    }
  }

  Widget _buildUrgentResult() => Scaffold(
    backgroundColor: const Color(0xFFFFF8F6),
    body: SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.warning_amber_rounded,
              size: 64,
              color: Color(0xFF93000A),
            ),
            const SizedBox(height: 20),
            const Text(
              'Nhật ký đã được lưu',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            const Text(
              'Một số thông tin cần được đánh giá sớm. Nếu bạn thấy không an toàn, hãy liên hệ cấp cứu hoặc cơ sở y tế gần nhất.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: () => context.push('/postpartum-safety-help'),
              icon: const Icon(Icons.health_and_safety_outlined),
              label: const Text('Xem hướng dẫn hỗ trợ'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(56),
              ),
            ),
            TextButton(
              onPressed: () => context.pop(true),
              child: const Text('Hoàn tất'),
            ),
          ],
        ),
      ),
    ),
  );

  String _date(DateTime value) =>
      '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/${value.year}';
}
