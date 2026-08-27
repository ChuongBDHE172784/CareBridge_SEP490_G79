import 'package:flutter/material.dart';

import '../models/linked_account.dart';
import '../services/auth_service.dart';

typedef LoadLinkedAccount = Future<LinkedAccount> Function();
typedef LinkGoogleAccount = Future<LinkedAccount> Function();

class LinkedAccountsScreen extends StatefulWidget {
  const LinkedAccountsScreen({
    super.key,
    this.loadLinkedAccount,
    this.onLinkGoogle,
  });

  final LoadLinkedAccount? loadLinkedAccount;
  final LinkGoogleAccount? onLinkGoogle;

  @override
  State<LinkedAccountsScreen> createState() => _LinkedAccountsScreenState();
}

class _LinkedAccountsScreenState extends State<LinkedAccountsScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFFFF);
  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentPressed = Color(0xFFB67868);
  static const _text = Color(0xFF5A463F);
  static const _secondaryText = Color(0xFF9C857C);
  static const _divider = Color(0xFFE8DDD6);

  LinkedAccount? _googleAccount;
  bool _loading = true;
  bool _linking = false;
  bool _loadFailed = false;
  String? _linkError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _loadFailed = false;
      });
    }
    try {
      final loader =
          widget.loadLinkedAccount ??
          AuthService.instance.getLinkedGoogleAccount;
      final account = await loader();
      if (!mounted) return;
      setState(() {
        _googleAccount = account;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _loadFailed = true;
      });
    }
  }

  Future<void> _linkGoogle() async {
    setState(() {
      _linking = true;
      _linkError = null;
    });
    try {
      final linker =
          widget.onLinkGoogle ?? AuthService.instance.linkGoogleAccount;
      final account = await linker();
      if (!mounted) return;
      setState(() => _googleAccount = account);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text(
            'Đã liên kết tài khoản Google.',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
          ),
          backgroundColor: _text,
          behavior: SnackBarBehavior.floating,
          shape: const StadiumBorder(),
          margin: const EdgeInsets.all(20),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      final failure = LinkedAccountFailure.from(error);
      if (!failure.isCanceled) {
        setState(() => _linkError = failure.userMessage);
      }
    } finally {
      if (mounted) setState(() => _linking = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: const Key('linked-accounts-screen'),
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        centerTitle: true,
        title: const Text(
          'Tài khoản liên kết',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _text,
          ),
        ),
      ),
      body: SafeArea(
        top: false,
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(
                  color: _accent,
                  semanticsLabel: 'Đang tải tài khoản liên kết',
                ),
              )
            : _loadFailed
            ? _buildLoadFailure()
            : _buildContent(),
      ),
    );
  }

  Widget _buildLoadFailure() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, color: _accent, size: 48),
            const SizedBox(height: 16),
            const Text(
              'Không thể tải tài khoản liên kết',
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
              'Hãy kiểm tra kết nối mạng và thử lại.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                color: _secondaryText,
              ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('linked-accounts-retry'),
              onPressed: _load,
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Thử lại'),
              style: _primaryButtonStyle(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 32),
      children: [
        const Text(
          'Quản lý cách đăng nhập',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: _text,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Liên kết Google để đăng nhập nhanh vào đúng tài khoản CareBridge hiện tại.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            height: 1.5,
            color: _secondaryText,
          ),
        ),
        const SizedBox(height: 24),
        _buildGoogleCard(),
      ],
    );
  }

  Widget _buildGoogleCard() {
    final account = _googleAccount!;
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: _divider.withValues(alpha: 0.5)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 32,
            offset: Offset(0, 12),
          ),
          BoxShadow(
            color: Color.fromRGBO(201, 140, 123, 0.05),
            blurRadius: 12,
            offset: Offset(-4, -2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: const BoxDecoration(
                  color: _nestedSurface,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.account_circle_outlined,
                  color: _accent,
                  size: 26,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Google',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: _text,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      account.email ??
                          'Dùng tài khoản Google để đăng nhập CareBridge.',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _secondaryText,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Semantics(
            key: const Key('google-link-status'),
            label: account.linked
                ? 'Google đã liên kết'
                : 'Google chưa liên kết',
            child: Align(
              alignment: Alignment.centerLeft,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: account.linked
                      ? const Color(0xFFE7F2EA)
                      : _nestedSurface,
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      account.linked
                          ? Icons.check_circle_outline
                          : Icons.link_off_outlined,
                      color: account.linked
                          ? const Color(0xFF52745B)
                          : _secondaryText,
                      size: 18,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      account.linked ? 'Đã liên kết' : 'Chưa liên kết',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: account.linked ? const Color(0xFF52745B) : _text,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (_linkError != null) ...[
            const SizedBox(height: 16),
            Semantics(
              liveRegion: true,
              child: Container(
                key: const Key('linked-account-error'),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _nestedSurface,
                  borderRadius: BorderRadius.circular(16),
                  border: const Border(
                    left: BorderSide(color: _accent, width: 4),
                  ),
                ),
                child: Text(
                  _linkError!,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    height: 1.4,
                    color: _text,
                  ),
                ),
              ),
            ),
          ],
          if (!account.linked) ...[
            const SizedBox(height: 20),
            Semantics(
              button: true,
              label: 'Liên kết tài khoản Google',
              child: FilledButton.icon(
                key: const Key('link-google-account'),
                onPressed: _linking ? null : _linkGoogle,
                icon: _linking
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.link_rounded),
                label: Text(
                  _linking ? 'Đang liên kết...' : 'Liên kết với Google',
                ),
                style: _primaryButtonStyle(),
              ),
            ),
          ],
        ],
      ),
    );
  }

  ButtonStyle _primaryButtonStyle() {
    return FilledButton.styleFrom(
      minimumSize: const Size.fromHeight(48),
      backgroundColor: _accent,
      disabledBackgroundColor: _accent.withValues(alpha: 0.55),
      foregroundColor: Colors.white,
      textStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        fontWeight: FontWeight.w700,
      ),
      shape: const StadiumBorder(),
      shadowColor: _text.withValues(alpha: 0.08),
      elevation: 4,
    ).copyWith(
      overlayColor: WidgetStateProperty.resolveWith(
        (states) =>
            states.contains(WidgetState.pressed) ? _accentPressed : null,
      ),
    );
  }
}
