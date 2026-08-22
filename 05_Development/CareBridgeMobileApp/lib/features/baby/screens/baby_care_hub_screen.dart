import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/baby_care_composite_model.dart';
import '../models/baby_daily_log_model.dart';
import '../models/baby_model.dart';
import '../services/baby_care_composite_service.dart';
import '../services/baby_service.dart';

class BabyCareHubScreen extends StatefulWidget {
  const BabyCareHubScreen({
    super.key,
    this.loadProfiles = true,
    this.initialBabyId,
    this.babyService,
    this.compositeService,
  });

  final bool loadProfiles;
  final String? initialBabyId;
  final BabyService? babyService;
  final BabyCareCompositeService? compositeService;

  @override
  State<BabyCareHubScreen> createState() => _BabyCareHubScreenState();
}

class _BabyCareHubScreenState extends State<BabyCareHubScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _primary = Color(0xFFC98C7B);

  late final BabyService _babyService = widget.babyService ?? BabyService();
  late final BabyCareCompositeService _compositeService =
      widget.compositeService ?? BabyCareCompositeService();

  List<BabyProfile> _profiles = const [];
  String? _selectedBabyId;
  String _selectedTab = 'overview';
  bool _profilesLoading = true;
  String? _profilesError;
  bool _compositeLoading = false;
  String? _compositeError;
  BabyCareOverview? _overview;
  BabyCareTimeline? _timeline;
  AppointmentPreparationSummary? _preparation;
  int _requestGeneration = 0;
  Future<void> _activeBabySwitch = Future<void>.value();

  @override
  void initState() {
    super.initState();
    if (widget.loadProfiles) {
      unawaited(_loadProfiles());
    } else {
      _profilesLoading = false;
    }
  }

  Future<void> _loadProfiles() async {
    final generation = ++_requestGeneration;
    if (mounted) {
      setState(() {
        _profilesLoading = true;
        _profilesError = null;
        _compositeError = null;
        _clearComposite();
      });
    }
    try {
      final profiles = await _babyService.listBabyProfiles();
      if (!mounted || generation != _requestGeneration) return;

      final selected = _preferredProfile(profiles, widget.initialBabyId);
      setState(() {
        _profiles = profiles;
        _selectedBabyId = selected?.id;
      });
      if (selected != null) {
        await _loadComposite(selected.id, _selectedTab, generation: generation);
      }
    } catch (_) {
      if (!mounted || generation != _requestGeneration) return;
      setState(() {
        _profiles = const [];
        _selectedBabyId = null;
        _profilesError = 'Không thể tải danh sách bé.';
        _compositeLoading = false;
        _clearComposite();
      });
    } finally {
      if (mounted && generation == _requestGeneration) {
        setState(() => _profilesLoading = false);
      }
    }
  }

  BabyProfile? _preferredProfile(List<BabyProfile> profiles, String? babyId) {
    if (profiles.isEmpty) return null;
    if (babyId != null) {
      return profiles.where((profile) => profile.id == babyId).firstOrNull;
    }
    return profiles.firstWhere(
      (profile) => profile.isActive,
      orElse: () => profiles.first,
    );
  }

  Future<void> _selectBaby(String? babyId) async {
    if (babyId == null || babyId == _selectedBabyId) return;
    BabyProfile? profile;
    for (final candidate in _profiles) {
      if (candidate.id == babyId) {
        profile = candidate;
        break;
      }
    }
    if (profile == null) return;
    final selectedProfile = profile;

    final generation = ++_requestGeneration;
    setState(() {
      _selectedBabyId = babyId;
      _compositeError = null;
      _clearComposite();
    });
    final switchRequest = _activeBabySwitch.then<void>(
      (_) async =>
          _babyService.switchActiveBabyProfile(babyId).then<void>((_) {}),
    );
    _activeBabySwitch = switchRequest.then<void>((_) {}, onError: (_) {});
    try {
      await switchRequest;
    } catch (_) {
      // The selected id remains authoritative for the read requests below.
      if (mounted && generation == _requestGeneration) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Không thể cập nhật bé đang hoạt động; vẫn tải dữ liệu theo bé đã chọn.',
            ),
          ),
        );
      }
    }
    if (!mounted ||
        generation != _requestGeneration ||
        babyId != _selectedBabyId) {
      return;
    }
    await _loadComposite(
      selectedProfile.id,
      _selectedTab,
      generation: generation,
    );
  }

  Future<void> _loadComposite(
    String babyId,
    String tab, {
    int? generation,
  }) async {
    final requestGeneration = generation ?? ++_requestGeneration;
    if (!_requestIsCurrent(requestGeneration, babyId)) return;
    setState(() {
      _selectedTab = tab;
      _compositeLoading = true;
      _compositeError = null;
    });
    try {
      if (tab == 'timeline') {
        final timeline = await _compositeService.getTimeline(babyId);
        if (!_requestIsCurrent(requestGeneration, babyId)) return;
        if (timeline.babyId != babyId) {
          throw const FormatException('Baby care timeline scope mismatch');
        }
        setState(() => _timeline = timeline);
      } else if (tab == 'preparation') {
        final preparation = await _compositeService.getPreparation(babyId);
        if (!_requestIsCurrent(requestGeneration, babyId)) return;
        if (preparation.babyId != babyId) {
          throw const FormatException('Appointment preparation scope mismatch');
        }
        setState(() => _preparation = preparation);
      } else {
        final overview = await _compositeService.getOverview(babyId);
        if (!_requestIsCurrent(requestGeneration, babyId)) return;
        if (overview.babyId != babyId) {
          throw const FormatException('Baby care overview scope mismatch');
        }
        setState(() => _overview = overview);
      }
      if (_requestIsCurrent(requestGeneration, babyId)) {
        setState(() => _compositeLoading = false);
      }
    } catch (_) {
      if (!_requestIsCurrent(requestGeneration, babyId)) return;
      setState(() {
        _compositeLoading = false;
        _compositeError = 'Không thể tải dữ liệu cho bé này.';
      });
    }
  }

  bool _requestIsCurrent(int generation, String babyId) =>
      mounted && generation == _requestGeneration && babyId == _selectedBabyId;

  void _clearComposite() {
    _overview = null;
    _timeline = null;
    _preparation = null;
  }

  Future<void> _retry() async {
    final babyId = _selectedBabyId;
    if (babyId == null) {
      await _loadProfiles();
      return;
    }
    await _loadComposite(babyId, _selectedTab);
  }

  BabyProfile? get _selectedProfile {
    for (final profile in _profiles) {
      if (profile.id == _selectedBabyId) return profile;
    }
    return null;
  }

  String get _selectedNickname =>
      _selectedProfile?.nickname ?? _overview?.nickname ?? 'Chưa chọn bé';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        title: const Text(
          'Tổng quan chăm sóc bé',
          style: TextStyle(fontWeight: FontWeight.w800, fontFamily: 'Lexend'),
        ),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_profilesLoading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_profilesError != null) {
      return _ErrorState(message: _profilesError!, onRetry: _loadProfiles);
    }
    if (_profiles.isEmpty || _selectedBabyId == null) {
      return _EmptyState(
        message: _profiles.isNotEmpty && widget.initialBabyId != null
            ? 'Bé được chọn không tồn tại hoặc bạn không có quyền truy cập.'
            : 'Chưa có hồ sơ bé để hiển thị dữ liệu chăm sóc.',
        onRetry: widget.loadProfiles ? _loadProfiles : null,
      );
    }

    final babyId = _selectedBabyId!;
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        _buildBabySelector(),
        const SizedBox(height: 16),
        Text(
          'Hiển thị dữ liệu của $_selectedNickname',
          key: const Key('active-baby-name'),
          style: const TextStyle(
            color: _text,
            fontSize: 18,
            fontWeight: FontWeight.w700,
            fontFamily: 'Lexend',
          ),
        ),
        const SizedBox(height: 16),
        SegmentedButton<String>(
          segments: const [
            ButtonSegment(value: 'overview', label: Text('Tổng quan')),
            ButtonSegment(value: 'timeline', label: Text('Dòng thời gian')),
            ButtonSegment(value: 'preparation', label: Text('Chuẩn bị')),
          ],
          selected: {_selectedTab},
          onSelectionChanged: (value) {
            unawaited(_loadComposite(babyId, value.first));
          },
        ),
        const SizedBox(height: 12),
        _buildCompositeContent(),
        const SizedBox(height: 20),
        _HubCard(
          key: const Key('baby-care-journal'),
          title: 'Nhật ký chăm sóc',
          description: 'Ghi nhận và nhật ký theo dõi hàng ngày',
          babyName: _selectedNickname,
          onTap: () => context.push('/babies/$babyId/log-summary'),
        ),
        const SizedBox(height: 12),
        _HubCard(
          key: const Key('baby-care-growth'),
          title: 'Chỉ số phát triển',
          description: 'Lịch sử chiều cao, cân nặng và chỉ số bé',
          babyName: _selectedNickname,
          onTap: () => context.push('/babies/$babyId/growth'),
        ),
        const SizedBox(height: 12),
        _HubCard(
          key: const Key('baby-care-milestones'),
          title: 'Cột mốc phát triển',
          description: 'Các cột mốc phát triển đáng nhớ của bé',
          babyName: _selectedNickname,
          onTap: () => context.push('/babies/detail/$babyId'),
        ),
        const SizedBox(height: 12),
        _HubCard(
          key: const Key('baby-care-vaccinations'),
          title: 'Lịch tiêm chủng',
          description: 'Lịch tiêm ngừa và lịch sử mũi tiêm của bé',
          babyName: _selectedNickname,
          onTap: () => context.push('/babies/detail/$babyId'),
        ),
      ],
    );
  }

  Widget _buildBabySelector() {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 18,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<String>(
            key: const Key('active-baby-selector'),
            isExpanded: true,
            value: _selectedBabyId,
            items: _profiles
                .map(
                  (baby) => DropdownMenuItem<String>(
                    value: baby.id,
                    child: Text(baby.nickname, style: const TextStyle(fontFamily: 'Lexend')),
                  ),
                )
                .toList(growable: false),
            onChanged: _selectBaby,
          ),
        ),
      ),
    );
  }

  Widget _buildCompositeContent() {
    if (_compositeLoading) {
      return const Center(
        key: Key('composite-loading'),
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(color: _primary),
        ),
      );
    }
    if (_compositeError != null) {
      return _ErrorState(
        key: const Key('composite-error'),
        message: _compositeError!,
        onRetry: _retry,
      );
    }
    return switch (_selectedTab) {
      'timeline' => _buildTimeline(_timeline),
      'preparation' => _buildPreparation(_preparation),
      _ => _buildOverview(_overview),
    };
  }

  Widget _buildOverview(BabyCareOverview? overview) {
    if (overview == null) {
      return const _EmptyState(
        key: Key('composite-empty'),
        message: 'Chưa có dữ liệu tổng quan cho bé.',
      );
    }
    final metrics = <({String label, int value, IconData icon})>[
      (label: 'Nhật ký', value: overview.journalCount, icon: Icons.notes),
      (
        label: 'Phát triển',
        value: overview.growthMeasurementCount,
        icon: Icons.show_chart,
      ),
      (
        label: 'Cột mốc',
        value: overview.milestoneCount,
        icon: Icons.flag_outlined,
      ),
      (
        label: 'Tiêm chủng',
        value: overview.vaccinationRecordCount,
        icon: Icons.vaccines_outlined,
      ),
    ];
    return Card(
      key: const Key('composite-read-model'),
      color: Colors.white,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              overview.nickname,
              style: const TextStyle(
                color: _text,
                fontSize: 18,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 12),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              childAspectRatio: 2.7,
              crossAxisSpacing: 8,
              mainAxisSpacing: 8,
              children: [
                for (final metric in metrics)
                  _MetricTile(
                    label: metric.label,
                    value: metric.value,
                    icon: metric.icon,
                  ),
              ],
            ),
            if (overview.notice != null) ...[
              const SizedBox(height: 12),
              _Notice(text: overview.notice!),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildTimeline(BabyCareTimeline? timeline) {
    if (timeline == null || timeline.events.isEmpty) {
      return const _EmptyState(
        key: Key('composite-empty'),
        message: 'Chưa có hoạt động chăm sóc nào.',
      );
    }
    return Card(
      key: const Key('composite-read-model'),
      color: Colors.white,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      child: Column(
        children: [
          for (final event in timeline.events)
            ListTile(
              key: ValueKey('baby-care-timeline-${event.sourceId}'),
              leading: CircleAvatar(
                backgroundColor: _primary.withAlpha(35),
                child: Icon(_timelineIcon(event.sourceType), color: _text),
              ),
              title: Text(_timelineLabel(event)),
              subtitle: Text(
                '${event.sourceType} · ${_formatDateTime(event.occurredAt)}',
              ),
              trailing: _timelineRoute(event) == null
                  ? null
                  : const Icon(Icons.chevron_right),
              onTap: _timelineRoute(event) == null
                  ? null
                  : () => _openTimelineEvent(event),
            ),
        ],
      ),
    );
  }

  Widget _buildPreparation(AppointmentPreparationSummary? preparation) {
    if (preparation == null) {
      return const _EmptyState(
        key: Key('composite-empty'),
        message: 'Chưa có thông tin chuẩn bị cho lịch hẹn.',
      );
    }
    return Card(
      key: const Key('composite-read-model'),
      color: Colors.white,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (preparation.facts.isEmpty && preparation.dueItems.isEmpty)
              const _BulletRow(
                text: 'Chưa có mục cần chuẩn bị.',
                icon: Icons.info_outline,
              ),
            if (preparation.facts.isNotEmpty) ...[
              const Text(
                'Thông tin đã ghi nhận',
                style: TextStyle(color: _text, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 8),
              for (final fact in preparation.facts)
                _BulletRow(text: fact, icon: Icons.info_outline),
            ],
            if (preparation.dueItems.isNotEmpty) ...[
              const SizedBox(height: 14),
              const Text(
                'Nội dung cần xem lại',
                style: TextStyle(color: _text, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 8),
              for (final item in preparation.dueItems)
                _BulletRow(text: item, icon: Icons.event_available_outlined),
            ],
            if (preparation.notice != null) ...[
              const SizedBox(height: 12),
              _Notice(text: preparation.notice!),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _openTimelineEvent(BabyCareTimelineEvent event) async {
    final babyId = _selectedBabyId;
    if (babyId == null) return;
    final route = _timelineRoute(event);
    if (route != null && mounted) await context.push(route);
  }

  String? _timelineRoute(BabyCareTimelineEvent event) {
    final babyId = _selectedBabyId;
    if (babyId == null) return null;
    return switch (event.sourceType.toUpperCase()) {
      'JOURNAL' => '/babies/$babyId/daily-logs/${event.sourceId}',
      'MILESTONE' => '/babies/$babyId/milestones/${event.sourceId}',
      'VACCINATION' => '/babies/$babyId/vaccinations/${event.sourceId}',
      'GROWTH' => '/babies/$babyId/growth',
      _ => null,
    };
  }

  static IconData _timelineIcon(String sourceType) =>
      switch (sourceType.toUpperCase()) {
        'JOURNAL' => Icons.notes,
        'GROWTH' => Icons.show_chart,
        'MILESTONE' => Icons.flag_outlined,
        'VACCINATION' => Icons.vaccines_outlined,
        _ => Icons.event_note_outlined,
      };

  static String _timelineLabel(BabyCareTimelineEvent event) {
    if (event.sourceType.toUpperCase() == 'JOURNAL') {
      final raw = event.displayLabel.trim();
      const knownTypes = {
        'FEEDING',
        'SLEEP',
        'DIAPER',
        'FEVER',
        'VOMITING',
        'MEDICINE',
        'SYMPTOM',
      };
      return knownTypes.contains(raw.toUpperCase())
          ? displayLogTypeLabel(raw)
          : (raw.isEmpty ? 'Nhật ký' : raw);
    }
    return event.displayLabel;
  }

  static String _formatDateTime(DateTime value) =>
      '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/${value.year} '
      '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}';
}

class _HubCard extends StatelessWidget {
  const _HubCard({
    super.key,
    required this.title,
    required this.description,
    required this.babyName,
    required this.onTap,
  });

  final String title;
  final String description;
  final String babyName;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
    color: Colors.white,
    elevation: 0,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
    child: InkWell(
      borderRadius: BorderRadius.circular(24),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                color: Color(0xFF5A463F),
                fontSize: 22,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              description,
              style: const TextStyle(color: Color(0xFF9C857C), fontSize: 15),
            ),
            const SizedBox(height: 14),
            DecoratedBox(
              decoration: BoxDecoration(
                color: const Color(0xFFF2EAE4),
                borderRadius: BorderRadius.circular(30),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 7,
                ),
                child: Text(
                  babyName,
                  style: const TextStyle(
                    color: Color(0xFFC98C7B),
                    fontWeight: FontWeight.w700,
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

class _MetricTile extends StatelessWidget {
  const _MetricTile({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final int value;
  final IconData icon;

  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: const Color(0xFFF6F1EC),
      borderRadius: BorderRadius.circular(14),
    ),
    child: Padding(
      padding: const EdgeInsets.all(10),
      child: Row(
        children: [
          Icon(icon, size: 20, color: const Color(0xFF845143)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              label,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Color(0xFF5A463F)),
            ),
          ),
          Text(
            '$value',
            style: const TextStyle(
              color: Color(0xFF5A463F),
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    ),
  );
}

class _BulletRow extends StatelessWidget {
  const _BulletRow({required this.text, required this.icon});

  final String text;
  final IconData icon;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: const Color(0xFFC98C7B)),
        const SizedBox(width: 8),
        Expanded(child: Text(text)),
      ],
    ),
  );
}

class _Notice extends StatelessWidget {
  const _Notice({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: const Color(0xFFF2EAE4),
      borderRadius: BorderRadius.circular(14),
    ),
    child: Text(text, style: const TextStyle(color: Color(0xFF5A463F))),
  );
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) => Card(
    color: Colors.white,
    elevation: 0,
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Text(message, textAlign: TextAlign.center),
          if (onRetry != null) ...[
            const SizedBox(height: 8),
            TextButton(onPressed: onRetry, child: const Text('Thử lại')),
          ],
        ],
      ),
    ),
  );
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({super.key, required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: Color(0xFFBA1A1A), size: 44),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 10),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    ),
  );
}
