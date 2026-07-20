import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../../journey/services/journey_service.dart';

class BabyJourneyLinkageScreen extends StatefulWidget {
  const BabyJourneyLinkageScreen({
    super.key,
    required this.journeyId,
    this.service,
    this.onCreate,
    this.onDoLater,
    this.eligibilityCheck,
  });

  final String journeyId;
  final BabyService? service;
  final Future<bool?> Function()? onCreate;
  final VoidCallback? onDoLater;
  final Future<bool> Function(String journeyId)? eligibilityCheck;

  @override
  State<BabyJourneyLinkageScreen> createState() =>
      BabyJourneyLinkageScreenState();
}

class BabyJourneyLinkageScreenState extends State<BabyJourneyLinkageScreen> {
  late final BabyService _service;
  List<BabyProfile> _babies = const [];
  String? _error;
  bool _loading = true;
  int _generation = 0;
  String? _accountId;
  bool _linking = false;
  bool? _eligible;
  final Map<String, String> _pendingSubmissionIds = {};

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? BabyService();
    _accountId = AuthState.instance.userId;
    AuthState.instance.addListener(_handleAuthChanged);
    _checkEligibilityAndRefresh();
  }

  void _handleAuthChanged() {
    if (AuthState.instance.userId == _accountId) return;
    _accountId = AuthState.instance.userId;
    _generation++;
    _pendingSubmissionIds.clear();
    if (mounted) {
      setState(() {
        _babies = const [];
        _loading = false;
        _linking = false;
        _error = null;
        _eligible = null;
      });
    }
    _checkEligibilityAndRefresh();
  }

  bool _isCurrentAccount(String? accountId) =>
      accountId == _accountId && accountId == AuthState.instance.userId;

  @override
  void dispose() {
    _generation++;
    AuthState.instance.removeListener(_handleAuthChanged);
    super.dispose();
  }

  Future<void> refresh() async {
    if (_eligible != true) return;
    final generation = ++_generation;
    final accountId = _accountId;
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final first = await _service.listJourneyBabies(widget.journeyId);
      final babies = <String, BabyProfile>{
        for (final baby in first.items) baby.id: baby,
      };
      for (var page = 1; page < first.totalPages; page++) {
        final next = await _service.listJourneyBabies(
          widget.journeyId,
          page: page,
          size: first.size,
        );
        if (!mounted ||
            generation != _generation ||
            !_isCurrentAccount(accountId)) {
          return;
        }
        for (final baby in next.items) {
          babies[baby.id] = baby;
        }
      }
      if (!mounted ||
          generation != _generation ||
          !_isCurrentAccount(accountId)) {
        return;
      }
      setState(() {
        _babies = babies.values.toList(growable: false);
        _loading = false;
      });
    } catch (_) {
      if (!mounted ||
          generation != _generation ||
          !_isCurrentAccount(accountId)) {
        return;
      }
      setState(() {
        _error = 'Không thể tải hồ sơ bé đã liên kết. Vui lòng thử lại.';
        _loading = false;
      });
    }
  }

  Future<void> _checkEligibilityAndRefresh() async {
    final generation = ++_generation;
    final accountId = _accountId;
    if (mounted) {
      setState(() {
        _eligible = null;
        _loading = true;
        _error = null;
      });
    }
    try {
      final eligible = widget.eligibilityCheck != null
          ? await widget.eligibilityCheck!(widget.journeyId)
          : await _serverEligibility(widget.journeyId);
      if (!mounted ||
          generation != _generation ||
          !_isCurrentAccount(accountId)) {
        return;
      }
      setState(() {
        _eligible = eligible;
        _loading = eligible;
      });
      if (eligible) await refresh();
    } catch (_) {
      if (!mounted ||
          generation != _generation ||
          !_isCurrentAccount(accountId)) {
        return;
      }
      setState(() {
        _eligible = false;
        _loading = false;
      });
    }
  }

  Future<bool> _serverEligibility(String journeyId) async {
    final dashboard = await JourneyService().getDashboard();
    return dashboard.babyActionsEligible && dashboard.journeyId == journeyId;
  }

  Future<void> _chooseExisting() async {
    if (_linking) return;
    final accountId = _accountId;
    try {
      final all = await _service.listBabyProfiles();
      if (!mounted || !_isCurrentAccount(accountId)) return;
      final available = all
          // listBabyProfiles already returns ACTIVE profiles. `isActive` is the
          // user's current selector state, not the profile lifecycle status.
          .where((baby) => baby.relatedJourneyId == null)
          .toList(growable: false);
      final selected = await showModalBottomSheet<BabyProfile>(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (context) => _LinkExistingBabySheet(babies: available),
      );
      if (selected == null || !mounted || !_isCurrentAccount(accountId)) return;
      final intentKey = '${selected.id}:${widget.journeyId}';
      final submissionId = _pendingSubmissionIds.putIfAbsent(
        intentKey,
        () => const Uuid().v4(),
      );
      setState(() => _linking = true);
      await _service.linkBabyToJourney(
        babyId: selected.id,
        relatedJourneyId: widget.journeyId,
        submissionId: submissionId,
      );
      if (!mounted || !_isCurrentAccount(accountId)) return;
      _pendingSubmissionIds.remove(intentKey);
      await refresh();
    } catch (_) {
      if (!mounted || !_isCurrentAccount(accountId)) return;
      setState(() => _error = 'Không thể liên kết hồ sơ bé. Vui lòng thử lại.');
    } finally {
      if (mounted && _isCurrentAccount(accountId)) {
        setState(() => _linking = false);
      }
    }
  }

  Future<void> _create() async {
    final created = await widget.onCreate?.call();
    if (created == true && mounted) await refresh();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        title: const Text('Hồ sơ bé'),
        backgroundColor: const Color(0xFFF6F1EC),
        foregroundColor: const Color(0xFF5A463F),
      ),
      body: RefreshIndicator(
        color: const Color(0xFFC98C7B),
        onRefresh: _checkEligibilityAndRefresh,
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            if (_eligible == true)
              BabyJourneyLinkageActions(
                journeyId: widget.journeyId,
                onCreate: widget.onCreate == null ? () async {} : _create,
                onLinkExisting: _chooseExisting,
                onDoLater:
                    widget.onDoLater ?? () => Navigator.maybePop(context),
              )
            else if (_eligible == false)
              const _IneligibleBabyLinkage(),
            const SizedBox(height: 24),
            if (_eligible != true && _loading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: CircularProgressIndicator(),
                ),
              ),
            if (_eligible != true)
              const SizedBox.shrink()
            else ...[
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Bé đã liên kết',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: Color(0xFF5A463F),
                      ),
                    ),
                  ),
                  IconButton(
                    key: const Key('linked-babies-refresh'),
                    tooltip: 'Tải lại hồ sơ bé đã liên kết',
                    onPressed: refresh,
                    constraints: const BoxConstraints(
                      minWidth: 48,
                      minHeight: 48,
                    ),
                    icon: const Icon(Icons.refresh_rounded),
                  ),
                ],
              ),
              if (_loading)
                const Center(
                  child: Padding(
                    padding: EdgeInsets.all(32),
                    child: CircularProgressIndicator(),
                  ),
                )
              else if (_error != null)
                Semantics(
                  liveRegion: true,
                  child: Text(
                    _error!,
                    style: const TextStyle(
                      fontSize: 16,
                      color: Color(0xFF8E3B32),
                    ),
                  ),
                )
              else if (_babies.isEmpty)
                const _EmptyLinkedBabies()
              else
                ..._babies.map((baby) => _BabyCard(baby: baby)),
            ],
          ],
        ),
      ),
    );
  }
}

