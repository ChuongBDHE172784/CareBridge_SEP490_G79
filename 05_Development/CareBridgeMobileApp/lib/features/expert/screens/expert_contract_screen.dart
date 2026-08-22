import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/expert_onboarding_model.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

/// Trang ký Thoả thuận hợp tác chuyên gia (click-wrap).
///
/// Bốn chi tiết ở đây phục vụ mục đích pháp lý chứ không phải trang trí:
///  1. Toàn văn render ngay trên trang, không phải link tải rồi tick đồng ý;
///  2. Nút đồng ý chỉ bật sau khi người đọc cuộn hết — bằng chứng đã có cơ hội đọc;
///  3. Checkbox không tick sẵn;
///  4. Gõ lại họ tên đúng như hồ sơ đã duyệt — bằng chứng về chủ ý, thay cho chữ ký.
class ExpertContractScreen extends StatefulWidget {
  const ExpertContractScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertContractScreen> createState() => _ExpertContractScreenState();
}

class _ExpertContractScreenState extends State<ExpertContractScreen> {
  static const _background = Color(0xFFF7F1ED);
  static const _accent = Color(0xFFC98C7B);
  static const _ink = Color(0xFF3B2C27);

  final ScrollController _scrollController = ScrollController();
  final TextEditingController _nameController = TextEditingController();

  ExpertContractOffer? _offer;
  bool _loading = true;
  bool _saving = false;
  bool _scrolledToEnd = false;
  bool _agreed = false;
  String? _error;

  ExpertOnboardingService get _service =>
      widget.service ?? ExpertOnboardingService.instance;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrolledToEnd || !_scrollController.hasClients) return;
    final position = _scrollController.position;
    // Nội dung ngắn hơn khung nhìn thì coi như đã đọc hết.
    if (position.maxScrollExtent <= 0 ||
        position.pixels >= position.maxScrollExtent - 24) {
      setState(() => _scrolledToEnd = true);
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final offer = await _service.getContractOffer();
      if (!mounted) return;
      setState(() {
        _offer = offer;
        _loading = false;
      });
      WidgetsBinding.instance.addPostFrameCallback((_) => _onScroll());
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không tải được Thoả thuận hợp tác. Vui lòng thử lại.';
      });
    }
  }

  bool get _nameMatches {
    final expected = _offer?.expectedFullName ?? '';
    if (expected.isEmpty) return _nameController.text.trim().isNotEmpty;
    return _normalize(_nameController.text) == _normalize(expected);
  }

  bool get _canSubmit =>
      !_saving && _scrolledToEnd && _agreed && _nameMatches && _offer != null;

  Future<void> _accept() async {
    final offer = _offer;
    if (offer == null || !_canSubmit) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.acceptContract(
        termsVersion: offer.termsVersion,
        termsHash: offer.termsHash,
        acceptedFullName: _nameController.text.trim(),
      );
      final state = await _service.loadState();
      ExpertOnboardingStore.instance.update(state);
      if (!mounted) return;
      context.go('/expert-onboarding');
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _error =
            'Không ghi nhận được chấp nhận. Điều khoản có thể đã được cập nhật — '
            'vui lòng tải lại và đọc bản mới nhất.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final offer = _offer;
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        elevation: 0,
        foregroundColor: _ink,
        title: const Text(
          'Điều khoản hợp tác chuyên gia',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
            fontSize: 17,
          ),
        ),
      ),
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _accent),
              )
            : offer == null
            ? _ErrorState(message: _error, onRetry: _load)
            : Column(
                children: [
                  _OfferHeader(offer: offer),
                  Expanded(
                    child: Container(
                      margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFE6DAD3)),
                      ),
                      child: Scrollbar(
                        controller: _scrollController,
                        thumbVisibility: true,
                        child: SingleChildScrollView(
                          key: const Key('contract-body'),
                          controller: _scrollController,
                          padding: const EdgeInsets.all(16),
                          child: SelectableText(
                            offer.content,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12.8,
                              height: 1.62,
                              color: Color(0xFF463630),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  _AcceptancePanel(
                    scrolledToEnd: _scrolledToEnd,
                    agreed: _agreed,
                    saving: _saving,
                    canSubmit: _canSubmit,
                    nameController: _nameController,
                    expectedName: offer.expectedFullName,
                    nameMatches: _nameMatches,
                    error: _error,
                    onAgreedChanged: (value) =>
                        setState(() => _agreed = value ?? false),
                    onNameChanged: () => setState(() {}),
                    onSubmit: _accept,
                  ),
                ],
              ),
      ),
    );
  }

  static String _normalize(String value) => value
      .trim()
      .replaceAll(RegExp(r'\s+'), ' ')
      .toLowerCase();
}

