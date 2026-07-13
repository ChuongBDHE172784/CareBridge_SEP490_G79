import 'package:flutter/material.dart';

import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class CompleteReminderScreen extends StatefulWidget {
  final String reminderId;

  const CompleteReminderScreen({super.key, required this.reminderId});

  @override
  State<CompleteReminderScreen> createState() => _CompleteReminderScreenState();
}

class _CompleteReminderScreenState extends State<CompleteReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _error = Color(0xFFBA1A1A);

  final _service = ReminderService.instance;
  Reminder? _reminder;
  bool _loading = true;
  bool _processing = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    try {
      final reminder = await _service.getReminderDetail(widget.reminderId);
      if (!mounted) return;
      setState(() {
        _reminder = reminder;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorText = 'Unable to load reminder.';
        _loading = false;
      });
    }
  }

  Future<void> _complete() async {
    setState(() => _processing = true);
    try {
      await _service.completeReminder(widget.reminderId);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Reminder completed.')));
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Unable to complete reminder: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Complete reminder',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _errorText != null
          ? _ErrorState(message: _errorText!, onRetry: _load)
          : _CompleteContent(
              reminder: _reminder!,
              processing: _processing,
              onComplete: _complete,
            ),
    );
  }
}

class _CompleteContent extends StatelessWidget {
  final Reminder reminder;
  final bool processing;
  final VoidCallback onComplete;

  const _CompleteContent({
    required this.reminder,
    required this.processing,
    required this.onComplete,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    final isTerminal = reminder.status.isTerminal;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
      children: [
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(18),
            boxShadow: [
              BoxShadow(
                color: _primary.withAlpha(18),
                blurRadius: 16,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: _primaryContainer.withAlpha(40),
                child: const Icon(
                  Icons.check_circle_outline_rounded,
                  color: _primary,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      reminder.title,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        color: _onSurface,
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Due ${_formatDateTime(reminder.scheduledAt)}',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Text(
          isTerminal
              ? 'This reminder is already ${reminder.status.displayLabel.toLowerCase()}.'
              : 'Complete only this reminder occurrence.',
          style: const TextStyle(
            fontFamily: 'Lexend',
            color: _onSurfaceVariant,
            height: 1.45,
          ),
        ),
        const SizedBox(height: 20),
        FilledButton.icon(
          onPressed: processing || isTerminal ? null : onComplete,
          icon: processing
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    color: Colors.white,
                    strokeWidth: 2,
                  ),
                )
              : const Icon(Icons.task_alt_rounded),
          label: const Text(
            'Complete',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w800),
          ),
          style: FilledButton.styleFrom(
            backgroundColor: _primary,
            minimumSize: const Size.fromHeight(54),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ),
      ],
    );
  }

  static String _formatDateTime(DateTime value) {
    final local = value.toLocal();
    final d = local.day.toString().padLeft(2, '0');
    final m = local.month.toString().padLeft(2, '0');
    final h = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$d/$m/${local.year} $h:$min';
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: Color(0xFFBA1A1A),
            size: 44,
          ),
          const SizedBox(height: 10),
          Text(message, style: const TextStyle(fontFamily: 'Lexend')),
          const SizedBox(height: 10),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      ),
    );
  }
}
