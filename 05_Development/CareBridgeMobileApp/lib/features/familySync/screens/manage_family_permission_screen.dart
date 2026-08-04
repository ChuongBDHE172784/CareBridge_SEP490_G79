import 'package:flutter/material.dart';

import '../models/care_group_model.dart';
import '../models/family_permission_model.dart';
import '../services/care_group_service.dart';

class ManageFamilyPermissionScreen extends StatefulWidget {
  const ManageFamilyPermissionScreen({
    super.key,
    required this.groupId,
    required this.member,
    this.service,
  });

  final String groupId;
  final CareGroupMember member;
  final CareGroupService? service;

  @override
  State<ManageFamilyPermissionScreen> createState() =>
      _ManageFamilyPermissionScreenState();
}

class _ManageFamilyPermissionScreenState
    extends State<ManageFamilyPermissionScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFBF8);
  static const _primary = Color(0xFF9B5E4E);
  static const _accent = Color(0xFFC98C7B);
  static const _text = Color(0xFF2E211D);
  static const _muted = Color(0xFF74645F);

  late final CareGroupService _service = widget.service ?? CareGroupService();
  bool _isLoading = true;
  bool _isSaving = false;
  bool _loadFailed = false;
  List<bool>? _savedValues;

  bool _calendar = false;
  bool _logs = false;
  bool _alerts = false;
  bool _records = false;
  bool _quickNotes = false;
  bool _quickNoteWeight = false;
  bool _quickNoteFetalMovement = false;
  bool _quickNoteBloodPressure = false;
  bool _quickNoteHydration = false;
  bool _quickNoteEpds = false;
  bool _quickNoteBloodGlucose = false;

  List<bool> get _values => [
    _calendar,
    _logs,
    _alerts,
    _records,
    _quickNotes,
    _quickNoteWeight,
    _quickNoteFetalMovement,
    _quickNoteBloodPressure,
    _quickNoteHydration,
    _quickNoteEpds,
    _quickNoteBloodGlucose,
  ];

  bool get _isDirty {
    final saved = _savedValues;
    if (saved == null) return false;
    final current = _values;
    for (var i = 0; i < saved.length; i++) {
      if (saved[i] != current[i]) return true;
    }
    return false;
  }

  int get _sharedMetricCount => [
    _quickNoteWeight,
    _quickNoteFetalMovement,
    _quickNoteBloodPressure,
    _quickNoteHydration,
    _quickNoteEpds,
    _quickNoteBloodGlucose,
  ].where((value) => value).length;

  @override
  void initState() {
    super.initState();
    _loadPermission();
  }

  Future<void> _loadPermission() async {
    setState(() {
      _isLoading = true;
      _loadFailed = false;
    });
    try {
      final permission = await _service.getFamilyPermission(
        widget.groupId,
        widget.member.memberId,
      );
      if (!mounted) return;
      setState(() {
        _applyPermission(permission);
        _savedValues = List<bool>.of(_values);
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _loadFailed = true;
      });
    }
  }

  void _applyPermission(FamilyPermission permission) {
    _calendar = permission.calendar;
    _logs = permission.logs;
    // The existing row controls both legacy alerts and checklist visibility;
    // keep it enabled when either persisted grant is present.
    _alerts = permission.alerts || permission.checklistView;
    _records = permission.records;
    _quickNotes = permission.quickNotes;
    _quickNoteWeight = permission.quickNoteWeight;
    _quickNoteFetalMovement = permission.quickNoteFetalMovement;
    _quickNoteBloodPressure = permission.quickNoteBloodPressure;
    _quickNoteHydration = permission.quickNoteHydration;
    _quickNoteEpds = permission.quickNoteEpds;
    _quickNoteBloodGlucose = permission.quickNoteBloodGlucose;
  }

  Future<void> _saveChanges() async {
    if (!_isDirty || _isSaving) return;
    setState(() => _isSaving = true);
    try {
      final savedPermission = await _service.updateFamilyPermission(
        widget.groupId,
        widget.member.memberId,
        calendar: _calendar,
        logs: _logs,
        alerts: _alerts,
        records: _records,
        // The existing "Việc cần làm & cảnh báo" switch is the family-facing
        // read grant for checklist content. Family members remain read-only.
        checklistView: _alerts,
        checklistComplete: false,
        quickNotes: _quickNotes,
        quickNoteWeight: _quickNoteWeight,
        quickNoteFetalMovement: _quickNoteFetalMovement,
        quickNoteBloodPressure: _quickNoteBloodPressure,
        quickNoteHydration: _quickNoteHydration,
        quickNoteEpds: _quickNoteEpds,
        quickNoteBloodGlucose: _quickNoteBloodGlucose,
      );
      if (!mounted) return;
      setState(() {
        _applyPermission(savedPermission);
        _savedValues = List<bool>.of(_values);
        _isSaving = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã cập nhật quyền chia sẻ.')),
      );
    } catch (_) {
      if (!mounted) return;
      setState(() => _isSaving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Chưa thể lưu thay đổi. Vui lòng thử lại.'),
        ),
      );
    }
  }

  Future<void> _refreshPermission() async {
    if (_isDirty || _isSaving) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Hãy lưu hoặc hoàn tác thay đổi trước khi làm mới.'),
        ),
      );
      return;
    }
    await _loadPermission();
  }

  Future<void> _removeMember() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text(
          'Xóa khỏi nhóm?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
        content: Text(
          '${widget.member.displayName} sẽ không còn xem được dữ liệu của bạn.',
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFBA1A1A),
            ),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Xóa thành viên'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await _service.removeMember(widget.groupId, widget.member.memberId);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Chưa thể xóa thành viên. Vui lòng thử lại.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        surfaceTintColor: Colors.transparent,
        title: const Text(
          'Quản lý quyền chia sẻ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _text,
          ),
        ),
      ),
      body: _isLoading
          ? const _PermissionLoading()
          : _loadFailed
          ? _buildLoadError()
          : RefreshIndicator(
              color: _primary,
              onRefresh: _refreshPermission,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                children: [
                  _buildMemberSummary(),
                  const SizedBox(height: 16),
                  _buildGeneralPermissions(),
                  const SizedBox(height: 16),
                  _buildHealthMetrics(),
                  const SizedBox(height: 16),
                  _buildPrivacyNote(),
                  const SizedBox(height: 16),
                  TextButton.icon(
                    onPressed: _removeMember,
                    style: TextButton.styleFrom(
                      foregroundColor: const Color(0xFFBA1A1A),
                      alignment: Alignment.centerLeft,
                    ),
                    icon: const Icon(Icons.person_remove_outlined),
                    label: const Text(
                      'Xóa thành viên khỏi nhóm',
                      style: TextStyle(fontFamily: 'Lexend'),
                    ),
                  ),
                ],
              ),
            ),
      bottomNavigationBar: _isLoading || _loadFailed
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: FilledButton(
                key: const Key('permission-save-button'),
                onPressed: _isDirty && !_isSaving ? _saveChanges : null,
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(54),
                  backgroundColor: _primary,
                  disabledBackgroundColor: const Color(0xFFE0D8D3),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(18),
                  ),
                ),
                child: _isSaving
                    ? const SizedBox.square(
                        dimension: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.4,
                          color: Colors.white,
                        ),
                      )
                    : Text(
                        _isDirty ? 'Lưu thay đổi' : 'Đã lưu',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ),
    );
  }

  Widget _buildLoadError() {
    return Center(
      key: const Key('permission-load-error'),
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 48, color: _primary),
            const SizedBox(height: 16),
            const Text(
              'Chưa tải được quyền chia sẻ',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _text,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Kiểm tra kết nối và thử lại. Quyền hiện tại chưa bị thay đổi.',
              textAlign: TextAlign.center,
              style: TextStyle(fontFamily: 'Lexend', color: _muted),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('permission-retry-button'),
              onPressed: _loadPermission,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMemberSummary() {
    final initial = widget.member.displayName.trim().isEmpty
        ? '?'
        : widget.member.displayName.trim()[0].toUpperCase();
    return _SectionCard(
      child: Row(
        children: [
          CircleAvatar(
            radius: 26,
            backgroundColor: const Color(0xFFFFE5DE),
            foregroundColor: _primary,
            child: Text(
              initial,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w700,
                fontSize: 20,
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.member.displayName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: _text,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  widget.member.familyRelationshipLabel ??
                      widget.member.roleLabel,
                  style: const TextStyle(fontFamily: 'Lexend', color: _muted),
                ),
              ],
            ),
          ),
          const _StatusPill(label: 'Chỉ xem'),
        ],
      ),
    );
  }

  Widget _buildGeneralPermissions() {
    return _SectionCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _SectionTitle(
            icon: Icons.shield_outlined,
            title: 'Phạm vi hỗ trợ',
            subtitle: 'Chọn những khu vực thành viên có thể xem.',
          ),
          const SizedBox(height: 10),
          _permissionRow(
            title: 'Lịch chăm sóc',
            icon: Icons.calendar_month_outlined,
            value: _calendar,
            onChanged: (value) => setState(() => _calendar = value),
          ),
          _permissionRow(
            title: 'Nhật ký chăm sóc',
            icon: Icons.menu_book_outlined,
            value: _logs,
            onChanged: (value) => setState(() => _logs = value),
          ),
          _permissionRow(
            title: 'Việc cần làm & cảnh báo',
            icon: Icons.notifications_active_outlined,
            value: _alerts,
            onChanged: (value) => setState(() => _alerts = value),
          ),
          _permissionRow(
            title: 'Hồ sơ được chia sẻ',
            icon: Icons.folder_shared_outlined,
            value: _records,
            onChanged: (value) => setState(() => _records = value),
            divider: false,
          ),
        ],
      ),
    );
  }

  Widget _buildHealthMetrics() {
    return KeyedSubtree(
      key: const Key('health-metrics-section'),
      child: _SectionCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _SectionTitle(
              icon: Icons.monitor_heart_outlined,
              title: 'Chỉ số sức khỏe',
              subtitle: _quickNotes
                  ? '$_sharedMetricCount/6 chỉ số đang chia sẻ'
                  : 'Thành viên không xem được chỉ số nào',
              trailing: Switch.adaptive(
                key: const Key('health-metrics-parent-switch'),
                value: _quickNotes,
                activeTrackColor: _accent,
                onChanged: _isSaving ? null : _setQuickNotes,
              ),
            ),
            if (_quickNotes) ...[
              const SizedBox(height: 8),
              Row(
                children: [
                  TextButton(
                    key: const Key('health-metrics-select-all'),
                    onPressed: _isSaving || _sharedMetricCount == 6
                        ? null
                        : () => _setAllMetrics(true),
                    child: const Text('Chọn tất cả'),
                  ),
                  TextButton(
                    key: const Key('health-metrics-clear-all'),
                    onPressed: _isSaving || _sharedMetricCount == 0
                        ? null
                        : () => _setAllMetrics(false),
                    child: const Text('Bỏ chọn'),
                  ),
                ],
              ),
              _metricRow(
                title: 'Chỉ số BMI',
                icon: Icons.calculate_outlined,
                value: _quickNoteWeight,
                onChanged: (value) => setState(() => _quickNoteWeight = value),
              ),
              _metricRow(
                title: 'Cử động thai',
                icon: Icons.child_friendly_outlined,
                value: _quickNoteFetalMovement,
                onChanged: (value) =>
                    setState(() => _quickNoteFetalMovement = value),
              ),
              _metricRow(
                title: 'Huyết áp',
                icon: Icons.favorite_border,
                value: _quickNoteBloodPressure,
                onChanged: (value) =>
                    setState(() => _quickNoteBloodPressure = value),
              ),
              _metricRow(
                title: 'Nước',
                icon: Icons.water_drop_outlined,
                value: _quickNoteHydration,
                onChanged: (value) =>
                    setState(() => _quickNoteHydration = value),
              ),
              _metricRow(
                title: 'Sàng lọc EPDS',
                icon: Icons.psychology_alt_outlined,
                value: _quickNoteEpds,
                onChanged: (value) => setState(() => _quickNoteEpds = value),
              ),
              _metricRow(
                title: 'Đường huyết',
                icon: Icons.bloodtype_outlined,
                value: _quickNoteBloodGlucose,
                onChanged: (value) =>
                    setState(() => _quickNoteBloodGlucose = value),
                divider: false,
              ),
            ],
          ],
        ),
      ),
    );
  }

  void _setQuickNotes(bool value) {
    setState(() {
      _quickNotes = value;
      if (!value) _setAllMetricsValue(false);
    });
  }

  void _setAllMetrics(bool value) {
    setState(() {
      _quickNotes = value || _quickNotes;
      _setAllMetricsValue(value);
    });
  }

  void _setAllMetricsValue(bool value) {
    _quickNoteWeight = value;
    _quickNoteFetalMovement = value;
    _quickNoteBloodPressure = value;
    _quickNoteHydration = value;
    _quickNoteEpds = value;
    _quickNoteBloodGlucose = value;
  }

  Widget _permissionRow({
    required String title,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
    bool divider = true,
  }) => _toggleRow(
    title: title,
    subtitle: value ? 'Đang chia sẻ' : 'Không chia sẻ',
    icon: icon,
    value: value,
    onChanged: onChanged,
    divider: divider,
  );

  Widget _metricRow({
    required String title,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
    bool divider = true,
  }) => _toggleRow(
    title: title,
    subtitle: value ? 'Được phép xem lịch sử' : 'Giữ riêng tư',
    icon: icon,
    value: value,
    onChanged: onChanged,
    divider: divider,
  );

  Widget _toggleRow({
    required String title,
    required String subtitle,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
    required bool divider,
  }) {
    return Column(
      children: [
        SwitchListTile.adaptive(
          contentPadding: EdgeInsets.zero,
          secondary: Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: value ? const Color(0xFFFFE5DE) : const Color(0xFFF2ECE8),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(icon, size: 21, color: value ? _primary : _muted),
          ),
          title: Text(
            title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w600,
              color: _text,
            ),
          ),
          subtitle: Text(
            subtitle,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _muted,
            ),
          ),
          value: value,
          activeTrackColor: _accent,
          onChanged: _isSaving ? null : onChanged,
        ),
        if (divider) const Divider(height: 1, color: Color(0xFFEDE4DF)),
      ],
    );
  }

  Widget _buildPrivacyNote() {
    return const _SectionCard(
      color: Color(0xFFFFF3EA),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.lock_outline, color: _primary),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              'Bạn toàn quyền thay đổi hoặc thu hồi quyền bất cứ lúc nào. '
              'Thành viên chỉ được xem dữ liệu bạn đã chọn.',
              style: TextStyle(
                fontFamily: 'Lexend',
                height: 1.45,
                color: _muted,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PermissionLoading extends StatelessWidget {
  const _PermissionLoading();

  @override
  Widget build(BuildContext context) {
    return ListView(
      key: const Key('permission-loading'),
      padding: const EdgeInsets.all(16),
      children: List.generate(
        3,
        (index) => Container(
          height: index == 0 ? 86 : 190,
          margin: const EdgeInsets.only(bottom: 16),
          decoration: BoxDecoration(
            color: const Color(0xFFECE4DF),
            borderRadius: BorderRadius.circular(20),
          ),
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.child, this.color});

  final Widget child;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color ?? _ManageFamilyPermissionScreenState._surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(22),
        side: const BorderSide(color: Color(0xFFEDE4DF)),
      ),
      child: Padding(padding: const EdgeInsets.all(18), child: child),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.trailing,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: _ManageFamilyPermissionScreenState._primary),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: _ManageFamilyPermissionScreenState._text,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                subtitle,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _ManageFamilyPermissionScreenState._muted,
                ),
              ),
            ],
          ),
        ),
        ?trailing,
      ],
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFEAF3EC),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: Color(0xFF356344),
        ),
      ),
    );
  }
}
