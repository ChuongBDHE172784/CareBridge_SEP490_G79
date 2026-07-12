import 'package:flutter/material.dart';
import '../models/care_group_model.dart';
import '../services/family_task_service.dart';
import '../services/care_group_service.dart';

class AssignCareTaskScreen extends StatefulWidget {
  final String groupId;
  final String groupName;

  const AssignCareTaskScreen({
    Key? key,
    required this.groupId,
    required this.groupName,
  }) : super(key: key);

  @override
  State<AssignCareTaskScreen> createState() => _AssignCareTaskScreenState();
}

class _AssignCareTaskScreenState extends State<AssignCareTaskScreen> {
  final _taskService = FamilyTaskService();
  final _groupService = CareGroupService();
  
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  
  List<CareGroupMember> _members = [];
  CareGroupMember? _selectedAssignee;
  DateTime? _dueAt;
  
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _loadMembers();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _loadMembers() async {
    try {
      final group = await _groupService.getGroupMembers(widget.groupId);
      if (mounted) {
        setState(() {
          _members = group.members.where((m) => m.inviteStatus == 'ACCEPTED').toList();
          if (_members.isNotEmpty) {
            _selectedAssignee = _members.first;
          }
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi tải thành viên: $e')));
      }
    }
  }

  Future<void> _pickDateTime() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _dueAt ?? DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: Color(0xFF845143),
              onPrimary: Colors.white,
              onSurface: Color(0xFF271812),
            ),
          ),
          child: child!,
        );
      },
    );
    if (date != null && mounted) {
      final time = await showTimePicker(
        context: context,
        initialTime: _dueAt != null ? TimeOfDay.fromDateTime(_dueAt!) : TimeOfDay.now(),
        builder: (context, child) {
          return Theme(
            data: Theme.of(context).copyWith(
              colorScheme: const ColorScheme.light(
                primary: Color(0xFF845143),
                onPrimary: Colors.white,
                onSurface: Color(0xFF271812),
              ),
            ),
            child: child!,
          );
        },
      );
      if (time != null && mounted) {
        setState(() {
          _dueAt = DateTime(date.year, date.month, date.day, time.hour, time.minute);
        });
      }
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedAssignee == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vui lòng chọn người thực hiện')));
      return;
    }
    if (_dueAt == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vui lòng chọn hạn chót')));
      return;
    }

    setState(() => _isSaving = true);
    try {
      await _taskService.assignTask(
        widget.groupId,
        _selectedAssignee!.memberId,
        _titleController.text.trim(),
        _descriptionController.text.trim(),
        _dueAt!,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đã giao việc thành công!')));
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isSaving = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Giao việc mới', style: TextStyle(color: Color(0xFF845143), fontSize: 22, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFFC98C7B)))
          : SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Context Section
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(24),
                        border: Border.all(color: const Color(0x66FFFFFF)),
                        boxShadow: const [
                          BoxShadow(color: Color(0x1FC98C7B), blurRadius: 16, offset: Offset(8, 8)),
                          BoxShadow(color: Color(0xCCFFFFFF), blurRadius: 12, offset: Offset(-6, -6)),
                        ],
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 64, height: 64,
                            decoration: BoxDecoration(color: const Color(0xFFFFE9E3), shape: BoxShape.circle, border: Border.all(color: Colors.white, width: 4)),
                            child: const Icon(Icons.groups, color: Color(0xFF845143), size: 32),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(widget.groupName, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                                const SizedBox(height: 4),
                                Row(
                                  children: [
                                    const Icon(Icons.group, size: 16, color: Color(0xFF524440)),
                                    const SizedBox(width: 4),
                                    Text('${_members.length} thành viên khả dụng', style: const TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand')),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 32),
                    
                    const Text('Phân bổ trách nhiệm để mọi người cùng chung tay.', style: TextStyle(fontSize: 14, color: Color(0xFF524440), fontFamily: 'Quicksand')),
                    const SizedBox(height: 24),
                    
                    // Task Title
                    const Text('Tiêu đề công việc', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _titleController,
                      decoration: InputDecoration(
                        hintText: 'VD: Mua thuốc bổ cho bà',
                        filled: true,
                        fillColor: const Color(0xFFFDFAF8),
                        contentPadding: const EdgeInsets.all(16),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(20), borderSide: BorderSide.none),
                        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(20), borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2)),
                      ),
                      validator: (value) => value == null || value.trim().isEmpty ? 'Vui lòng nhập tiêu đề' : null,
                    ),
                    
                    const SizedBox(height: 24),
                    
                    // Assignee Selection
                    const Text('Người thực hiện', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                    const SizedBox(height: 12),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: _members.map((m) => _buildAssigneeAvatar(m)).toList(),
                      ),
                    ),
                    
                    const SizedBox(height: 24),
                    
                    // Deadline & Status
                    Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Hạn chót', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                              const SizedBox(height: 8),
                              GestureDetector(
                                onTap: _pickDateTime,
                                child: Container(
                                  padding: const EdgeInsets.all(16),
                                  decoration: BoxDecoration(color: const Color(0xFFFDFAF8), borderRadius: BorderRadius.circular(20)),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.event, color: Color(0xFF845143), size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(
                                          _dueAt == null ? 'Chọn ngày giờ' : '${_dueAt!.day}/${_dueAt!.month} ${_dueAt!.hour.toString().padLeft(2, '0')}:${_dueAt!.minute.toString().padLeft(2, '0')}',
                                          style: TextStyle(color: _dueAt == null ? const Color(0xFF84736F) : const Color(0xFF271812), fontSize: 14, fontFamily: 'Quicksand'),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Trạng thái', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                              const SizedBox(height: 8),
                              Container(
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20)),
                                child: Row(
                                  children: [
                                    Container(width: 8, height: 8, decoration: const BoxDecoration(color: Color(0xFF845143), shape: BoxShape.circle)),
                                    const SizedBox(width: 8),
                                    const Text('Chưa bắt đầu', style: TextStyle(color: Color(0xFF845143), fontWeight: FontWeight.bold, fontSize: 14, fontFamily: 'Quicksand')),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    
                    const SizedBox(height: 24),
                    
                    // Message
                    const Text('Lời nhắn', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Color(0xFF271812), fontFamily: 'Quicksand')),
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _descriptionController,
                      maxLines: 3,
                      decoration: InputDecoration(
                        hintText: 'Ghi chú thêm cho người thực hiện...',
                        filled: true,
                        fillColor: const Color(0xFFFDFAF8),
                        contentPadding: const EdgeInsets.all(16),
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(20), borderSide: BorderSide.none),
                        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(20), borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2)),
                      ),
                    ),
                    
                    const SizedBox(height: 32),
                    
                    ElevatedButton.icon(
                      onPressed: _isSaving ? null : _submit,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFC98C7B),
                        foregroundColor: Colors.white,
                        minimumSize: const Size(double.infinity, 56),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
                        elevation: 4,
                        shadowColor: const Color(0x4DC98C7B),
                      ),
                      icon: _isSaving ? const SizedBox.shrink() : const Icon(Icons.save),
                      label: _isSaving 
                        ? const CircularProgressIndicator(color: Colors.white)
                        : const Text('Lưu công việc', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, fontFamily: 'Quicksand')),
                    ),
                    
                    const SizedBox(height: 48),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildAssigneeAvatar(CareGroupMember m) {
    final isSelected = _selectedAssignee?.memberId == m.memberId;
    return GestureDetector(
      onTap: () => setState(() => _selectedAssignee = m),
      child: Container(
        margin: const EdgeInsets.only(right: 16),
        child: Column(
          children: [
            Container(
              width: 56, height: 56,
              decoration: BoxDecoration(
                color: const Color(0xFFFFE9E3),
                shape: BoxShape.circle,
                border: isSelected ? Border.all(color: const Color(0xFFC98C7B), width: 3) : Border.all(color: Colors.transparent, width: 3),
                boxShadow: isSelected ? const [BoxShadow(color: Color(0x33C98C7B), blurRadius: 8, offset: Offset(0, 4))] : null,
              ),
              child: Center(
                child: Text(
                  m.displayName.isNotEmpty ? m.displayName[0] : '?',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: isSelected ? const Color(0xFF845143) : const Color(0xFFA09A95), fontFamily: 'Quicksand'),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              m.displayName,
              style: TextStyle(
                fontSize: 12, 
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                color: isSelected ? const Color(0xFF271812) : const Color(0xFF625D59), 
                fontFamily: 'Quicksand'
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