class _OfferHeader extends StatelessWidget {
  const _OfferHeader({required this.offer});

  final ExpertContractOffer offer;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFF10B981).withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.description_outlined,
              size: 18, color: Color(0xFF0F8A63)),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'Số ${offer.contractNumber} · Phiên bản ${offer.termsVersion} · '
              'Thời hạn ${offer.termMonths} tháng · Cam kết ${offer.minSlotsPerWeek} ca/tuần',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11.8,
                fontWeight: FontWeight.w600,
                color: Color(0xFF0F6B4F),
                height: 1.45,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _AcceptancePanel extends StatelessWidget {
  const _AcceptancePanel({
    required this.scrolledToEnd,
    required this.agreed,
    required this.saving,
    required this.canSubmit,
    required this.nameController,
    required this.expectedName,
    required this.nameMatches,
    required this.error,
    required this.onAgreedChanged,
    required this.onNameChanged,
    required this.onSubmit,
  });

  final bool scrolledToEnd;
  final bool agreed;
  final bool saving;
  final bool canSubmit;
  final TextEditingController nameController;
  final String expectedName;
  final bool nameMatches;
  final String? error;
  final ValueChanged<bool?> onAgreedChanged;
  final VoidCallback onNameChanged;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
      decoration: const BoxDecoration(
        color: Color(0xFFF7F1ED),
        border: Border(top: BorderSide(color: Color(0xFFE6DAD3))),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!scrolledToEnd)
            Row(
              children: const [
                Icon(Icons.arrow_downward_rounded,
                    size: 16, color: Color(0xFF7B6A63)),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Vui lòng đọc hết Thoả thuận để tiếp tục.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12.5,
                      color: Color(0xFF7B6A63),
                    ),
                  ),
                ),
              ],
            ),
          if (scrolledToEnd) ...[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(
                  width: 26,
                  height: 26,
                  child: Checkbox(
                    key: const Key('contract-agree'),
                    value: agreed,
                    onChanged: saving ? null : onAgreedChanged,
                    activeColor: const Color(0xFFC98C7B),
                  ),
                ),
                const SizedBox(width: 10),
                const Expanded(
                  child: Text(
                    'Tôi đã đọc, hiểu và đồng ý với toàn bộ nội dung Thoả thuận hợp tác '
                    'chuyên gia nêu trên.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      height: 1.45,
                      color: Color(0xFF3B2C27),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('contract-name'),
              controller: nameController,
              enabled: !saving,
              onChanged: (_) => onNameChanged(),
              textCapitalization: TextCapitalization.words,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 14),
              decoration: InputDecoration(
                labelText: 'Gõ lại họ và tên để xác nhận',
                helperText: expectedName.isEmpty
                    ? null
                    : 'Đúng như hồ sơ đã duyệt: $expectedName',
                helperMaxLines: 2,
                helperStyle: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11.5,
                  color: Color(0xFF7B6A63),
                ),
                labelStyle: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: Color(0xFF7B6A63),
                ),
                filled: true,
                fillColor: Colors.white,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: Color(0xFFE6DAD3)),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide(
                    color: nameController.text.isEmpty || nameMatches
                        ? const Color(0xFFE6DAD3)
                        : const Color(0xFFB4342A),
                  ),
                ),
              ),
            ),
          ],
          if (error != null) ...[
            const SizedBox(height: 10),
            Text(
              error!,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12.5,
                color: Color(0xFFB4342A),
                height: 1.45,
              ),
            ),
          ],
          const SizedBox(height: 14),
          SizedBox(
            height: 50,
            width: double.infinity,
            child: FilledButton(
              key: const Key('contract-accept'),
              onPressed: canSubmit ? onSubmit : null,
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFC98C7B),
                disabledBackgroundColor:
                    const Color(0xFFC98C7B).withValues(alpha: 0.35),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: saving
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.4,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Xác nhận chấp nhận',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 15.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String? message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded,
                size: 52, color: Color(0xFF845143)),
            const SizedBox(height: 16),
            Text(
              message ?? 'Không tải được Thoả thuận hợp tác.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13.5,
                color: Color(0xFF7B6A63),
                height: 1.5,
              ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Thử lại'),
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFC98C7B),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
