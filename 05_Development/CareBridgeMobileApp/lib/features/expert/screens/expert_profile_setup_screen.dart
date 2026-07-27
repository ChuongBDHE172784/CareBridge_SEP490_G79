import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class ExpertProfileSetupScreen extends StatefulWidget {
  const ExpertProfileSetupScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertProfileSetupScreen> createState() => _ExpertProfileSetupScreenState();
}

class _ExpertProfileSetupScreenState extends State<ExpertProfileSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _title = TextEditingController();
  final _experience = TextEditingController();
  final _scope = TextEditingController();
  List<ExpertMasterOption> _specialties = const [];
  List<ExpertMasterOption> _hospitals = const [];
  String? _specialtyId;
  String? _hospitalId;
  String? _trackAsiaName;
  String? _trackAsiaAddress;
  double? _trackAsiaLat;
  double? _trackAsiaLng;
  bool _loadingMasterData = true;
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadMasterData();
  }

  Future<void> _loadMasterData() async {
    try {
      final service = widget.service ?? ExpertOnboardingService.instance;
      final values = await Future.wait([
        service.loadSpecialties(),
        service.loadHospitals(),
      ]);
      if (!mounted) return;
      setState(() {
        _specialties = values[0];
        _hospitals = values[1];
        _loadingMasterData = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _loadingMasterData = false;
          _error = 'Không thể tải danh mục chuyên khoa và cơ sở y tế.';
        });
      }
    }
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
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await (widget.service ?? ExpertOnboardingService.instance).createProfile(
        specialtyId: _specialtyId!,
        professionalTitle: _title.text.trim(),
        experienceYears: int.parse(_experience.text.trim()),
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
      setState(() => _error = e.statusCode == 409
          ? 'Hồ sơ đã tồn tại. Đang chuyển sang bước tiếp theo…'
          : 'Không thể lưu hồ sơ. Vui lòng kiểm tra thông tin.');
      if (e.statusCode == 409 && mounted) context.go('/expert-onboarding');
    } catch (_) {
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _required(String? value) => (value ?? '').trim().isEmpty ? 'Thông tin này là bắt buộc' : null;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(title: const Text('Hồ sơ chuyên gia'), backgroundColor: Colors.transparent),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              const Text('Bước 1/3', style: TextStyle(color: Color(0xFFC06F5A), fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              const Text('Giới thiệu chuyên môn của bạn', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: Color(0xFF5A463F))),
              const SizedBox(height: 8),
              const Text('Thông tin này giúp hội đồng xác minh và hiển thị hồ sơ sau khi được duyệt.', style: TextStyle(height: 1.5, color: Color(0xFF75635C))),
              const SizedBox(height: 24),
              _masterDropdown(
                label: 'Chuyên khoa',
                value: _specialtyId,
                options: _specialties,
                onChanged: (value) => setState(() => _specialtyId = value),
              ),
              _field(_title, 'Chức danh chuyên môn', 'Ví dụ: Bác sĩ chuyên khoa I', validator: _required),
              _field(
                _experience,
                'Số năm kinh nghiệm',
                'Ví dụ: 8',
                keyboardType: TextInputType.number,
                validator: (value) {
                  final years = int.tryParse((value ?? '').trim());
                  return years == null || years < 0 || years > 80 ? 'Nhập số năm từ 0 đến 80' : null;
                },
              ),
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Autocomplete<Map<String, dynamic>>(
                  optionsBuilder: (TextEditingValue textEditingValue) async {
                    if (textEditingValue.text.trim().length < 2) {
                      return const Iterable<Map<String, dynamic>>.empty();
                    }
                    final service = widget.service ?? ExpertOnboardingService.instance;
                    try {
                      return await service.searchTrackAsiaHospitals(textEditingValue.text);
                    } catch (_) {
                      return const Iterable<Map<String, dynamic>>.empty();
                    }
                  },
                  displayStringForOption: (option) => option['name'] as String,
                  onSelected: (Map<String, dynamic> selection) {
                    setState(() {
                      _hospitalId = selection['id'] as String;
                      _trackAsiaName = selection['name'] as String?;
                      _trackAsiaAddress = selection['address'] as String?;
                      _trackAsiaLat = selection['lat'] as double?;
                      _trackAsiaLng = selection['lng'] as double?;
                    });
                  },
                  fieldViewBuilder: (BuildContext context, TextEditingController fieldTextEditingController, FocusNode fieldFocusNode, VoidCallback onFieldSubmitted) {
                    return TextFormField(
                      controller: fieldTextEditingController,
                      focusNode: fieldFocusNode,
                      decoration: InputDecoration(
                        labelText: 'Bệnh viện/Cơ sở y tế *',
                        hintText: 'Tìm bệnh viện hoặc phòng khám...',
                        filled: true,
                        fillColor: Colors.white,
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                      validator: (value) => _hospitalId == null ? 'Vui lòng chọn cơ sở từ danh sách tìm kiếm' : null,
                      onChanged: (val) {
                        if (_hospitalId != null) {
                          setState(() {
                            _hospitalId = null;
                            _trackAsiaName = null;
                            _trackAsiaAddress = null;
                            _trackAsiaLat = null;
                            _trackAsiaLng = null;
                          });
                        }
                      },
                    );
                  },
                  optionsViewBuilder: (BuildContext context, AutocompleteOnSelected<Map<String, dynamic>> onSelected, Iterable<Map<String, dynamic>> options) {
                    return Align(
                      alignment: Alignment.topLeft,
                      child: Material(
                        elevation: 4.0,
                        borderRadius: BorderRadius.circular(16),
                        child: SizedBox(
                          height: 200.0,
                          width: MediaQuery.of(context).size.width - 40,
                          child: ListView.builder(
                            padding: const EdgeInsets.all(8.0),
                            itemCount: options.length,
                            itemBuilder: (BuildContext context, int index) {
                              final option = options.elementAt(index);
                              return ListTile(
                                title: Text(option['name'] as String, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text(option['address'] as String? ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
                                onTap: () => onSelected(option),
                              );
                            },
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              _field(_scope, 'Phạm vi tư vấn', 'Mô tả chủ đề bạn có thể hỗ trợ', maxLines: 4, validator: _required),
              if (_error != null) ...[
                Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(color: const Color(0xFFFFDAD6), borderRadius: BorderRadius.circular(14)),
                  child: Text(_error!, style: const TextStyle(color: Color(0xFF93000A))),
                ),
              ],
              SizedBox(
                height: 54,
                child: FilledButton(
                  onPressed: _loading || _loadingMasterData ? null : _submit,
                  style: FilledButton.styleFrom(backgroundColor: const Color(0xFFC98C7B), shape: const StadiumBorder()),
                  child: _loading
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text('Lưu và tiếp tục', style: TextStyle(fontWeight: FontWeight.w700)),
                ),
              ),
            ],
          ),
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
          decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
          ),
        ),
      );

  Widget _masterDropdown({
    required String label,
    required String? value,
    required List<ExpertMasterOption> options,
    required ValueChanged<String?> onChanged,
  }) => Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: DropdownButtonFormField<String>(
          initialValue: value,
          isExpanded: true,
          items: options
              .map((option) => DropdownMenuItem(
                    value: option.id,
                    child: Text(option.name, overflow: TextOverflow.ellipsis),
                  ))
              .toList(),
          onChanged: _loadingMasterData ? null : onChanged,
          validator: (selected) => selected == null ? 'Thông tin này là bắt buộc' : null,
          decoration: InputDecoration(
            labelText: label,
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
          ),
        ),
      );
}
