import 'package:flutter/material.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../services/baby_log_service.dart';
import '../../healthRecords/services/growth_measurement_service.dart';
import '../../healthRecords/services/vaccination_service.dart';
import '../services/baby_care_composite_service.dart';

class BabyCareHubScreen extends StatefulWidget {
  const BabyCareHubScreen({super.key, this.loadProfiles = true});

  final bool loadProfiles;

  @override
  State<BabyCareHubScreen> createState() => _BabyCareHubScreenState();
}

class _BabyCareHubScreenState extends State<BabyCareHubScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  String _activeBaby = 'Baby A';
  List<BabyProfile> _profiles = const [];
  final _babyService = BabyService();
  final _babyLogService = BabyLogService();
  final _growthService = GrowthMeasurementService();
  final _vaccinationService = VaccinationService();
  final _compositeService = BabyCareCompositeService();
  String _selectedTab = 'overview';
  bool _compositeLoading = false;
  String? _compositeError;
  Map<String, dynamic>? _compositeData;
  int? _journalCount;
  int? _growthCount;
  int? _milestoneCount;
  int? _vaccinationCount;

  @override
  void initState() {
    super.initState();
    if (widget.loadProfiles) _loadProfiles();
  }

  Future<void> _loadProfiles() async {
    try {
      final profiles = await _babyService.listBabyProfiles();
      debugPrint('[MF03] profiles loaded: ${profiles.length}');
      if (!mounted || profiles.isEmpty) return;
      final active =
          profiles.where((profile) => profile.isActive).firstOrNull ??
          profiles.first;
      setState(() {
        _profiles = profiles;
        _activeBaby = active.nickname;
      });
      await _loadCareCounts(active.id);
      await _loadComposite(active.id, _selectedTab);
    } catch (error) {
      debugPrint('[MF03] loadProfiles failed: ${error.runtimeType}');
      // Keep deterministic local fixture for offline/unauthenticated smoke runs.
    }
  }

  Future<void> _loadCareCounts(String babyId) async {
    try {
      final results = await Future.wait([
        _babyLogService.getDailyLogs(babyId).then((value) => value.length).catchError((_) => -1),
        _growthService.getGrowthHistory(babyId).then((value) => value.length).catchError((_) => -1),
        _babyLogService.getMilestones(babyId).then((value) => value.length).catchError((_) => -1),
        _vaccinationService.listVaccinationRecords(babyId).then((value) => value.length).catchError((_) => -1),
      ]);
      debugPrint('[MF03] care counts loaded');
      if (!mounted) return;
      setState(() {
        _journalCount = results[0] >= 0 ? results[0] : null;
        _growthCount = results[1] >= 0 ? results[1] : null;
        _milestoneCount = results[2] >= 0 ? results[2] : null;
        _vaccinationCount = results[3] >= 0 ? results[3] : null;
      });
    } catch (error) {
      debugPrint('[MF03] loadCareCounts failed: ${error.runtimeType}');
      // Individual cards remain descriptive when a read model is unavailable.
    }
  }

  Future<void> _selectBaby(String? value) async {
    if (value == null) return;
    setState(() => _activeBaby = value);
    final selected = _profiles
        .where((profile) => profile.nickname == value)
        .firstOrNull;
    if (selected != null) {
      try {
        await _babyService.switchActiveBabyProfile(selected.id);
      } catch (_) {
        // Retry on the next refresh while keeping the current selection visible.
      }
      await _loadCareCounts(selected.id);
      await _loadComposite(selected.id, _selectedTab);
    }
  }

  Future<void> _loadComposite(String babyId, String tab) async {
    setState(() { _selectedTab = tab; _compositeLoading = true; _compositeError = null; });
    try {
      final data = tab == 'overview'
          ? await _compositeService.getOverview(babyId)
          : tab == 'timeline'
              ? await _compositeService.getTimeline(babyId)
              : await _compositeService.getPreparation(babyId);
      if (!mounted) return;
      setState(() { _compositeData = data; _compositeLoading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _compositeLoading = false; _compositeError = 'Unable to load this view.'; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        title: const Text(
          'Baby care hub',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          DecoratedBox(
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
                  value: _activeBaby,
                  items:
                      (_profiles.isEmpty
                              ? const ['Baby A', 'Baby B']
                              : _profiles
                                    .map((profile) => profile.nickname)
                                    .toList())
                          .map(
                            (baby) => DropdownMenuItem(
                              value: baby,
                              child: Text(baby),
                            ),
                          )
                          .toList(),
                  onChanged: _selectBaby,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Showing data for $_activeBaby',
            key: const Key('active-baby-name'),
            style: const TextStyle(
              color: _text,
              fontSize: 18,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 16),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'overview', label: Text('Overview')),
              ButtonSegment(value: 'timeline', label: Text('Timeline')),
              ButtonSegment(value: 'preparation', label: Text('Preparation')),
            ],
            selected: {_selectedTab},
            onSelectionChanged: (value) {
              final selected = value.first;
              final profile = _profiles.where((p) => p.nickname == _activeBaby).firstOrNull;
              if (profile != null) _loadComposite(profile.id, selected);
            },
          ),
          const SizedBox(height: 12),
          if (_compositeLoading)
            const Center(child: CircularProgressIndicator())
          else if (_compositeError != null)
            Text(_compositeError!, key: const Key('composite-error'))
          else if (_compositeData != null)
            Card(
              key: const Key('composite-read-model'),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  _selectedTab == 'timeline'
                      ? '${(_compositeData!['events'] as List?)?.length ?? 0} events'
                      : _selectedTab == 'preparation'
                          ? '${(_compositeData!['dueItems'] as List?)?.length ?? 0} due items'
                          : '${_compositeData!['journalCount'] ?? 0} journal entries',
                  key: const Key('composite-read-model-summary'),
                ),
              ),
            ),
          const SizedBox(height: 16),
          for (final card in const [
            (
              'Journal',
              'Recent observations and daily notes',
              'baby-care-journal',
            ),
            ('Growth', 'Recorded measurements and history', 'baby-care-growth'),
            (
              'Milestones',
              'Observed development milestones',
              'baby-care-milestones',
            ),
            (
              'Vaccinations',
              'Recorded doses and schedule',
              'baby-care-vaccinations',
            ),
          ]) ...[
            _HubCard(
              key: Key(card.$3),
              title: card.$1,
              description: card.$2,
              baby: _activeBaby,
              detail: card.$3 == 'baby-care-journal' && _journalCount != null
                  ? '${_journalCount!} entries'
                  : card.$3 == 'baby-care-growth' && _growthCount != null
                  ? '${_growthCount!} measurements'
                  : card.$3 == 'baby-care-milestones' && _milestoneCount != null
                  ? '${_milestoneCount!} milestones'
                  : card.$3 == 'baby-care-vaccinations' &&
                        _vaccinationCount != null
                  ? '${_vaccinationCount!} records'
                  : null,
            ),
            const SizedBox(height: 12),
          ],
        ],
      ),
    );
  }
}

class _HubCard extends StatelessWidget {
  const _HubCard({
    super.key,
    required this.title,
    required this.description,
    required this.baby,
    this.detail,
  });

  final String title;
  final String description;
  final String baby;
  final String? detail;

  @override
  Widget build(BuildContext context) => Card(
    color: Colors.white,
    elevation: 0,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
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
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
              child: Text(
                baby,
                style: const TextStyle(
                  color: Color(0xFFC98C7B),
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
          if (detail != null) ...[
            const SizedBox(height: 8),
            Text(
              detail!,
              style: const TextStyle(
                color: Color(0xFF5A463F),
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ],
      ),
    ),
  );
}
