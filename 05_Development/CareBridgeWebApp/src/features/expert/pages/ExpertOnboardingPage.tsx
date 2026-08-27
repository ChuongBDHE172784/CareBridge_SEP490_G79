import { useEffect, useRef, useState } from 'react';
import { ContractStep, ExpertTypeStep } from '../components/ExpertTwoTierSteps';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowLeft, ArrowRight, BadgeCheck, Camera, Check, FileBadge, RefreshCw, ShieldCheck } from 'lucide-react';
import {
  createMyProfile,
  getExpertOnboarding,
  submitCredential,
  submitIdentityEvidence,
  verifyFace,
  getProvinces,
  getDistricts,
  getWards,
  getSpecialties,
  getHospitals,
  searchTrackAsiaHospitals,
} from '../services/expertApi';
import type {
  ExpertOnboardingResponse,
  ExpertOnboardingStep,
  ProvinceResponse,
  DistrictResponse,
  WardResponse,
  SpecialtyResponse,
  HospitalResponse,
} from '../services/expertApi';

const IMAGE_LIMIT = 5 * 1024 * 1024;

type ImageSlot = 'selfie' | 'identityFront' | 'identityBack';
type CapturedImage = { file: File; preview: string };

const steps = [
  ['EXPERT_TYPE', 'Hình thức'],
  ['IDENTITY', 'Định danh'],
  ['CREDENTIAL', 'Chứng chỉ'],
  ['UNDER_REVIEW', 'Xét duyệt'],
  ['CONTRACT', 'Ký thoả thuận'],
] as const;

/** Bước ký thoả thuận chỉ áp dụng cho Chuyên gia Hệ thống. */
function visibleStepsFor(expertType: string | null | undefined) {
  return steps.filter(
    ([step]) => step !== 'CONTRACT'
      || expertType === 'PENDING_CONTRACT'
      || expertType === 'CONTRACTED',
  );
}

