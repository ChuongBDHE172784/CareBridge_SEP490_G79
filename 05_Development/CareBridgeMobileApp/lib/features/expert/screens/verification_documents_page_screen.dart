import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mime/mime.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/api_client.dart';

class VerificationDocumentsPageScreen extends StatefulWidget {
  const VerificationDocumentsPageScreen({super.key});

  @override
  State<VerificationDocumentsPageScreen> createState() =>
      _VerificationDocumentsPageScreenState();
}

class _VerificationDocumentsPageScreenState
    extends State<VerificationDocumentsPageScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  static const _issuers = [
    'Bộ Y tế',
    'Sở Y tế Hà Nội',
    'Sở Y tế TP Hồ Chí Minh',
    'Sở Y tế Đà Nẵng',
    'Sở Y tế khác',
    'Hội đồng Y khoa Việt Nam',
    'Trường Đại học Y Hà Nội',
    'Trường Đại học Y dược TP Hồ Chí Minh',
    'Trường Đại học Y dược Huế',
    'Trường Đại học Y dược Cần Thơ',
    'Học viện Y học cổ truyền Việt Nam',
    'Bộ Giáo dục và Đào tạo',
    'Trường Đại học khác',
    'Cơ quan đăng ký hành nghề y tế',
    'UBND tỉnh / thành phố',
    'Hội nghề nghiệp y tế',
    'Tổ chức y tế quốc tế',
    'Khác',
  ];

  static const _typeLabels = {
    'MEDICAL_LICENSE': 'Giấy phép hành nghề y',
    'DEGREE': 'Bằng cấp chuyên môn',
    'CERTIFICATE': 'Chứng chỉ đào tạo',
    'IDENTITY_DOCUMENT': 'Giấy tờ định danh',
    'PROFESSIONAL_LICENSE': 'Giấy phép hành nghề',
  };

  List<Map<String, dynamic>> _credentials = [];
  bool _loading = true;
  bool _showUploadForm = false;
  bool _submitting = false;
  String? _deletingId;
  String? _errorMsg;

  // Form State
  String? _selectedType;
  final _numberCtrl = TextEditingController();
  String? _selectedIssuer;
  final _customIssuerCtrl = TextEditingController();
  final _issuedDateCtrl = TextEditingController();
  final _expiryDateCtrl = TextEditingController();
  PlatformFile? _selectedFile;

  @override
  void initState() {
    super.initState();
    _loadCredentials();
  }

  @override
  void dispose() {
    _numberCtrl.dispose();
    _customIssuerCtrl.dispose();
    _issuedDateCtrl.dispose();
    _expiryDateCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadCredentials() async {
    setState(() => _loading = true);
    try {
      final res = await apiGet('/api/v1/expert/credentials/me');
      final list = (res['data'] as List? ?? [])
          .map((e) => e as Map<String, dynamic>)
          .toList();
      if (mounted) {
        setState(() {
          _credentials = list;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _errorMsg = 'Không thể tải danh sách chứng chỉ';
        });
      }
    }
  }

  Future<void> _pickFile() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['pdf', 'jpg', 'jpeg', 'png'],
        withData: true,
      );
      if (result != null && result.files.isNotEmpty) {
        setState(() => _selectedFile = result.files.first);
      }
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể chọn tệp. Vui lòng thử lại.')),
      );
    }
  }

  Future<void> _selectDate(TextEditingController controller) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(1970),
      lastDate: DateTime(2040),
    );
    if (picked != null) {
      controller.text = DateFormat('yyyy-MM-dd').format(picked);
    }
  }

  Future<void> _submitUpload() async {
    if (_selectedType == null ||
        _issuedDateCtrl.text.trim().isEmpty ||
        _selectedFile == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Vui lòng chọn loại chứng chỉ, ngày cấp và chọn tệp đính kèm',
          ),
        ),
      );
      return;
    }

    setState(() {
      _submitting = true;
      _errorMsg = null;
    });

    try {
      final bytes = _selectedFile!.bytes;
      if (bytes == null) throw Exception('Tệp không hợp lệ');

      final issuerVal = _selectedIssuer == 'Khác' &&
              _customIssuerCtrl.text.trim().isNotEmpty
          ? _customIssuerCtrl.text.trim()
          : (_selectedIssuer ?? '');

      final fields = <String, String>{
        'credentialType': _selectedType!,
        'issuedDate': _issuedDateCtrl.text.trim(),
      };
      if (_numberCtrl.text.trim().isNotEmpty) {
        fields['credentialNumber'] = _numberCtrl.text.trim();
      }
      if (issuerVal.isNotEmpty) {
        fields['issuer'] = issuerVal;
      }
      if (_expiryDateCtrl.text.trim().isNotEmpty) {
        fields['expiryDate'] = _expiryDateCtrl.text.trim();
      }

      final mime = lookupMimeType(_selectedFile!.name) ?? 'application/octet-stream';
      await apiMultipart(
        '/api/v1/expert/credentials',
        fields,
        files: [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: bytes,
            fileName: _selectedFile!.name,
            mimeType: mime,
          ),
        ],
      );

      if (mounted) {
        setState(() {
          _submitting = false;
          _showUploadForm = false;
          _resetForm();
        });
        _loadCredentials();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _submitting = false;
          _errorMsg = 'Tải lên chứng chỉ thất bại. Vui lòng thử lại.';
        });
      }
    }
  }

  void _resetForm() {
    _selectedType = null;
    _numberCtrl.clear();
    _selectedIssuer = null;
    _customIssuerCtrl.clear();
    _issuedDateCtrl.clear();
    _expiryDateCtrl.clear();
    _selectedFile = null;
  }

  Future<void> _deleteCredential(String id) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: const Text('Bạn có chắc muốn xóa chứng chỉ này?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Xóa', style: TextStyle(color: _error)),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    setState(() => _deletingId = id);
    try {
      await apiDelete('/api/v1/expert/credentials/$id');
      if (mounted) {
        setState(() {
          _credentials.removeWhere((c) => c['credentialId'] == id);
          _deletingId = null;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _deletingId = null);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể xóa chứng chỉ. Vui lòng thử lại.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      appBar: AppBar(
        backgroundColor: _bgColor,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chứng chỉ & Giấy tờ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
        centerTitle: true,
      ),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          : SafeArea(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 32),
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Danh sách tài liệu xác minh',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _outline,
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed: () {
                          setState(() {
                            _showUploadForm = !_showUploadForm;
                            if (!_showUploadForm) _resetForm();
                          });
                        },
                        icon: Icon(
                          _showUploadForm ? Icons.close : Icons.add,
                          size: 18,
                        ),
                        label: Text(
                          _showUploadForm ? 'Đóng' : 'Tải lên mới',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _primary,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  if (_errorMsg != null) ...[
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: _errorContainer,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Text(
                        _errorMsg!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _error,
                          fontSize: 13,
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],

                  // Upload Form
                  if (_showUploadForm) ...[
                    Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: _surface,
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: const [
                          BoxShadow(
                            color: Color.fromRGBO(90, 70, 63, 0.08),
                            blurRadius: 20,
                            offset: Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.upload_file, color: _primary, size: 22),
                              SizedBox(width: 8),
                              Text(
                                'Tải lên chứng chỉ mới',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 16,
                                  fontWeight: FontWeight.w700,
                                  color: _onSurface,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          _buildLabel('Loại chứng chỉ', required: true),
                          DropdownButtonFormField<String>(
                            initialValue: _selectedType,
                            decoration: _inputDecoration(
                              hint: '-- Chọn loại chứng chỉ --',
                            ),
                            items: _typeLabels.entries
                                .map(
                                  (e) => DropdownMenuItem(
                                    value: e.key,
                                    child: Text(
                                      e.value,
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 14,
                                      ),
                                    ),
                                  ),
                                )
                                .toList(),
                            onChanged: (v) => setState(() => _selectedType = v),
                          ),
                          const SizedBox(height: 14),

                          _buildLabel('Số chứng chỉ / Số hiệu'),
                          TextField(
                            controller: _numberCtrl,
                            decoration: _inputDecoration(
                              hint: 'VD: 012345/BYT-CCHN...',
                            ),
                          ),
                          const SizedBox(height: 14),

                          _buildLabel('Đơn vị / Nơi cấp'),
                          DropdownButtonFormField<String>(
                            initialValue: _selectedIssuer,
                            decoration:
                                _inputDecoration(hint: '-- Chọn nơi cấp --'),
                            items: _issuers
                                .map(
                                  (s) => DropdownMenuItem(
                                    value: s,
                                    child: Text(
                                      s,
                                      style: const TextStyle(
                                        fontFamily: 'Lexend',
                                        fontSize: 14,
                                      ),
                                    ),
                                  ),
                                )
                                .toList(),
                            onChanged: (v) =>
                                setState(() => _selectedIssuer = v),
                          ),
                          if (_selectedIssuer == 'Khác') ...[
                            const SizedBox(height: 8),
                            TextField(
                              controller: _customIssuerCtrl,
                              decoration: _inputDecoration(
                                hint: 'Nhập tên cơ quan cấp...',
                              ),
                            ),
                          ],
                          const SizedBox(height: 14),

                          Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    _buildLabel('Ngày cấp', required: true),
                                    TextField(
                                      controller: _issuedDateCtrl,
                                      readOnly: true,
                                      onTap: () => _selectDate(_issuedDateCtrl),
                                      decoration: _inputDecoration(
                                        hint: 'YYYY-MM-DD',
                                        suffixIcon: const Icon(
                                          Icons.calendar_today,
                                          size: 18,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    _buildLabel('Hết hạn (nếu có)'),
                                    TextField(
                                      controller: _expiryDateCtrl,
                                      readOnly: true,
                                      onTap: () => _selectDate(_expiryDateCtrl),
                                      decoration: _inputDecoration(
                                        hint: 'YYYY-MM-DD',
                                        suffixIcon: const Icon(
                                          Icons.calendar_today,
                                          size: 18,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),

                          _buildLabel('Tệp đính kèm (PDF, JPG, PNG)', required: true),
                          OutlinedButton.icon(
                            onPressed: _pickFile,
                            icon: const Icon(Icons.attach_file, size: 18),
                            label: Text(
                              _selectedFile != null
                                  ? _selectedFile!.name
                                  : 'Chọn tệp đính kèm...',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: _primary,
                              side: const BorderSide(color: _outlineVariant),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 12,
                              ),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                            ),
                          ),
                          const SizedBox(height: 20),

                          Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              TextButton(
                                onPressed: () {
                                  setState(() {
                                    _showUploadForm = false;
                                    _resetForm();
                                  });
                                },
                                child: const Text(
                                  'Hủy',
                                  style: TextStyle(color: _outline),
                                ),
                              ),
                              const SizedBox(width: 12),
                              ElevatedButton(
                                onPressed: _submitting ? null : _submitUpload,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: _primary,
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(20),
                                  ),
                                ),
                                child: _submitting
                                    ? const SizedBox(
                                        width: 16,
                                        height: 16,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          color: Colors.white,
                                        ),
                                      )
                                    : const Text('Gửi xét duyệt'),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],

                  // Credentials List
                  if (_credentials.isEmpty && !_showUploadForm) ...[
                    Container(
                      padding: const EdgeInsets.all(32),
                      decoration: BoxDecoration(
                        color: _surface,
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: const [
                          BoxShadow(
                            color: Color.fromRGBO(90, 70, 63, 0.06),
                            blurRadius: 20,
                            offset: Offset(0, 4),
                          ),
                        ],
                      ),
                      child: const Column(
                        children: [
                          Icon(
                            Icons.description_outlined,
                            size: 48,
                            color: _outlineVariant,
                          ),
                          SizedBox(height: 12),
                          Text(
                            'Chưa có chứng chỉ nào được tải lên',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color: _outline,
                            ),
                          ),
                          SizedBox(height: 4),
                          Text(
                            'Nhấn "Tải lên mới" để gửi bằng cấp hoặc chứng chỉ hành nghề.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              color: _outlineVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ] else ...[
                    ..._credentials.map((cred) => _buildCredentialCard(cred)),
                  ],
                ],
              ),
            ),
    );
  }

  Widget _buildCredentialCard(Map<String, dynamic> cred) {
    final id = cred['credentialId'] as String? ?? '';
    final type = cred['credentialType'] as String? ?? '';
    final typeLabel = _typeLabels[type] ?? type;
    final number = cred['credentialNumber'] as String? ?? '';
    final issuer = cred['issuer'] as String? ?? '';
    final issuedDate = cred['issuedDate'] as String? ?? '';
    final expiryDate = cred['expiryDate'] as String?;
    final status = cred['reviewStatus'] as String? ?? 'PENDING';
    final note = cred['reviewNote'] as String?;
    final fileUrl = cred['fileUrl'] as String?;

    final isApproved = status == 'APPROVED';
    final isRejected = status == 'REJECTED';
    final statusLabel = isApproved
        ? 'Đã duyệt'
        : isRejected
            ? 'Bị từ chối'
            : 'Chờ xét duyệt';
    final statusBg = isApproved
        ? const Color(0xFFE6F4EA)
        : isRejected
            ? _errorContainer
            : const Color(0xFFFFF3E0);
    final statusFg = isApproved
        ? const Color(0xFF137333)
        : isRejected
            ? _error
            : const Color(0xFFE65100);

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 18,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 20,
                backgroundColor: _surfaceLow,
                child: Icon(
                  type == 'DEGREE' ? Icons.school_outlined : Icons.description_outlined,
                  color: _primary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            typeLabel,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: _onSurface,
                            ),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: statusBg,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            statusLabel,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              color: statusFg,
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (number.isNotEmpty) ...[
                      const SizedBox(height: 3),
                      Text(
                        'Số hiệu: $number',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: _outline,
                        ),
                      ),
                    ],
                    if (issuer.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Text(
                        'Nơi cấp: $issuer',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: _outline,
                        ),
                      ),
                    ],
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Text(
                          'Ngày cấp: $issuedDate',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _outlineVariant,
                          ),
                        ),
                        if (expiryDate != null && expiryDate.isNotEmpty) ...[
                          const SizedBox(width: 8),
                          Text(
                            '• Hết hạn: $expiryDate',
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 12,
                              color: _outlineVariant,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (note != null && note.isNotEmpty) ...[
            const SizedBox(height: 10),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: _errorContainer.withValues(alpha: 0.5),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                'Phản hồi: $note',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _error,
                ),
              ),
            ),
          ],
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (fileUrl != null && fileUrl.isNotEmpty) ...[
                OutlinedButton.icon(
                  onPressed: () async {
                    final uri = Uri.tryParse(fileUrl);
                    if (uri != null) {
                      await launchUrl(uri, mode: LaunchMode.externalApplication);
                    }
                  },
                  icon: const Icon(Icons.visibility_outlined, size: 16),
                  label: const Text('Xem file'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _primary,
                    side: const BorderSide(color: _outlineVariant),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 8,
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
              ],
              IconButton(
                onPressed: _deletingId == id ? null : () => _deleteCredential(id),
                icon: _deletingId == id
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.delete_outline, color: _error, size: 20),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildLabel(String text, {bool required = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: RichText(
        text: TextSpan(
          text: text,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: _outline,
          ),
          children: [
            if (required)
              const TextSpan(
                text: ' *',
                style: TextStyle(color: _error),
              ),
          ],
        ),
      ),
    );
  }

  InputDecoration _inputDecoration({required String hint, Widget? suffixIcon}) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        color: _outlineVariant,
      ),
      suffixIcon: suffixIcon,
      contentPadding: const EdgeInsets.symmetric(
        horizontal: 16,
        vertical: 14,
      ),
      filled: true,
      fillColor: _surface,
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _primary, width: 1.5),
      ),
    );
  }
}
