import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class ExpertProfileSetupScreen extends StatefulWidget {
  const ExpertProfileSetupScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertProfileSetupScreen> createState() =>
      _ExpertProfileSetupScreenState();
}

class _ExpertProfileSetupScreenState extends State<ExpertProfileSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _title = TextEditingController();
  final _experience = TextEditingController();
  final _scope = TextEditingController();

  List<ExpertMasterOption> _specialties = const [];
  List<ExpertMasterOption> _provinces = const [];
  List<ExpertMasterOption> _districts = const [];
  List<ExpertMasterOption> _hospitals = const [];
  String? _specialtyId;
  String? _provinceId;
  String? _districtId;
  String? _hospitalId;
  String? _trackAsiaName;
  String? _trackAsiaAddress;
  double? _trackAsiaLat;
  double? _trackAsiaLng;
  bool _loadingMasterData = true;
  bool _loadingLocation = false;
  bool _loading = false;
  String? _error;

  ExpertOnboardingService get _service =>
      widget.service ?? ExpertOnboardingService.instance;

  @override
  void initState() {
    super.initState();
    _loadMasterData();
  }

  Future<void> _loadMasterData() async {
    try {
      final values = await Future.wait([
        _service.loadSpecialties(),
        _service.loadProvinces(),
      ]);
      if (!mounted) return;
      setState(() {
        _specialties = values[0];
        _provinces = values[1];
        _loadingMasterData = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loadingMasterData = false;
        _error = 'Không thể tải danh mục chuyên khoa và tỉnh/thành phố.';
      });
    }
  }

  Future<void> _selectProvince(String? provinceId) async {
    setState(() {
      _provinceId = provinceId;
      _districtId = null;
      _hospitalId = null;
      _districts = const [];
      _hospitals = const [];
      _clearTrackAsia();
      _error = null;
      _loadingLocation = provinceId != null;
    });
    if (provinceId == null) return;

    try {
      final values = await Future.wait([
        _service.loadDistricts(provinceId),
        _service.loadHospitals(provinceId: provinceId),
      ]);
      if (!mounted || _provinceId != provinceId) return;
      setState(() {
        _districts = values[0];
        _hospitals = values[1];
      });
    } catch (_) {
      if (mounted && _provinceId == provinceId) {
        setState(() {
          _error = 'Không thể tải cơ sở y tế tại khu vực đã chọn.';
        });
      }
    } finally {
      if (mounted && _provinceId == provinceId) {
        setState(() => _loadingLocation = false);
      }
    }
  }

  void _selectHospital(String? hospitalId) {
    setState(() {
      _hospitalId = hospitalId;
      _error = null;
      if (hospitalId != 'OTHER') _clearTrackAsia();
    });
  }

  void _clearTrackAsia() {
    _trackAsiaName = null;
    _trackAsiaAddress = null;
    _trackAsiaLat = null;
    _trackAsiaLng = null;
  }

  double? _asDouble(dynamic value) {
    if (value is num) return value.toDouble();
    return double.tryParse(value?.toString() ?? '');
  }

  @override
  void dispose() {
    _title.dispose();
    _experience.dispose();
    _scope.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_hospitalId == 'OTHER' && _trackAsiaName == null) {
      setState(() => _error = 'Vui lòng chọn cơ sở y tế từ kết quả tìm kiếm.');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final experienceText = _experience.text.trim();
      await _service.createProfile(
        specialtyId: _specialtyId!,
        professionalTitle: _title.text.trim(),
        experienceYears: experienceText.isEmpty
            ? null
            : int.parse(experienceText),
        hospitalId: _hospitalId!,
        trackAsiaName: _trackAsiaName,
        trackAsiaAddress: _trackAsiaAddress,
        trackAsiaLat: _trackAsiaLat,
        trackAsiaLng: _trackAsiaLng,
        consultationScope: _scope.text.trim(),
      );
      ExpertOnboardingStore.instance.invalidate();
      if (mounted) context.go('/expert/identity');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.statusCode == 409
            ? 'Hồ sơ đã tồn tại. Đang chuyển sang bước tiếp theo…'
            : 'Không thể lưu hồ sơ. Vui lòng kiểm tra thông tin.';
      });
      if (e.statusCode == 409 && mounted) context.go('/expert-onboarding');
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _required(String? value) =>
      (value ?? '').trim().isEmpty ? 'Thông tin này là bắt buộc' : null;

  String? _experienceValidator(String? value) {
    final text = (value ?? '').trim();
    if (text.isEmpty) return null;
    final years = int.tryParse(text);
    return years == null || years < 0 || years > 80
        ? 'Nhập số năm từ 0 đến 80'
        : null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(
        title: const Text('Hồ sơ chuyên gia'),
        backgroundColor: Colors.transparent,
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              const Text(
                'Bước 1/3',
                style: TextStyle(
                  color: Color(0xFFC06F5A),
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Giới thiệu chuyên môn của bạn',
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF5A463F),
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Thông tin này giúp hội đồng xác minh và hiển thị hồ sơ sau khi được duyệt.',
                style: TextStyle(height: 1.5, color: Color(0xFF75635C)),
              ),
              const SizedBox(height: 24),
              _masterDropdown(
                label: 'Chuyên khoa *',
                value: _specialtyId,
                options: _specialties,
                onChanged: (value) => setState(() => _specialtyId = value),
              ),
              _field(
                _title,
                'Chức danh chuyên môn *',
                'Ví dụ: Bác sĩ chuyên khoa I',
                validator: _required,
              ),
              _field(
                _experience,
                'Số năm kinh nghiệm',
                'Ví dụ: 8',
                keyboardType: TextInputType.number,
                validator: _experienceValidator,
              ),
              _field(
                _scope,
                'Phạm vi tư vấn',
                'Mô tả chủ đề bạn có thể hỗ trợ',
                maxLines: 4,
              ),
              const SizedBox(height: 4),
              const Text(
                'Địa điểm công tác',
                style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 12),
              _masterDropdown(
                label: 'Tỉnh/Thành phố *',
                value: _provinceId,
                options: _provinces,
                onChanged: _selectProvince,
              ),
              _masterDropdown(
                label: 'Quận/Huyện',
                value: _districtId,
                options: _districts,
                requiredField: false,
                enabled: _provinceId != null && !_loadingLocation,
                onChanged: (value) => setState(() => _districtId = value),
              ),
              _masterDropdown(
                label: 'Cơ sở y tế *',
                value: _hospitalId,
                options: [
                  ..._hospitals,
                  const ExpertMasterOption(id: 'OTHER', name: 'Khác'),
                ],
                enabled: _provinceId != null && !_loadingLocation,
                onChanged: _selectHospital,
              ),
              if (_loadingLocation)
                const Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: LinearProgressIndicator(),
                ),
              if (_hospitalId == 'OTHER') _trackAsiaPicker(),
              if (_error != null) ...[
                Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFDAD6),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Text(
                    _error!,
                    style: const TextStyle(color: Color(0xFF93000A)),
                  ),
                ),
              ],
              SizedBox(
                height: 54,
                child: FilledButton(
                  onPressed: _loading || _loadingMasterData || _loadingLocation
                      ? null
                      : _submit,
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    shape: const StadiumBorder(),
                  ),
                  child: _loading
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text(
                          'Lưu và tiếp tục',
                          style: TextStyle(fontWeight: FontWeight.w700),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _trackAsiaPicker() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: const Color(0xFFEFF6FF),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFBFDBFE)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Tìm cơ sở y tế trên bản đồ',
              style: TextStyle(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 10),
            Autocomplete<Map<String, dynamic>>(
              optionsBuilder: (value) async {
                final query = value.text.trim();
                if (query.length < 2) {
                  return const Iterable<Map<String, dynamic>>.empty();
                }
                try {
                  return await _service.searchTrackAsiaHospitals(query);
                } catch (_) {
                  return const Iterable<Map<String, dynamic>>.empty();
                }
              },
              displayStringForOption: (option) =>
                  option['name']?.toString() ?? '',
              onSelected: (selection) {
                setState(() {
                  _trackAsiaName = selection['name']?.toString();
                  _trackAsiaAddress = selection['address']?.toString();
                  _trackAsiaLat = _asDouble(selection['lat']);
                  _trackAsiaLng = _asDouble(selection['lng']);
                  _error = null;
                });
              },
              fieldViewBuilder: (context, controller, focusNode, onSubmitted) {
                return TextFormField(
                  controller: controller,
                  focusNode: focusNode,
                  decoration: _decoration(
                    'Tên bệnh viện/phòng khám *',
                    hint: 'Nhập ít nhất 2 ký tự',
                  ),
                  validator: (_) => _trackAsiaName == null
                      ? 'Vui lòng chọn cơ sở từ danh sách tìm kiếm'
                      : null,
                  onChanged: (_) {
                    if (_trackAsiaName != null) {
                      setState(_clearTrackAsia);
                    }
                  },
                );
              },
              optionsViewBuilder: (context, onSelected, options) => Align(
                alignment: Alignment.topLeft,
                child: Material(
                  elevation: 6,
                  borderRadius: BorderRadius.circular(14),
                  child: SizedBox(
                    width: MediaQuery.of(context).size.width - 68,
                    height: 220,
                    child: ListView.builder(
                      padding: const EdgeInsets.all(6),
                      itemCount: options.length,
                      itemBuilder: (context, index) {
                        final option = options.elementAt(index);
                        return ListTile(
                          title: Text(option['name']?.toString() ?? ''),
                          subtitle: Text(
                            option['address']?.toString() ?? '',
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          onTap: () => onSelected(option),
                        );
                      },
                    ),
                  ),
                ),
              ),
            ),
            if (_trackAsiaName != null) ...[
              const SizedBox(height: 10),
              Text(
                _trackAsiaName!,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              if ((_trackAsiaAddress ?? '').isNotEmpty)
                Text(
                  _trackAsiaAddress!,
                  style: const TextStyle(color: Color(0xFF75635C)),
                ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController controller,
    String label,
    String hint, {
    TextInputType? keyboardType,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) => Padding(
    padding: const EdgeInsets.only(bottom: 16),
    child: TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      maxLines: maxLines,
      validator: validator,
      decoration: _decoration(label, hint: hint),
    ),
  );

  Widget _masterDropdown({
    required String label,
    required String? value,
    required List<ExpertMasterOption> options,
    required ValueChanged<String?> onChanged,
    bool requiredField = true,
    bool enabled = true,
  }) => Padding(
    padding: const EdgeInsets.only(bottom: 16),
    child: DropdownButtonFormField<String>(
      key: ValueKey('$label-$value'),
      initialValue: value,
      isExpanded: true,
      items: options
          .map(
            (option) => DropdownMenuItem(
              value: option.id,
              child: Text(option.name, overflow: TextOverflow.ellipsis),
            ),
          )
          .toList(),
      onChanged: enabled && !_loadingMasterData ? onChanged : null,
      validator: requiredField
          ? (selected) => selected == null ? 'Thông tin này là bắt buộc' : null
          : null,
      decoration: _decoration(label),
    ),
  );

  InputDecoration _decoration(String label, {String? hint}) => InputDecoration(
    labelText: label,
    hintText: hint,
    filled: true,
    fillColor: Colors.white,
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
  );
}