class BabyJourneyLinkageActions extends StatelessWidget {
  const BabyJourneyLinkageActions({
    super.key,
    required this.journeyId,
    required this.onCreate,
    required this.onLinkExisting,
    required this.onDoLater,
  });

  final String journeyId;
  final VoidCallback onCreate;
  final VoidCallback onLinkExisting;
  final VoidCallback onDoLater;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 32,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Chọn cách tiếp tục',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: Color(0xFF5A463F),
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Bạn có thể tạo hồ sơ mới, liên kết hồ sơ hiện có hoặc thực hiện sau.',
            style: TextStyle(
              fontSize: 16,
              height: 1.4,
              color: Color(0xFF9C857C),
            ),
          ),
          const SizedBox(height: 16),
          _action(
            const Key('baby-link-create'),
            'Tạo hồ sơ bé',
            Icons.add_circle_outline,
            onCreate,
          ),
          const SizedBox(height: 12),
          _action(
            const Key('baby-link-existing'),
            'Liên kết hồ sơ hiện có',
            Icons.link_rounded,
            onLinkExisting,
          ),
          const SizedBox(height: 12),
          _action(
            const Key('baby-link-later'),
            'Để sau',
            Icons.schedule_rounded,
            onDoLater,
          ),
        ],
      ),
    );
  }

  Widget _action(
    Key key,
    String label,
    IconData icon,
    VoidCallback onPressed,
  ) => SizedBox(
    key: key,
    height: 52,
    child: OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon),
      label: Text(
        label,
        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
      style: OutlinedButton.styleFrom(
        foregroundColor: const Color(0xFF5A463F),
        side: const BorderSide(color: Color(0xFFC98C7B), width: 2),
        shape: const StadiumBorder(),
      ),
    ),
  );
}

