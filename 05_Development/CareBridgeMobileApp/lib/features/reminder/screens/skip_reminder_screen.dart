import 'package:flutter/material.dart';

class SkipReminderScreen extends StatefulWidget {
  final String reminderId;

  const SkipReminderScreen({
    Key? key,
    required this.reminderId,
  }) : super(key: key);

  @override
  State<SkipReminderScreen> createState() => _SkipReminderScreenState();
}

class _SkipReminderScreenState extends State<SkipReminderScreen> {
  final TextEditingController _reasonController = TextEditingController();
  bool _isProcessing = false;
  bool _showToast = false;

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _skipReminder() async {
    setState(() => _isProcessing = true);
    
    try {
      // API call: POST /api/v1/reminders/{reminderId}/skip
      // final reason = _reasonController.text;
      await Future.delayed(const Duration(milliseconds: 600)); // Simulate network
      
      setState(() {
        _isProcessing = false;
        _showToast = true;
      });
      
      // Auto dismiss after 2s and pop
      Future.delayed(const Duration(seconds: 2), () {
        if (mounted && _showToast) {
          Navigator.pop(context, true);
        }
      });
      
    } catch (e) {
      setState(() => _isProcessing = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Có lỗi xảy ra')),
        );
      }
    }
  }

  void _undoSkip() {
    setState(() {
      _showToast = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFF845143);
    const bgColor = Color(0xFFFEF8F4);
    const textColor = Color(0xFF2D2A28);
    const primaryFixedColor = Color(0xFFFFDBD1);

    return Scaffold(
      backgroundColor: bgColor,
      appBar: AppBar(
        backgroundColor: bgColor,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: primaryColor),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'CareBridge',
          style: TextStyle(
            color: primaryColor,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 20,
          ),
        ),
        actions: const [
          SizedBox(width: 48), // Spacer
        ],
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
            child: AnimatedOpacity(
              opacity: _showToast ? 0.4 : 1.0,
              duration: const Duration(milliseconds: 300),
              child: IgnorePointer(
                ignoring: _showToast,
                child: Column(
                  children: [
                    // Reminder Summary Section
                    Container(
                      padding: const EdgeInsets.all(24),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(32),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFC98C7B).withOpacity(0.08),
                            blurRadius: 20,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Container(
                                width: 48,
                                height: 48,
                                decoration: const BoxDecoration(
                                  color: primaryFixedColor,
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(Icons.medical_services, color: Color(0xFF693A2D)),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      'SẮP DIỄN RA LÚC 08:30',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold,
                                        color: primaryColor,
                                        letterSpacing: 1.2,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    const Text(
                                      'Uống thuốc điều trị cao huyết áp',
                                      style: TextStyle(
                                        fontSize: 20,
                                        fontWeight: FontWeight.bold,
                                        color: textColor,
                                        fontFamily: 'Quicksand',
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      'Liều lượng: 1 viên (5mg), sau khi ăn sáng.',
                                      style: TextStyle(
                                        fontSize: 14,
                                        color: textColor.withOpacity(0.7),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 24),
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF6F1EC), // surface-accent-light
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: const Color(0xFFD6C2BD).withOpacity(0.3)),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: const [
                                    Icon(Icons.calendar_today, color: primaryColor, size: 20),
                                    SizedBox(width: 8),
                                    Text(
                                      'Chỉ lần này',
                                      style: TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.bold,
                                        color: primaryColor,
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                Text(
                                  'Các lần nhắc nhở tiếp theo trong lịch trình sẽ không thay đổi.',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: const Color(0xFF524440),
                                    fontStyle: FontStyle.italic,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    
                    const SizedBox(height: 24),
                    
                    // Skip Reason & Preview
                    Container(
                      padding: const EdgeInsets.all(24),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(32),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFFC98C7B).withOpacity(0.08),
                            blurRadius: 20,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Padding(
                            padding: EdgeInsets.only(left: 4, bottom: 12),
                            child: Text(
                              'Lý do bỏ qua (không bắt buộc)',
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF524440),
                              ),
                            ),
                          ),
                          TextField(
                            controller: _reasonController,
                            maxLines: 4,
                            decoration: InputDecoration(
                              hintText: 'Ví dụ: Đã uống sớm hơn, Hết thuốc...',
                              hintStyle: TextStyle(color: const Color(0xFF84736F).withOpacity(0.6)),
                              filled: true,
                              fillColor: const Color(0xFFF6F1EC),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(16),
                                borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
                              ),
                              enabledBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(16),
                                borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(16),
                                borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2),
                              ),
                            ),
                          ),
                          
                          const SizedBox(height: 24),
                          
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF9F2EE), // surface-container-low
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.update, color: Color(0xFF625D59)), // secondary
                                const SizedBox(width: 12),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: const [
                                    Text(
                                      'LẦN NHẮC KẾ TIẾP',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.bold,
                                        color: Color(0xFF625D59),
                                      ),
                                    ),
                                    SizedBox(height: 2),
                                    Text(
                                      'Mai, 08:30',
                                      style: TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.bold,
                                        color: textColor,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    
                    const SizedBox(height: 32),
                    
                    // Call to Action
                    ElevatedButton(
                      onPressed: _isProcessing ? null : _skipReminder,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFC98C7B),
                        disabledBackgroundColor: const Color(0xFFC98C7B).withOpacity(0.6),
                        minimumSize: const Size(double.infinity, 56),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(28),
                        ),
                        elevation: 4,
                      ),
                      child: _isProcessing
                          ? const SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                            )
                          : const Text(
                              'Bỏ qua lần này',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF51271B), // on-primary-container
                                fontFamily: 'Quicksand',
                              ),
                            ),
                    ),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      style: TextButton.styleFrom(
                        minimumSize: const Size(double.infinity, 48),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                      child: const Text(
                        'Hủy bỏ',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF625D59), // secondary
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          
          // Success State Toast
          AnimatedPositioned(
            duration: const Duration(milliseconds: 500),
            curve: Curves.easeOutBack,
            bottom: _showToast ? 40 : -100,
            left: 20,
            right: 20,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: const Color(0xFF32302D), // inverse-surface
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.2),
                    blurRadius: 20,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: const [
                      Icon(Icons.check_circle, color: primaryFixedColor),
                      SizedBox(width: 12),
                      Text(
                        'Đã bỏ qua nhắc nhở',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                  ElevatedButton(
                    onPressed: _undoSkip,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: primaryColor,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
                      minimumSize: const Size(80, 36),
                    ),
                    child: const Text('Hoàn tác'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