export default function ExpertOnboardingPage() {
  const navigate = useNavigate();
  const [state, setState] = useState<ExpertOnboardingResponse | null>(null);
  const [overrideStep, setOverrideStep] = useState<ExpertOnboardingStep | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = async () => {
    setError(null);
    setOverrideStep(null); 
    try {
      const next = await getExpertOnboarding();
      setState(next);
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tải tiến độ đăng ký.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void reload(); }, []);

  if (loading) return <Loading />;

  const currentStep = overrideStep || state?.nextStep;

  // CONTRACT chỉ thuộc luồng Chuyên gia Hệ thống, nên danh sách bước hiển thị
  // phải lọc trước khi đánh số — nếu không, số thứ tự sẽ nhảy cóc.
  const visibleSteps = visibleStepsFor(state?.expertType);
  // serverIndex là tiến độ THẬT do máy chủ giữ; viewIndex là bước đang xem.
  // Tách hai thứ này ra để người dùng lùi lại xem bước cũ mà không bị coi là
  // tụt tiến độ, và quan trọng hơn: mọi bước sau serverIndex vẫn khoá, không
  // thể nhảy qua khâu định danh rồi gửi hồ sơ.
  const serverIndex = Math.max(0, visibleSteps.findIndex(([step]) => step === state?.nextStep));
  const viewIndex = Math.max(0, visibleSteps.findIndex(([step]) => step === currentStep));
  const isRevisiting = overrideStep !== null && viewIndex < serverIndex;
  const goToStep = (index: number) => setOverrideStep(visibleSteps[index][0]);

  return (
    <div className="p-8 font-sans space-y-8 max-w-5xl mx-auto">
      {/* Header Banner */}
      <header className="rounded-3xl bg-surface border border-outline-variant/60 p-7 text-on-surface shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="material-symbols-outlined text-primary text-xl">medical_services</span>
            <span className="text-xs font-bold text-primary uppercase tracking-wider">Cổng Chuyên gia CareBridge</span>
          </div>
          <h1 className="text-[26px] font-bold text-on-surface m-0 leading-tight">Hoàn tất xác minh hồ sơ chuyên gia</h1>
          <p className="mt-2 text-sm text-on-surface-variant max-w-2xl leading-relaxed m-0">
            Quy trình tuân thủ chuẩn Bộ Y Tế. Vui lòng cung cấp thông tin chính xác và ảnh rõ nét để được phê duyệt nhanh nhất.
          </p>
        </div>
        <div className="shrink-0 flex items-center gap-3 self-start md:self-auto">
          <span className="px-3.5 py-1.5 rounded-full text-xs font-bold border bg-primary-container/40 text-primary border-primary/30 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
            Xác minh Y tế
          </span>
        </div>
      </header>

      {/* Stepper — bước đã qua bấm được để xem và sửa lại */}
      <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        {visibleSteps.map(([key, label], index) => {
          const complete = currentStep === 'COMPLETE' || index < serverIndex;
          const active = index === viewIndex && currentStep !== 'COMPLETE';
          // Chỉ mở tới đúng mốc máy chủ cho phép; bước phía trước vẫn khoá.
          const reachable = index <= serverIndex && currentStep !== 'COMPLETE';
          return (
            <button
              key={key}
              type="button"
              disabled={!reachable}
              aria-current={active ? 'step' : undefined}
              onClick={() => goToStep(index)}
              className={`w-full text-left rounded-2xl border p-4 flex items-center gap-3.5 transition-all shadow-xs ${
                active
                  ? 'border-primary bg-primary-container/30 text-primary font-bold shadow-sm'
                  : complete
                  ? 'border-emerald-300 bg-emerald-50/60 text-emerald-900'
                  : 'border-outline-variant/50 bg-surface text-outline'
              } ${reachable ? 'cursor-pointer hover:shadow-sm' : 'cursor-not-allowed'}`}
            >
              <div
                className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                  active
                    ? 'bg-primary text-on-primary shadow-xs'
                    : complete
                    ? 'bg-emerald-600 text-white shadow-xs'
                    : 'bg-surface-container-highest text-outline'
                }`}
              >
                {complete && !active ? <Check size={18} /> : index + 1}
              </div>
              <div>
                <p className="text-[11px] text-outline font-semibold uppercase tracking-wider m-0">Bước {index + 1}</p>
                <p className="text-sm font-bold m-0">{label}</p>
              </div>
            </button>
          );
        })}
      </div>

      {/* Điều hướng lùi. Tách riêng khỏi stepper vì trên màn hình hẹp stepper
          xếp thành nhiều dòng, nút bấm nằm lẫn trong đó sẽ khó thấy. */}
      {currentStep !== 'COMPLETE' && (viewIndex > 0 || isRevisiting) && (
        <div className="flex flex-wrap items-center gap-3">
          {viewIndex > 0 && (
            <button
              type="button"
              onClick={() => goToStep(viewIndex - 1)}
              className="inline-flex items-center gap-2 rounded-full border border-outline-variant px-5 py-2.5 text-sm font-semibold text-on-surface-variant transition-colors hover:bg-surface-variant"
            >
              <ArrowLeft size={16} />
              Quay lại bước {viewIndex}: {visibleSteps[viewIndex - 1][1]}
            </button>
          )}
          {isRevisiting && (
            <button
              type="button"
              onClick={() => setOverrideStep(null)}
              className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-on-primary-fixed-variant"
            >
              Về bước hiện tại: {visibleSteps[serverIndex]?.[1]}
              <ArrowRight size={16} />
            </button>
          )}
        </div>
      )}

      {error && <ErrorBanner message={error} retry={reload} />}
      {currentStep === 'EXPERT_TYPE' && <ExpertTypeStep onDone={reload} currentType={state?.expertType} />}
      {currentStep === 'CONTRACT' && <ContractStep onDone={reload} />}
      {(currentStep === 'PROFILE' || currentStep === 'IDENTITY') && (
        <IdentityStep 
          needsProfile={!state?.profileExists}
          onDone={reload} 
          latestReason={
            (state?.rejectionReason ?? state?.latestIdentityAttempt?.reviewReason)?.includes('Pipeline processing failed') 
              ? 'Ảnh chụp của bạn chưa đạt yêu cầu hoặc khuôn mặt không khớp. Vui lòng chụp lại ảnh rõ nét, giữ giấy tờ thẳng đứng.' 
              : (state?.rejectionReason ?? state?.latestIdentityAttempt?.reviewReason)
          } 
        />
      )}
      {currentStep === 'CREDENTIAL' && <CredentialStep onDone={reload} />}
      {currentStep === 'UNDER_REVIEW' && state && <ReviewStep state={state} reload={reload} setOverrideStep={setOverrideStep} />}
      {currentStep === 'COMPLETE' && (
        <section className="rounded-3xl border border-emerald-300 bg-emerald-50/80 p-8 text-center shadow-md">
          <BadgeCheck className="mx-auto text-emerald-600" size={60} />
          <h2 className="mt-4 text-2xl font-bold text-emerald-950">Hồ sơ đã được xác minh</h2>
          <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-emerald-800">
            Chúc mừng! Bạn đã hoàn tất toàn bộ quy trình xác minh và có thể sử dụng đầy đủ cổng Chuyên gia CareBridge.
          </p>
          <button
            className="mt-6 rounded-full bg-emerald-700 hover:bg-emerald-800 px-8 py-3.5 font-bold text-white text-sm cursor-pointer shadow-md transition-all"
            onClick={() => navigate('/expert/dashboard', { replace: true })}
          >
            Vào Trang chủ Chuyên gia
          </button>
        </section>
      )}
    </div>
  );
}

function IdentityStep({ onDone, latestReason, needsProfile }: { onDone: () => Promise<void>; latestReason?: string | null; needsProfile?: boolean }) {
  const [images, setImages] = useState<Partial<Record<ImageSlot, CapturedImage>>>({});
  const [activeCamera, setActiveCamera] = useState<ImageSlot | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [aiResult, setAiResult] = useState<{ similar: boolean; similarity: number; reason?: string | null } | null>(null);
  const imageUrls = useRef(new Set<string>());

  const [form, setForm] = useState({
    specialtyId: '',
    professionalTitle: '',
    experienceYears: '',
    provinceId: '',
    districtId: '',
    wardId: '',
    hospitalId: '',
    trackAsiaName: '',
    trackAsiaAddress: '',
    trackAsiaLat: undefined as number | undefined,
    trackAsiaLng: undefined as number | undefined,
    consultationScope: '',
  });
  const [options, setOptions] = useState({
    provinces: [] as ProvinceResponse[],
    districts: [] as DistrictResponse[],
    wards: [] as WardResponse[],
    specialties: [] as SpecialtyResponse[],
    hospitals: [] as HospitalResponse[],
  });
  const [trackAsiaQuery, setTrackAsiaQuery] = useState('');
  const [trackAsiaResults, setTrackAsiaResults] = useState<any[]>([]);
  const [searchingHospitals, setSearchingHospitals] = useState(false);

  useEffect(() => {
    if (!needsProfile) return;
    const loadMasterData = async () => {
      try {
        const [p, s] = await Promise.all([getProvinces(), getSpecialties()]);
        setOptions(prev => ({ ...prev, provinces: p, specialties: s }));
      } catch {
        setError('Không thể tải dữ liệu danh mục.');
      }
    };
    void loadMasterData();
  }, [needsProfile]);

  useEffect(() => {
    if (!needsProfile) return;
    let active = true;
    if (!form.provinceId) {
      setOptions(prev => ({ ...prev, districts: [] }));
      return () => { active = false; };
    }
    const requestedProvince = form.provinceId;
    setOptions(prev => ({ ...prev, districts: [] }));
    getDistricts(requestedProvince)
      .then(districts => { if (active) setOptions(prev => ({ ...prev, districts })); })
      .catch(() => { if (active) setError('Không thể tải danh sách quận/huyện.'); });
    return () => { active = false; };
  }, [needsProfile, form.provinceId]);

  useEffect(() => {
    if (!needsProfile) return;
    let active = true;
    if (!form.provinceId) {
      setOptions(prev => ({ ...prev, wards: [], hospitals: [] }));
      return () => { active = false; };
    }
    const requestedProvince = form.provinceId;
    setOptions(prev => ({ ...prev, wards: [], hospitals: [] }));
    getWards({ provinceId: requestedProvince })
      .then(wards => { if (active) setOptions(prev => ({ ...prev, wards })); })
      .catch(() => { if (active) setError('Không thể tải danh sách phường/xã.'); });
    getHospitals({ provinceId: requestedProvince })
      .then(hospitals => { if (active) setOptions(prev => ({ ...prev, hospitals })); })
      .catch(() => { if (active) setError('Không thể tải danh sách cơ sở y tế.'); });
    return () => { active = false; };
  }, [needsProfile, form.provinceId]);

  useEffect(() => {
    if (!needsProfile || trackAsiaQuery.trim().length < 2) { setTrackAsiaResults([]); return; }
    const timer = setTimeout(() => {
      setSearchingHospitals(true);
      searchTrackAsiaHospitals(trackAsiaQuery)
        .then(res => setTrackAsiaResults(res || []))
        .catch(() => setTrackAsiaResults([]))
        .finally(() => setSearchingHospitals(false));
    }, 500);
    return () => clearTimeout(timer);
  }, [needsProfile, trackAsiaQuery]);

  const update =
    (key: keyof typeof form) =>
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      const val = event.target.value;
      setForm(prev => {
        const next = { ...prev, [key]: val };
        if (key === 'provinceId') { next.districtId = ''; next.wardId = ''; next.hospitalId = ''; }
        if (key === 'districtId') { next.wardId = ''; next.hospitalId = ''; }
        return next;
      });
    };

  useEffect(() => () => imageUrls.current.forEach(url => URL.revokeObjectURL(url)), []);

  const select = async (slot: ImageSlot, file: File) => {
    if (!['image/jpeg', 'image/png'].includes(file.type)) { setError('Chỉ chấp nhận ảnh JPEG hoặc PNG.'); return; }
    if (file.size > IMAGE_LIMIT) { setError('Mỗi ảnh phải có dung lượng tối đa 5 MB.'); return; }
    setAiResult(null);
    setImages(current => {
      const previous = current[slot];
      if (previous) { URL.revokeObjectURL(previous.preview); imageUrls.current.delete(previous.preview); }
      const preview = URL.createObjectURL(file);
      imageUrls.current.add(preview);
      return { ...current, [slot]: { file, preview } };
    });
    setError(null);
    setActiveCamera(null);
  };

  useEffect(() => {
    const runVerify = async () => {
      if (images.selfie && images.identityFront) {
        try {
          const res = await verifyFace({ selfie: images.selfie.file, idCard: images.identityFront.file });
          setAiResult({ similar: res.similar, similarity: res.similarity, reason: res.reason });
        } catch (e) { console.error('AI Verify failed', e); }
      }
    };
    void runVerify();
  }, [images.selfie, images.identityFront]);

  const submit = async () => {
    if (needsProfile) {
      if (!form.specialtyId || !form.professionalTitle || !form.provinceId || !form.hospitalId) {
        setError('Vui lòng điền đầy đủ thông tin chuyên môn bắt buộc.');
        return;
      }
    }
    if (!images.selfie || !images.identityFront || !images.identityBack) {
      setError('Vui lòng chụp đầy đủ 3 ảnh.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (needsProfile) {
        await createMyProfile({
          specialtyId: form.specialtyId,
          professionalTitle: form.professionalTitle.trim(),
          hospitalId: form.hospitalId,
          trackAsiaName: form.trackAsiaName,
          trackAsiaAddress: form.trackAsiaAddress,
          trackAsiaLat: form.trackAsiaLat,
          trackAsiaLng: form.trackAsiaLng,
          consultationScope: form.consultationScope.trim(),
          experienceYears: form.experienceYears ? Number(form.experienceYears) : undefined,
        });
      }
      await submitIdentityEvidence({
        selfie: images.selfie.file,
        identityFront: images.identityFront.file,
        identityBack: images.identityBack.file,
      });
      await onDone();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Lỗi hệ thống khi gửi dữ liệu.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StepCard
      icon={<Camera />}
      title="Hồ sơ & Định danh"
      description="Cung cấp thông tin chuyên môn và bộ ảnh định danh (Chân dung & CCCD) để xác minh danh tính."
    >
      {latestReason && <div className="mb-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Yêu cầu bổ sung: {latestReason}</div>}
      {error && <ErrorBanner message={error} />}

      {needsProfile && (
        <div className="mb-8 space-y-5 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
          <h3 className="font-semibold text-gray-900">1. Thông tin chuyên môn</h3>
          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-2 text-sm font-medium">
              Chuyên khoa *
              <select required value={form.specialtyId} onChange={update('specialtyId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20">
                <option value="">-- Chọn chuyên khoa --</option>
                {options.specialties.map(s => (
                  <option key={s.specialtyId} value={s.specialtyId}>{s.name}</option>
                ))}
              </select>
            </label>
            <Input label="Chức danh *" value={form.professionalTitle} onChange={update('professionalTitle')} placeholder="Ví dụ: BS.CKII, ThS.BS..." />
            <Input label="Số năm kinh nghiệm" type="number" min="0" max="80" value={form.experienceYears} onChange={update('experienceYears')} />
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Phạm vi tư vấn</label>
              <textarea 
                value={form.consultationScope} 
                onChange={update('consultationScope')} 
                className="h-24 resize-none rounded-xl border border-gray-300 p-3 font-normal outline-none focus:border-primary focus:ring-4 focus:ring-primary/20"
                placeholder="Mô tả các bệnh lý và chuyên môn tư vấn chính..."
              />
            </div>
            <div className="sm:col-span-2 grid gap-4 p-4 rounded-2xl bg-gray-50 border border-gray-200">
              <p className="text-xs font-bold uppercase text-gray-500">Địa điểm công tác</p>
              <div className="grid gap-4 sm:grid-cols-4">
                <label className="grid gap-2 text-sm font-medium">
                  Tỉnh/Thành phố *
                  <select required value={form.provinceId} onChange={update('provinceId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
                    <option value="">-- Chọn --</option>
                    {options.provinces.map(p => <option key={p.provinceId} value={p.provinceId}>{p.name}</option>)}
                  </select>
                </label>
                <label className="grid gap-2 text-sm font-medium">
                  Quận/Huyện
                  <select disabled={!form.provinceId} value={form.districtId} onChange={update('districtId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
                    <option value="">-- Chọn --</option>
                    {options.districts.map(d => <option key={d.districtId} value={d.districtId}>{d.name}</option>)}
                  </select>
                </label>
                <label className="grid gap-2 text-sm font-medium sm:col-span-2">
                  Cơ sở y tế *
                  <select required disabled={!form.provinceId} value={form.hospitalId} onChange={update('hospitalId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
                    <option value="">-- Chọn cơ sở --</option>
                    {options.hospitals.map(h => <option key={h.hospitalId} value={h.hospitalId}>{h.name}</option>)}
                    <option value="OTHER">-- Khác (Nhập tên cơ sở) --</option>
                  </select>
                </label>
              </div>
              {form.hospitalId === 'OTHER' && (
                <div className="mt-2 grid gap-4 rounded-xl border border-blue-200 bg-blue-50 p-4 sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <p className="mb-3 text-sm font-semibold text-blue-900">Tìm kiếm cơ sở y tế trên bản đồ</p>
                    <div className="relative">
                      <Input
                        label=""
                        placeholder="Gõ tên bệnh viện/phòng khám (VD: Bệnh viện Chợ Rẫy)..."
                        value={trackAsiaQuery}
                        onChange={e => setTrackAsiaQuery(e.target.value)}
                      />
                      {searchingHospitals && <div className="absolute right-3 top-3"><RefreshCw className="animate-spin text-blue-500" size={18} /></div>}
                      {trackAsiaResults.length > 0 && (
                        <div className="absolute top-14 z-10 max-h-60 w-full overflow-y-auto rounded-xl border border-gray-200 bg-white shadow-xl">
                          {trackAsiaResults.map((r, i) => (
                            <button
                              key={i}
                              type="button"
                              className="w-full border-b p-3 text-left hover:bg-blue-50 last:border-0"
                              onClick={() => {
                                update('trackAsiaName')({ target: { value: r.name } } as any);
                                update('trackAsiaAddress')({ target: { value: r.address } } as any);
                                update('trackAsiaLat')({ target: { value: String(r.lat) } } as any);
                                update('trackAsiaLng')({ target: { value: String(r.lng) } } as any);
                                setTrackAsiaQuery(r.name);
                                setTrackAsiaResults([]);
                              }}
                            >
                              <p className="font-semibold">{r.name}</p>
                              <p className="text-xs text-gray-500">{r.address}</p>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <Input label="Tên cơ sở y tế" value={form.trackAsiaName} readOnly className="bg-white/50" />
                  <Input label="Địa chỉ" value={form.trackAsiaAddress} readOnly className="bg-white/50" />
                  <Input label="Vĩ độ (Lat)" value={form.trackAsiaLat} readOnly className="bg-white/50" />
                  <Input label="Kinh độ (Lng)" value={form.trackAsiaLng} readOnly className="bg-white/50" />
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="mb-2 space-y-5 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
        <h3 className="font-semibold text-gray-900">{needsProfile ? '2.' : '1.'} Ảnh định danh</h3>
        <div className="grid gap-4 md:grid-cols-3">
          <ImageCapture title="Ảnh chân dung" hint="Nhìn thẳng, rõ mặt" facing="user" value={images.selfie} onFile={file => select('selfie', file)} onCamera={() => setActiveCamera('selfie')} />
          <ImageCapture title="CCCD mặt trước" hint="Đầy đủ 4 góc, rõ chữ" facing="environment" value={images.identityFront} onFile={file => select('identityFront', file)} onCamera={() => setActiveCamera('identityFront')} />
          <ImageCapture title="CCCD mặt sau" hint="Đầy đủ 4 góc, rõ chữ" facing="environment" value={images.identityBack} onFile={file => select('identityBack', file)} onCamera={() => setActiveCamera('identityBack')} />
        </div>

        {aiResult && (
          <div className={`mt-6 flex items-center gap-3 rounded-2xl p-4 border ${aiResult.similar ? 'bg-green-50 border-green-200 text-green-800' : 'bg-red-50 border-red-200 text-red-800'}`}>
            {aiResult.similar ? <BadgeCheck className="text-green-600" /> : <AlertCircle className="text-red-600" />}
            <div>
              <p className="font-bold">{aiResult.similar ? 'Khuôn mặt trùng khớp' : 'Khuôn mặt không khớp'}</p>
              <p className="text-xs opacity-80">Độ tương đồng: {(aiResult.similarity * 100).toFixed(1)}%</p>
            </div>
          </div>
        )}
      </div>

      <button
        onClick={submit}
        disabled={submitting}
        className="mt-5 w-full rounded-full bg-primary px-6 py-3 font-semibold text-white disabled:opacity-50"
      >
        {submitting ? 'Đang gửi dữ liệu...' : 'Hoàn tất & Gửi hồ sơ'}
      </button>

      {activeCamera && (
        <CameraDialog facing={activeCamera === 'selfie' ? 'user' : 'environment'} onClose={() => setActiveCamera(null)} onCapture={file => select(activeCamera, file)} />
      )}
    </StepCard>
  );
}

function CredentialStep({ onDone }: { onDone: () => Promise<void> }) {
  const [form, setForm] = useState({ 
    credentialType: 'MEDICAL_LICENSE', 
    credentialNumber: '', 
    issuer: '', 
    customIssuer: '',
    issuedDate: '', 
    expiryDate: '' 
  });
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const commonIssuers = [
    'Bộ Y tế',
    'Sở Y tế TP. Hồ Chí Minh',
    'Sở Y tế Hà Nội',
    'Sở Y tế Đà Nẵng',
    'Sở Y tế Cần Thơ',
    'Sở Y tế Hải Phòng',
    'Đại học Y Dược TP.HCM',
    'Đại học Y Hà Nội',
  ];

  const update =
    (key: keyof typeof form) =>
    (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm(current => {
        const next = { ...current, [key]: event.target.value };
        // If switching to Degree, clear expiry date as it's permanent
        if (key === 'credentialType' && next.credentialType === 'DEGREE') {
          next.expiryDate = '';
        }
        return next;
      });

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    // 1. Validate File
    if (!file) {
      setError('Vui lòng chọn ảnh hoặc PDF của chứng chỉ.');
      return;
    }

    // 2. Validate Issuer
    const finalIssuer = form.issuer === 'OTHER' ? form.customIssuer.trim() : form.issuer.trim();
    if (!finalIssuer) {
      setError('Vui lòng chọn hoặc nhập đơn vị cấp.');
      return;
    }

    // 3. Validate Dates
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Start of today

    const issued = new Date(form.issuedDate);
    if (issued > today) {
      setError('Ngày cấp không được vượt quá ngày hiện tại.');
      return;
    }

    if (form.credentialType !== 'DEGREE' && !form.expiryDate) {
      setError('Loại chứng chỉ này yêu cầu ngày hết hạn.');
      return;
    }

    if (form.expiryDate) {
      const expiry = new Date(form.expiryDate);
      if (expiry <= issued) {
        setError('Ngày hết hạn phải lớn hơn ngày cấp.');
        return;
      }
      if (expiry <= today) {
        setError('Chứng chỉ đã hết hạn. Ngày hết hạn phải là một ngày trong tương lai.');
        return;
      }
    }

    setSubmitting(true);
    try {
      await submitCredential({ 
        body: {
          credentialType: form.credentialType,
          credentialNumber: form.credentialNumber,
          issuer: finalIssuer,
          issuedDate: form.issuedDate,
          expiryDate: form.expiryDate || undefined
        }, 
        file 
      });
      await onDone();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tải chứng chỉ.');
    } finally {
      setSubmitting(false);
    }
  };

  const todayStr = new Date().toISOString().split('T')[0];

  return (
    <StepCard icon={<FileBadge />} title="Chứng chỉ chuyên môn" description="Cung cấp bằng chứng về trình độ chuyên môn để quản trị viên phê duyệt.">
      {error && <ErrorBanner message={error} />}
      <form className="grid gap-5 sm:grid-cols-2" onSubmit={submit}>
        <label className="grid gap-2 text-sm font-medium">
          Loại chứng chỉ *
          <select value={form.credentialType} onChange={update('credentialType')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
            <option value="MEDICAL_LICENSE">Giấy phép hành nghề y (CCHN Khám chữa bệnh)</option>
            <option value="DEGREE">Bằng cấp chuyên môn (BS, ThS, TS, BSCK I/II...)</option>
            <option value="CERTIFICATE">Chứng chỉ đào tạo liên tục (CME, Khóa học ngắn hạn)</option>
            <option value="PROFESSIONAL_LICENSE">Giấy phép chuyên môn khác (Dược, Điều dưỡng...)</option>
            <option value="IDENTITY_DOCUMENT">Giấy tờ định danh y tế bổ sung</option>
          </select>
        </label>
        <Input label="Số chứng chỉ *" value={form.credentialNumber} onChange={update('credentialNumber')} />
        
        <div className="grid gap-2 text-sm font-medium">
          <label>Đơn vị cấp *</label>
          <select required value={form.issuer} onChange={update('issuer')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
            <option value="">-- Chọn đơn vị cấp --</option>
            {commonIssuers.map(iss => (
              <option key={iss} value={iss}>{iss}</option>
            ))}
            <option value="OTHER">Khác (Nhập tên đơn vị)</option>
          </select>
          {form.issuer === 'OTHER' && (
            <input
              required
              type="text"
              placeholder="Nhập tên đơn vị cấp..."
              value={form.customIssuer}
              onChange={update('customIssuer')}
              className="mt-2 h-12 w-full rounded-xl border border-gray-300 px-3 font-normal focus:border-primary focus:ring-4 focus:ring-primary/20"
            />
          )}
        </div>

        <Input label="Ngày cấp *" type="date" max={todayStr} value={form.issuedDate} onChange={update('issuedDate')} />
        
        {form.credentialType !== 'DEGREE' ? (
          <Input label="Ngày hết hạn *" type="date" min={form.issuedDate || todayStr} value={form.expiryDate} onChange={update('expiryDate')} />
        ) : (
          <div className="grid gap-2 text-sm font-medium text-gray-400">
            Ngày hết hạn
            <div className="flex h-12 items-center rounded-xl border border-gray-200 bg-gray-50 px-3 font-normal italic">
              Bằng cấp chuyên môn không có thời hạn
            </div>
          </div>
        )}

        <label className="grid gap-2 text-sm font-medium sm:col-span-2">
          Tệp chứng chỉ *
          <input required type="file" accept=".pdf,.doc,.docx,image/jpeg,image/png,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={event => setFile(event.target.files?.[0] ?? null)} className="rounded-xl border border-dashed border-gray-300 p-3 font-normal" />
        </label>
        <SubmitButton busy={submitting}>Gửi chứng chỉ</SubmitButton>
      </form>
    </StepCard>
  );
}

function ReviewStep({ state, reload, setOverrideStep }: { state: ExpertOnboardingResponse; reload: () => Promise<void>; setOverrideStep: (step: ExpertOnboardingStep) => void }) {
  return (
    <StepCard icon={<ShieldCheck />} title="Đang chờ quản trị viên xét duyệt" description="CareBridge đang đối soát thông tin và bằng cấp của bạn.">
      <div className="grid gap-4 sm:grid-cols-3">
        <Status label="Định danh" value={state.identityStatus} />
        <Status label="Chứng chỉ" value={state.credentialStatus} />
        <Status label="Hồ sơ" value={state.verificationStatus} />
      </div>
      {(state.rejectionReason || state.latestIdentityAttempt?.reviewReason) && (
        <div className="mt-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">
          Phản hồi xét duyệt: {state.rejectionReason ?? state.latestIdentityAttempt?.reviewReason}
        </div>
      )}
      <div className="mt-6 flex flex-wrap gap-4">
        <button onClick={() => void reload()} className="inline-flex items-center gap-2 rounded-full border border-primary px-5 py-2.5 font-semibold text-primary">
          <RefreshCw size={17} />
          Kiểm tra trạng thái
        </button>
        
        {/* Cho phép chuyên gia làm lại nếu bị lỗi hoặc yêu cầu kiểm tra thủ công */}
        {(state.identityStatus === 'REJECTED' || state.identityStatus === 'MANUAL_REVIEW_REQUIRED') && (
          <button onClick={() => setOverrideStep('IDENTITY')} className="inline-flex items-center gap-2 rounded-full bg-red-50 border border-red-200 px-5 py-2.5 font-semibold text-red-700 hover:bg-red-100">
            Làm lại Định danh
          </button>
        )}
        {(state.credentialStatus === 'REJECTED') && (
          <button onClick={() => setOverrideStep('CREDENTIAL')} className="inline-flex items-center gap-2 rounded-full bg-red-50 border border-red-200 px-5 py-2.5 font-semibold text-red-700 hover:bg-red-100">
            Làm lại Chứng chỉ
          </button>
        )}
      </div>
    </StepCard>
  );
}

function ImageCapture({
  title,
  hint,
  facing,
  value,
  onFile,
  onCamera,
}: {
  title: string;
  hint: string;
  facing: 'user' | 'environment';
  value?: CapturedImage;
  onFile: (file: File) => void;
  onCamera: () => void;
}) {
  return (
    <div className="rounded-2xl border border-gray-200 p-4">
      <p className="font-semibold text-on-surface">{title}</p>
      <p className="mt-1 min-h-10 text-xs leading-5 text-gray-500">{hint}</p>
      {value ? (
        <img className="mt-3 h-44 w-full rounded-xl bg-gray-100 object-cover" src={value.preview} alt={title} />
      ) : (
        <div className="mt-3 flex h-44 items-center justify-center rounded-xl bg-gray-100 text-gray-400">
          <Camera size={38} />
        </div>
      )}
      <div className="mt-3 grid grid-cols-2 gap-2">
        <button type="button" onClick={onCamera} className="rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-white">
          {value ? 'Chụp lại' : 'Chụp ảnh'}
        </button>
        <label className="cursor-pointer rounded-lg border border-gray-300 px-3 py-2 text-center text-sm font-semibold text-gray-700">
          Chọn tệp
          <input
            className="hidden"
            type="file"
            accept="image/jpeg,image/png"
            capture={facing}
            onChange={event => {
              const selected = event.target.files?.[0];
              if (selected) onFile(selected);
              event.target.value = '';
            }}
          />
        </label>
      </div>
    </div>
  );
}

function CameraDialog({
  facing,
  onCapture,
  onClose,
}: {
  facing: 'user' | 'environment';
  onCapture: (file: File) => void;
  onClose: () => void;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const request = navigator.mediaDevices?.getUserMedia({
      video: { facingMode: { ideal: facing }, width: { ideal: 1280 } },
      audio: false,
    });
    if (!request) {
      setError('Trình duyệt không hỗ trợ camera.');
      return () => {
        active = false;
      };
    }
    request
      .then(stream => {
        if (!active) {
          stream.getTracks().forEach(track => track.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          void videoRef.current.play();
        }
      })
      .catch(() => setError('Không thể mở camera.'));
    return () => {
      if (streamRef.current) streamRef.current.getTracks().forEach(track => track.stop());
    };
  }, [facing]);

  const capture = () => {
    const video = videoRef.current;
    if (!video?.videoWidth) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')?.drawImage(video, 0, 0);
    canvas.toBlob(blob => {
      if (blob) onCapture(new File([blob], `carebridge-${Date.now()}.jpg`, { type: 'image/jpeg' }));
    }, 'image/jpeg', 0.9);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-2xl rounded-2xl bg-white p-5">
        <h3 className="font-semibold">Chụp ảnh</h3>
        {error && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        <video ref={videoRef} playsInline muted className="mt-4 max-h-[60vh] w-full rounded-xl bg-black object-contain" />
        <div className="mt-4 flex justify-end gap-3">
          <button onClick={onClose} className="rounded-full border px-5 py-2">Hủy</button>
          <button disabled={!!error} onClick={capture} className="rounded-full bg-primary px-5 py-2 font-semibold text-white disabled:opacity-50">
            Chụp
          </button>
        </div>
      </div>
    </div>
  );
}

function StepCard({ icon, title, description, children }: { icon: React.ReactNode; title: string; description: string; children: React.ReactNode }) {
  return (
    <section className="rounded-3xl border border-outline-variant/60 bg-surface p-6 sm:p-8 shadow-md">
      <div className="mb-6 flex items-start gap-4 pb-4 border-b border-outline-variant/40">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary-container text-primary shadow-xs">{icon}</div>
        <div>
          <h2 className="text-xl font-bold text-on-surface m-0 leading-tight">{title}</h2>
          <p className="mt-1 text-xs text-on-surface-variant leading-relaxed m-0">{description}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

function Input({ label, className, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="grid gap-1.5 text-xs font-semibold text-on-surface">
      {label}
      <input
        {...props}
        required={label.includes('*')}
        className={`h-11 rounded-2xl border border-outline-variant bg-surface px-4 text-sm text-on-surface outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary font-sans ${className || ''}`}
      />
    </label>
  );
}

function SubmitButton({ busy, children }: { busy: boolean; children: React.ReactNode }) {
  return (
    <button
      disabled={busy}
      className="min-h-12 rounded-full bg-primary px-8 py-3 font-semibold text-on-primary text-sm outline-none transition-all hover:brightness-110 active:scale-95 disabled:opacity-50 sm:col-span-2 cursor-pointer shadow-md"
    >
      {busy ? 'Đang xử lý...' : children}
    </button>
  );
}

function ErrorBanner({ message, retry }: { message: string; retry?: () => Promise<void> }) {
  return (
    <div className="mb-5 flex items-start gap-3 rounded-2xl bg-rose-50 border border-rose-200 p-4 text-sm text-rose-800 shadow-xs" role="alert">
      <AlertCircle size={20} className="shrink-0 text-rose-600 mt-0.5" />
      <span className="flex-1 text-xs leading-relaxed">{message}</span>
      {retry && <button className="font-bold underline text-xs text-rose-900 cursor-pointer" onClick={() => void retry()}>Thử lại</button>}
    </div>
  );
}

function Loading() {
  return (
    <div className="flex min-h-[420px] items-center justify-center gap-3 text-on-surface-variant text-sm font-medium">
      <div className="h-8 w-8 animate-spin rounded-full border-3 border-primary border-t-transparent" />
      Đang tải dữ liệu tiến độ xác minh...
    </div>
  );
}

function Status({ label, value }: { label: string; value: string | null }) {
  const positive = value === 'APPROVED';
  const rejected = value === 'REJECTED';

  const statusTranslations: Record<string, string> = {
    APPROVED: 'Đã duyệt',
    REJECTED: 'Từ chối',
    PENDING: 'Đang chờ',
    MANUAL_REVIEW_REQUIRED: 'Chờ duyệt thủ công',
  };

  const displayValue = value ? (statusTranslations[value] || value) : 'CHƯA GỬI';

  return (
    <div
      className={`rounded-2xl border p-4 shadow-xs ${
        positive
          ? 'border-emerald-300 bg-emerald-50/70 text-emerald-950'
          : rejected
          ? 'border-rose-300 bg-rose-50/70 text-rose-950'
          : 'border-amber-300 bg-amber-50/70 text-amber-950'
      }`}
    >
      <p className="text-[11px] font-semibold text-outline uppercase tracking-wider m-0">{label}</p>
      <p className="mt-1 font-bold text-sm m-0">{displayValue}</p>
    </div>
  );
}
