import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, BadgeCheck, Camera, Check, FileBadge, RefreshCw, ShieldCheck } from 'lucide-react';
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
  type ExpertOnboardingResponse,
  type ProvinceResponse,
  type DistrictResponse,
  type WardResponse,
  type SpecialtyResponse,
  type HospitalResponse,
} from '../services/expertApi';

const IMAGE_LIMIT = 5 * 1024 * 1024;

type ImageSlot = 'selfie' | 'identityFront' | 'identityBack';
type CapturedImage = { file: File; preview: string };

const steps = [
  ['PROFILE', 'Hồ sơ'],
  ['IDENTITY', 'Định danh'],
  ['CREDENTIAL', 'Chứng chỉ'],
  ['UNDER_REVIEW', 'Xét duyệt'],
] as const;

export default function ExpertOnboardingPage() {
  const navigate = useNavigate();
  const [state, setState] = useState<ExpertOnboardingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = async () => {
    setError(null);
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

  return (
    <div className="mx-auto max-w-5xl p-5 sm:p-8">
      <header className="rounded-3xl bg-gradient-to-br from-primary to-[#a96856] p-7 text-white shadow-lg">
        <p className="text-sm font-medium text-white/75">Cổng chuyên gia CareBridge</p>
        <h1 className="mt-2 text-3xl font-semibold">Hoàn tất xác minh chuyên gia</h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-white/85">
          Quy trình tuân thủ chuẩn Bộ Y Tế. Vui lòng cung cấp thông tin chính xác và ảnh rõ nét để được phê duyệt nhanh nhất.
        </p>
      </header>

      <div className="my-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {steps.map(([key, label], index) => {
          const activeIndex = Math.max(0, steps.findIndex(([step]) => step === state?.nextStep));
          const complete = state?.nextStep === 'COMPLETE' || index < activeIndex;
          const active = index === activeIndex && state?.nextStep !== 'COMPLETE';
          return (
            <div key={key} className={`rounded-2xl border p-4 ${active ? 'border-primary bg-primary/5' : 'border-gray-200 bg-white'}`}>
              <div className={`mb-2 flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold ${complete ? 'bg-green-600 text-white' : active ? 'bg-primary text-white' : 'bg-gray-100 text-gray-500'}`}>
                {complete ? <Check size={17} /> : index + 1}
              </div>
              <p className="text-sm font-semibold text-on-surface">{label}</p>
            </div>
          );
        })}
      </div>

      {error && <ErrorBanner message={error} retry={reload} />}
      {state?.nextStep === 'PROFILE' && <ProfileStep onDone={reload} />}
      {state?.nextStep === 'IDENTITY' && <IdentityStep onDone={reload} latestReason={state.latestIdentityAttempt?.reviewReason} />}
      {state?.nextStep === 'CREDENTIAL' && <CredentialStep onDone={reload} />}
      {state?.nextStep === 'UNDER_REVIEW' && <ReviewStep state={state} reload={reload} />}
      {state?.nextStep === 'COMPLETE' && (
        <section className="rounded-3xl border border-green-200 bg-green-50 p-8 text-center">
          <BadgeCheck className="mx-auto text-green-700" size={58} />
          <h2 className="mt-4 text-2xl font-semibold text-green-900">Hồ sơ đã được xác minh</h2>
          <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-green-800">Bạn có thể sử dụng đầy đủ cổng chuyên gia CareBridge.</p>
          <button className="mt-6 rounded-full bg-green-700 px-6 py-3 font-semibold text-white" onClick={() => navigate('/expert/dashboard', { replace: true })}>
            Đi đến trang chuyên gia
          </button>
        </section>
      )}
    </div>
  );
}

