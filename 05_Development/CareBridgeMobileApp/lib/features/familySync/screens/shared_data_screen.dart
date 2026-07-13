import 'package:flutter/material.dart';

class SharedDataScreen extends StatelessWidget {
  final String groupId;

  const SharedDataScreen({super.key, required this.groupId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chia sẻ',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
            fontFamily: 'Lexend',
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.more_horiz, color: Color(0xFF845143)),
            onPressed: () {},
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 8.0),
        children: [
          // Permission Banner
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFFF8DDD2), // secondary-fixed
              borderRadius: BorderRadius.circular(16),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: const BoxDecoration(
                    color: Color(0xFFFFE2D9),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.security, color: Color(0xFF845143)),
                ),
                const SizedBox(width: 16),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Dữ liệu của Bé Mỡ',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF271812),
                          fontFamily: 'Lexend',
                        ),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Được chia sẻ bởi Mẹ Linh. Bạn chỉ có quyền xem các danh mục được cấp phép.',
                        style: TextStyle(
                          fontSize: 14,
                          color: Color(0xCC271812),
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 24),

          // Filter Action
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Tổng quan',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF271812),
                  fontFamily: 'Lexend',
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: const Color(0xFFD6C2BD)),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x0F5A463F),
                      blurRadius: 20,
                      offset: Offset(0, 4),
                    ),
                  ],
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.calendar_month,
                      color: Color(0xFF845143),
                      size: 18,
                    ),
                    SizedBox(width: 8),
                    Text(
                      'Tháng 10, 2023',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF271812),
                        fontFamily: 'Lexend',
                      ),
                    ),
                    SizedBox(width: 4),
                    Icon(
                      Icons.arrow_drop_down,
                      color: Color(0xFF84736F),
                      size: 18,
                    ),
                  ],
                ),
              ),
            ],
          ),

          const SizedBox(height: 24),

          // Card 1: Biểu đồ tăng trưởng
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 40,
                          height: 40,
                          decoration: const BoxDecoration(
                            color: Color(0xFFFFDBD1),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.trending_up,
                            color: Color(0xFF693A2D),
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          'Biểu đồ tăng trưởng',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF271812),
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFE2D9),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: const Row(
                        children: [
                          Icon(
                            Icons.face_3,
                            size: 14,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 4),
                          Text(
                            'Mẹ Linh nhập',
                            style: TextStyle(
                              fontSize: 12,
                              color: Color(0xFF524440),
                              fontFamily: 'Lexend',
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 128,
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      _buildBar(
                        flex: 1,
                        heightFactor: 0.4,
                        color: const Color(0xFFF8DDD2),
                      ),
                      const SizedBox(width: 16),
                      _buildBar(
                        flex: 1,
                        heightFactor: 0.7,
                        color: const Color(0xFFC98C7B),
                        label: '7.2kg',
                      ),
                      const SizedBox(width: 16),
                      _buildBar(
                        flex: 1,
                        heightFactor: 0.85,
                        color: const Color(0xFFF8DDD2),
                      ),
                      const SizedBox(width: 16),
                      _buildBar(
                        flex: 1,
                        heightFactor: 0.95,
                        color: const Color(0xFFF8DDD2),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                const Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Tuần 1',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                        fontFamily: 'Lexend',
                      ),
                    ),
                    Text(
                      'Tuần 2',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                        fontFamily: 'Lexend',
                      ),
                    ),
                    Text(
                      'Tuần 3',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                        fontFamily: 'Lexend',
                      ),
                    ),
                    Text(
                      'Tuần 4',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                        fontFamily: 'Lexend',
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 24),

          // Card 2: Lịch sử tiêm chủng
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: const BoxDecoration(
                        color: Color(0xFFE9E1DB),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.vaccines,
                        color: Color(0xFF1E1B18),
                        size: 20,
                      ),
                    ),
                    const SizedBox(width: 8),
                    const Text(
                      'Lịch sử tiêm chủng',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF271812),
                        fontFamily: 'Lexend',
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                _buildVaccineItem(
                  'Mũi 6 trong 1 (Hexaxim)',
                  'Ngày tiêm: 15/10/2023',
                  'Hoàn thành',
                  const Color(0xFFFFDBD1),
                  const Color(0xFF693A2D),
                  Icons.check_circle,
                ),
                const Divider(color: Color(0xFFFFE2D9), height: 32),
                _buildVaccineItem(
                  'Phế cầu khuẩn (Synflorix)',
                  'Dự kiến: 20/11/2023',
                  'Sắp tới',
                  const Color(0xFFFADCD3),
                  const Color(0xFF524440),
                  Icons.schedule,
                ),
              ],
            ),
          ),

          const SizedBox(height: 24),

          // Card 3: Nhật ký giấc ngủ
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 40,
                          height: 40,
                          decoration: const BoxDecoration(
                            color: Color(0xFFFFE2D9),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.bedtime,
                            color: Color(0xFF6E5A52),
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          'Nhật ký giấc ngủ hôm nay',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF271812),
                            fontFamily: 'Lexend',
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFEF8F4),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: const Color(0xFFFADCD3)),
                  ),
                  child: Row(
                    children: [
                      _buildSleepStat(
                        'Ngủ ngày',
                        '3h 20m',
                        Icons.wb_sunny,
                        const Color(0xFF845143),
                      ),
                      Container(
                        width: 1,
                        height: 40,
                        color: const Color(0xFFFFE2D9),
                        margin: const EdgeInsets.symmetric(horizontal: 16),
                      ),
                      _buildSleepStat(
                        'Ngủ đêm',
                        '8h 45m',
                        Icons.nights_stay,
                        const Color(0xFF625D59),
                      ),
                      Container(
                        width: 1,
                        height: 40,
                        color: const Color(0xFFFFE2D9),
                        margin: const EdgeInsets.symmetric(horizontal: 16),
                      ),
                      _buildSleepStat(
                        'Tổng cộng',
                        '12h 05m',
                        Icons.timelapse,
                        const Color(0xFF6E5A52),
                        isPrimary: true,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 48),
        ],
      ),
    );
  }

  Widget _buildBar({
    required int flex,
    required double heightFactor,
    required Color color,
    String? label,
  }) {
    return Expanded(
      flex: flex,
      child: Stack(
        clipBehavior: Clip.none,
        alignment: Alignment.topCenter,
        children: [
          if (label != null)
            Positioned(
              top: -32,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFF3E2C26),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  label,
                  style: const TextStyle(
                    color: Color(0xFFFFEDE7),
                    fontSize: 12,
                    fontFamily: 'Lexend',
                  ),
                ),
              ),
            ),
          FractionallySizedBox(
            heightFactor: heightFactor,
            child: Container(
              decoration: BoxDecoration(
                color: color,
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(8),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVaccineItem(
    String title,
    String subtitle,
    String status,
    Color statusBg,
    Color statusColor,
    IconData icon,
  ) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFF271812),
                fontFamily: 'Lexend',
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: const TextStyle(
                fontSize: 14,
                color: Color(0xFF84736F),
                fontFamily: 'Lexend',
              ),
            ),
          ],
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: statusBg,
            borderRadius: BorderRadius.circular(999),
          ),
          child: Row(
            children: [
              Icon(icon, size: 14, color: statusColor),
              const SizedBox(width: 4),
              Text(
                status,
                style: TextStyle(
                  fontSize: 12,
                  color: statusColor,
                  fontFamily: 'Lexend',
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSleepStat(
    String label,
    String value,
    IconData icon,
    Color iconColor, {
    bool isPrimary = false,
  }) {
    return Expanded(
      child: Column(
        children: [
          Icon(icon, color: iconColor, size: 24),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF84736F),
              fontFamily: 'Lexend',
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: isPrimary
                  ? const Color(0xFF845143)
                  : const Color(0xFF271812),
              fontFamily: 'Lexend',
            ),
          ),
        ],
      ),
    );
  }
}
