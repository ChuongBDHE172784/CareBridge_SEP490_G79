import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class SwitchActiveBabySheet extends StatefulWidget {
  final VoidCallback onActiveBabyChanged;

  const SwitchActiveBabySheet({
    Key? key,
    required this.onActiveBabyChanged,
  }) : super(key: key);

  @override
  State<SwitchActiveBabySheet> createState() => _SwitchActiveBabySheetState();
}

class _SwitchActiveBabySheetState extends State<SwitchActiveBabySheet> {
  bool _isLoading = false;
  
  // Mock data for profiles - in real app, fetch from GET /api/v1/babies
  final List<Map<String, dynamic>> _profiles = [
    {
      'id': '123',
      'name': 'Bé Na',
      'age': '8 tháng tuổi',
      'isActive': true,
      'avatarUrl': 'https://ui-avatars.com/api/?name=Na&background=c98c7b&color=fff',
      'updatedAt': '2 giờ trước',
    },
    {
      'id': '456',
      'name': 'Bé Bo',
      'age': '2 tuổi 3 tháng',
      'isActive': false,
      'avatarUrl': 'https://ui-avatars.com/api/?name=Bo&background=dbc1b7&color=fff',
      'updatedAt': '2 ngày trước',
    },
  ];

  Future<void> _switchActiveBaby(String babyId) async {
    setState(() => _isLoading = true);
    try {
      // API call: PATCH /api/v1/babies/{babyId}/active
      // await apiPatch('/api/v1/babies/$babyId/active', body: {});
      
      // MOCK: simulate network delay
      await Future.delayed(const Duration(milliseconds: 600));
      
      if (mounted) {
        widget.onActiveBabyChanged();
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: Không thể chuyển đổi hồ sơ bé')),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFFC98C7B);
    const textColor = Color(0xFF5A463F);
    const secondaryTextColor = Color(0xFF9C857C);
    
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      padding: const EdgeInsets.only(left: 20, right: 20, bottom: 32, top: 12),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Drag handle
          Container(
            width: 48,
            height: 6,
            margin: const EdgeInsets.only(bottom: 24),
            decoration: BoxDecoration(
              color: Colors.grey[300],
              borderRadius: BorderRadius.circular(3),
            ),
          ),
          const Text(
            'Chọn Hồ Sơ Bé',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: textColor,
              fontFamily: 'Quicksand',
            ),
          ),
          const SizedBox(height: 24),
          
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(32.0),
              child: Center(child: CircularProgressIndicator(color: primaryColor)),
            )
          else
            Column(
              children: [
                ..._profiles.map((profile) => _buildProfileItem(profile, primaryColor, textColor, secondaryTextColor)),
                
                // Add new profile action
                InkWell(
                  onTap: () {
                    // Navigate to Add Baby Profile Screen
                    Navigator.pop(context);
                    // Navigator.pushNamed(context, '/add_baby');
                  },
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      border: Border.all(
                        color: Colors.grey[400]!,
                        style: BorderStyle.solid,
                      ),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 56,
                          height: 56,
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.add, color: primaryColor),
                        ),
                        const SizedBox(width: 16),
                        const Text(
                          'Thêm hồ sơ bé mới',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: primaryColor,
                            fontFamily: 'Quicksand',
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          
          const SizedBox(height: 8),
          
          // Manage action
          OutlinedButton.icon(
            onPressed: () {
              Navigator.pop(context);
              // Navigate to manage profiles
            },
            icon: const Icon(Icons.manage_accounts, color: primaryColor),
            label: const Text(
              'Quản lý hồ sơ',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: primaryColor,
              side: const BorderSide(color: primaryColor, width: 2),
              minimumSize: const Size(double.infinity, 56),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(28),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProfileItem(Map<String, dynamic> profile, Color primaryColor, Color textColor, Color secondaryColor) {
    final bool isActive = profile['isActive'];
    
    return InkWell(
      onTap: () {
        if (!isActive) {
          _switchActiveBaby(profile['id']);
        }
      },
      borderRadius: BorderRadius.circular(16),
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isActive ? primaryColor.withOpacity(0.1) : Colors.white,
          border: Border.all(
            color: isActive ? primaryColor : Colors.grey[200]!,
            width: 2,
          ),
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.02),
              blurRadius: 10,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            Stack(
              children: [
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: isActive ? primaryColor : Colors.transparent,
                      width: 2,
                    ),
                    image: DecorationImage(
                      image: NetworkImage(profile['avatarUrl']),
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
                if (isActive)
                  Positioned(
                    bottom: 0,
                    right: 0,
                    child: Container(
                      width: 18,
                      height: 18,
                      decoration: BoxDecoration(
                        color: Colors.green[400],
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 2),
                      ),
                      child: const Icon(Icons.check, color: Colors.white, size: 12),
                    ),
                  ),
              ],
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        profile['name'],
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: textColor,
                          fontFamily: 'Quicksand',
                        ),
                      ),
                      if (isActive)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: primaryColor,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Text(
                            'Đang chọn',
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    profile['age'],
                    style: TextStyle(
                      fontSize: 14,
                      color: secondaryColor,
                      fontFamily: 'Quicksand',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Cập nhật: ${profile['updatedAt']}',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[500],
                      fontFamily: 'Quicksand',
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