function ProfileStep({ onDone }: { onDone: () => Promise<void> }) {
  const [form, setForm] = useState({
    specialtyId: '',
    professionalTitle: '',
    experienceYears: '',
    provinceId: '',
    districtId: '',
    wardId: '',
    hospitalId: '',
    consultationScope: '',
  });
  const [options, setOptions] = useState({
    provinces: [] as ProvinceResponse[],
    districts: [] as DistrictResponse[],
    wards: [] as WardResponse[],
    specialties: [] as SpecialtyResponse[],
    hospitals: [] as HospitalResponse[],
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadMasterData = async () => {
      try {
        const [p, s] = await Promise.all([getProvinces(), getSpecialties()]);
        setOptions(prev => ({ ...prev, provinces: p, specialties: s }));
      } catch {
        setError('Không thể tải dữ liệu danh mục.');
      }
    };
    void loadMasterData();
  }, []);

  useEffect(() => {
    let active = true;
    if (!form.provinceId) {
      setOptions(prev => ({ ...prev, districts: [] }));
      return () => { active = false; };
    }

    const requestedProvince = form.provinceId;
    setOptions(prev => ({ ...prev, districts: [] }));
    getDistricts(requestedProvince)
      .then(districts => {
        if (active) setOptions(prev => ({ ...prev, districts }));
      })
      .catch(() => {
        if (active) setError('Không thể tải danh sách quận/huyện.');
      });

    return () => { active = false; };
  }, [form.provinceId]);

  useEffect(() => {
    let active = true;
    if (!form.provinceId) {
      setOptions(prev => ({ ...prev, wards: [], hospitals: [] }));
      return () => { active = false; };
    }

    const requestedProvince = form.provinceId;
    const requestedDistrict = form.districtId;
    setOptions(prev => ({ ...prev, wards: [], hospitals: [] }));
    if (requestedDistrict) {
      getWards(requestedDistrict)
        .then(wards => {
          if (active) setOptions(prev => ({ ...prev, wards }));
        })
        .catch(() => {
          if (active) setError('Không thể tải danh sách phường/xã.');
        });
    }

    getHospitals({
        provinceId: requestedProvince,
        districtId: requestedDistrict || undefined,
      })
      .then(hospitals => {
        if (active) setOptions(prev => ({ ...prev, hospitals }));
      })
      .catch(() => {
        if (active) setError('Không thể tải danh sách cơ sở y tế.');
      });

    return () => { active = false; };
  }, [form.districtId, form.provinceId]);

  const update =
    (key: keyof typeof form) =>
    (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      const val = event.target.value;
      setForm(prev => {
        const next = { ...prev, [key]: val };
        if (key === 'provinceId') {
          next.districtId = '';
          next.wardId = '';
          next.hospitalId = '';
        }
        if (key === 'districtId') {
          next.wardId = '';
          next.hospitalId = '';
        }
        return next;
      });
    };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createMyProfile({
        specialtyId: form.specialtyId,
        professionalTitle: form.professionalTitle.trim(),
        hospitalId: form.hospitalId,
        consultationScope: form.consultationScope.trim(),
        experienceYears: form.experienceYears ? Number(form.experienceYears) : undefined,
      });
      await onDone();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể lưu hồ sơ.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StepCard icon={<FileBadge />} title="Thông tin chuyên môn" description="Thông tin được chuẩn hóa theo Bộ Y Tế. Vui lòng chọn địa bàn và cơ sở y tế nơi công tác.">
      {error && <ErrorBanner message={error} />}
      <form className="grid gap-5 sm:grid-cols-2" onSubmit={submit}>
        <label className="grid gap-2 text-sm font-medium">
          Chuyên khoa *
          <select required value={form.specialtyId} onChange={update('specialtyId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20">
            <option value="">-- Chọn chuyên khoa --</option>
            {options.specialties.map(s => (
              <option key={s.specialtyId} value={s.specialtyId}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <Input label="Chức danh *" value={form.professionalTitle} onChange={update('professionalTitle')} placeholder="Ví dụ: BS.CKII, ThS.BS..." />
        <Input label="Số năm kinh nghiệm" type="number" min="0" max="80" value={form.experienceYears} onChange={update('experienceYears')} />

        <div className="sm:col-span-2 grid gap-4 p-4 rounded-2xl bg-gray-50 border border-gray-200">
          <p className="text-xs font-bold uppercase text-gray-500">Địa điểm công tác</p>
          <div className="grid gap-4 sm:grid-cols-4">
            <label className="grid gap-2 text-sm font-medium">
              Tỉnh/Thành phố *
              <select required value={form.provinceId} onChange={update('provinceId')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20">
                <option value="">-- Chọn Tỉnh/TP --</option>
                {options.provinces.map(p => (
                  <option key={p.provinceId} value={p.provinceId}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-2 text-sm font-medium">
              Quận/Huyện
              <select value={form.districtId} onChange={update('districtId')} disabled={!form.provinceId} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20 disabled:cursor-not-allowed disabled:bg-gray-100">
                <option value="">-- Chọn Huyện --</option>
                {options.districts.map(d => (
                  <option key={d.districtId} value={d.districtId}>
                    {d.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-2 text-sm font-medium">
              Phường/Xã
              <select value={form.wardId} onChange={update('wardId')} disabled={!form.districtId} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20 disabled:cursor-not-allowed disabled:bg-gray-100">
                <option value="">-- Chọn Phường/Xã --</option>
                {options.wards.map(w => (
                  <option key={w.wardId} value={w.wardId}>
                    {w.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-2 text-sm font-medium">
              Bệnh viện/Cơ sở y tế *
              <select required value={form.hospitalId} onChange={update('hospitalId')} disabled={!form.provinceId} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20 disabled:cursor-not-allowed disabled:bg-gray-100">
                <option value="">-- Chọn cơ sở --</option>
                {options.hospitals.map(h => (
                  <option key={h.hospitalId} value={h.hospitalId}>{h.name}</option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <label className="grid gap-2 text-sm font-medium sm:col-span-2">
          Phạm vi tư vấn *
          <textarea required rows={4} value={form.consultationScope} onChange={update('consultationScope')} className="rounded-xl border border-gray-300 p-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20" placeholder="Mô tả chi tiết các bệnh lý hoặc lĩnh vực bạn có thể hỗ trợ..." />
        </label>
        <SubmitButton busy={submitting}>Lưu và tiếp tục</SubmitButton>
      </form>
    </StepCard>
  );
}

function IdentityStep({ onDone, latestReason }: { onDone: () => Promise<void>; latestReason?: string | null }) {
  const [images, setImages] = useState<Partial<Record<ImageSlot, CapturedImage>>>({});
  const [activeCamera, setActiveCamera] = useState<ImageSlot | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [aiResult, setAiResult] = useState<{ similar: boolean; similarity: number } | null>(null);
  const imageUrls = useRef(new Set<string>());

  useEffect(() => () => imageUrls.current.forEach(url => URL.revokeObjectURL(url)), []);

  const select = async (slot: ImageSlot, file: File) => {
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setError('Chỉ chấp nhận ảnh JPEG hoặc PNG.');
      return;
    }
    if (file.size > IMAGE_LIMIT) {
      setError('Mỗi ảnh phải có dung lượng tối đa 5 MB.');
      return;
    }

    setAiResult(null);
    setImages(current => {
      const previous = current[slot];
      if (previous) {
        URL.revokeObjectURL(previous.preview);
        imageUrls.current.delete(previous.preview);
      }
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
          setAiResult({ similar: res.similar, similarity: res.similarity });
        } catch (e) {
          console.error('AI Verify failed', e);
        }
      }
    };
    void runVerify();
  }, [images.selfie, images.identityFront]);

  const submit = async () => {
    if (!images.selfie || !images.identityFront || !images.identityBack) {
      setError('Cần đủ ảnh chân dung và hai mặt CCCD.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await submitIdentityEvidence({
        selfie: images.selfie.file,
        identityFront: images.identityFront.file,
        identityBack: images.identityBack.file,
      });
      await onDone();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể gửi bộ ảnh định danh.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StepCard icon={<Camera />} title="Ảnh chân dung và CCCD" description="Hệ thống AI sẽ đối soát khuôn mặt bạn với CCCD để xác minh danh tính.">
      {latestReason && <div className="mb-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Yêu cầu bổ sung: {latestReason}</div>}
      {error && <ErrorBanner message={error} />}

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

      <button
        onClick={submit}
        disabled={submitting}
        className="mt-5 w-full rounded-full bg-primary px-6 py-3 font-semibold text-white disabled:opacity-50"
      >
        {submitting ? 'Đang tải bộ ảnh...' : 'Gửi bộ ảnh định danh'}
      </button>
      {activeCamera && (
        <CameraDialog facing={activeCamera === 'selfie' ? 'user' : 'environment'} onClose={() => setActiveCamera(null)} onCapture={file => select(activeCamera, file)} />
      )}
    </StepCard>
  );
}

function CredentialStep({ onDone }: { onDone: () => Promise<void> }) {
  const [form, setForm] = useState({ credentialType: 'MEDICAL_LICENSE', credentialNumber: '', issuer: '', issuedDate: '', expiryDate: '' });
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const update =
    (key: keyof typeof form) =>
    (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm(current => ({ ...current, [key]: event.target.value }));

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!file) {
      setError('Vui lòng chọn ảnh hoặc PDF của chứng chỉ.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await submitCredential({ body: form, file });
      await onDone();
    } catch (caught: unknown) {
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tải chứng chỉ.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StepCard icon={<FileBadge />} title="Chứng chỉ chuyên môn" description="Cung cấp bằng chứng về trình độ chuyên môn để quản trị viên phê duyệt.">
      {error && <ErrorBanner message={error} />}
      <form className="grid gap-5 sm:grid-cols-2" onSubmit={submit}>
        <label className="grid gap-2 text-sm font-medium">
          Loại chứng chỉ *
          <select value={form.credentialType} onChange={update('credentialType')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal">
            <option value="MEDICAL_LICENSE">Giấy phép hành nghề y</option>
            <option value="DEGREE">Bằng cấp chuyên môn</option>
            <option value="CERTIFICATE">Chứng chỉ đào tạo</option>
            <option value="PROFESSIONAL_LICENSE">Giấy phép chuyên môn</option>
          </select>
        </label>
        <Input label="Số chứng chỉ *" value={form.credentialNumber} onChange={update('credentialNumber')} />
        <Input label="Đơn vị cấp *" value={form.issuer} onChange={update('issuer')} />
        <Input label="Ngày cấp *" type="date" value={form.issuedDate} onChange={update('issuedDate')} />
        <Input label="Ngày hết hạn" type="date" value={form.expiryDate} onChange={update('expiryDate')} />
        <label className="grid gap-2 text-sm font-medium">
          Tệp chứng chỉ *
          <input required type="file" accept="image/jpeg,image/png,application/pdf" onChange={event => setFile(event.target.files?.[0] ?? null)} className="rounded-xl border border-dashed border-gray-300 p-3 font-normal" />
        </label>
        <SubmitButton busy={submitting}>Gửi chứng chỉ</SubmitButton>
      </form>
    </StepCard>
  );
}

function ReviewStep({ state, reload }: { state: ExpertOnboardingResponse; reload: () => Promise<void> }) {
  return (
    <StepCard icon={<ShieldCheck />} title="Đang chờ quản trị viên xét duyệt" description="CareBridge đang đối soát thông tin và bằng cấp của bạn.">
      <div className="grid gap-4 sm:grid-cols-3">
        <Status label="Định danh" value={state.identityStatus} />
        <Status label="Chứng chỉ" value={state.credentialStatus} />
        <Status label="Hồ sơ" value={state.verificationStatus} />
      </div>
      {state.latestIdentityAttempt?.reviewReason && <div className="mt-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">Phản hồi định danh: {state.latestIdentityAttempt.reviewReason}</div>}
      <button onClick={() => void reload()} className="mt-6 inline-flex items-center gap-2 rounded-full border border-primary px-5 py-2.5 font-semibold text-primary">
        <RefreshCw size={17} />
        Kiểm tra trạng thái
      </button>
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
    <section className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="mb-6 flex gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">{icon}</div>
        <div>
          <h2 className="text-xl font-semibold text-on-surface">{title}</h2>
          <p className="mt-1 text-sm leading-6 text-gray-600">{description}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

function Input({ label, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  return (
    <label className="grid gap-2 text-sm font-medium">
      {label}
      <input {...props} required={label.includes('*')} className="h-12 rounded-xl border border-gray-300 px-3 font-normal outline-none transition-colors focus:border-primary focus:ring-4 focus:ring-primary/20" />
    </label>
  );
}

function SubmitButton({ busy, children }: { busy: boolean; children: React.ReactNode }) {
  return <button disabled={busy} className="min-h-12 rounded-full bg-primary px-6 py-3 font-semibold text-white outline-none transition-all duration-300 hover:-translate-y-0.5 active:scale-95 focus-visible:ring-4 focus-visible:ring-primary/20 disabled:opacity-50 sm:col-span-2">{busy ? 'Đang xử lý...' : children}</button>;
}

function ErrorBanner({ message, retry }: { message: string; retry?: () => Promise<void> }) {
  return (
    <div className="mb-5 flex items-start gap-3 rounded-xl bg-red-50 p-4 text-sm text-red-700" role="alert">
      <AlertCircle size={19} className="shrink-0" />
      <span className="flex-1">{message}</span>
      {retry && <button className="font-semibold underline" onClick={() => void retry()}>Thử lại</button>}
    </div>
  );
}

function Loading() {
  return <div className="flex min-h-[420px] items-center justify-center"><div className="h-9 w-9 animate-spin rounded-full border-2 border-primary border-t-transparent" /></div>;
}

function Status({ label, value }: { label: string; value: string | null }) {
  const positive = value === 'APPROVED';
  const rejected = value === 'REJECTED';
  return (
    <div className={`rounded-2xl border p-4 ${positive ? 'border-green-200 bg-green-50' : rejected ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="mt-1 font-semibold">{value ?? 'CHƯA GỬI'}</p>
    </div>
  );
}