class _LinkExistingBabySheet extends StatelessWidget {
  const _LinkExistingBabySheet({required this.babies});
  final List<BabyProfile> babies;

  @override
  Widget build(BuildContext context) => Semantics(
    namesRoute: true,
    label: 'Chọn hồ sơ bé để liên kết',
    child: Container(
      height: MediaQuery.sizeOf(context).height * 0.82,
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 32),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 48,
            height: 6,
            decoration: BoxDecoration(
              color: const Color(0xFFE8DDD6),
              borderRadius: BorderRadius.circular(99),
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Liên kết hồ sơ hiện có',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: Color(0xFF5A463F),
            ),
          ),
          const SizedBox(height: 12),
          if (babies.isEmpty)
            const Padding(
              padding: EdgeInsets.all(24),
              child: Text(
                'Không có hồ sơ đang hoạt động và chưa liên kết.',
                style: TextStyle(fontSize: 16),
              ),
            )
          else
            Expanded(
              child: ListView.builder(
                key: const Key('baby-link-candidates-list'),
                primary: false,
                itemCount: babies.length,
                itemBuilder: (context, index) {
                  final baby = babies[index];
                  return Material(
                    type: MaterialType.transparency,
                    child: ListTile(
                      minTileHeight: 56,
                      title: Text(
                        baby.nickname,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      trailing: const Icon(Icons.chevron_right_rounded),
                      onTap: () => Navigator.pop(context, baby),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    ),
  );
}

class _IneligibleBabyLinkage extends StatelessWidget {
  const _IneligibleBabyLinkage();

  @override
  Widget build(BuildContext context) => Semantics(
    liveRegion: true,
    child: Container(
      key: const Key('baby-link-ineligible'),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(24),
        border: const Border(
          left: BorderSide(color: Color(0xFFC98C7B), width: 4),
        ),
      ),
      child: const Text(
        'Liên kết hồ sơ bé hiện chưa khả dụng cho hành trình này. Vui lòng quay lại bảng điều khiển để làm mới trạng thái.',
        style: TextStyle(fontSize: 16, color: Color(0xFF5A463F)),
      ),
    ),
  );
}

class _BabyCard extends StatelessWidget {
  const _BabyCard({required this.baby});
  final BabyProfile baby;
  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(top: 12),
    elevation: 0,
    color: Colors.white,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
    child: ListTile(
      minTileHeight: 64,
      leading: const CircleAvatar(
        backgroundColor: Color(0xFFF2EAE4),
        child: Icon(Icons.child_care, color: Color(0xFFC98C7B)),
      ),
      title: Text(
        baby.nickname,
        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
      ),
    ),
  );
}

class _EmptyLinkedBabies extends StatelessWidget {
  const _EmptyLinkedBabies();
  @override
  Widget build(BuildContext context) => Semantics(
    label:
        'Chưa có hồ sơ bé liên kết. Các chức năng phục hồi vẫn sử dụng bình thường.',
    child: Card(
      key: const Key('linked-babies-empty'),
      elevation: 0,
      color: Color(0xFFF2EAE4),
      child: const Padding(
        padding: EdgeInsets.all(20),
        child: Text(
          'Chưa có hồ sơ bé liên kết. Bạn vẫn có thể tiếp tục nhật ký và chăm sóc phục hồi.',
          style: TextStyle(fontSize: 16, color: Color(0xFF5A463F)),
        ),
      ),
    ),
  );
}
