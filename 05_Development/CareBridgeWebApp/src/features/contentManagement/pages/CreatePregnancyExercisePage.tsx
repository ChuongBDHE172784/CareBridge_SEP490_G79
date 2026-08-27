import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  adminExerciseLimits,
  type AdminExerciseFieldErrors,
  type AdminExerciseForm,
  type AdminExerciseFormField,
  type DifficultyLevel,
  type TrimesterScope,
} from '../models/adminExercise';
import {
  createAdminExercise,
  toAdminExerciseRequestError,
  updateAdminExercise,
} from '../services/adminExerciseApi';

const initialForm: AdminExerciseForm = {
  title: '',
  description: '',
  trimesterScope: 'ALL',
  difficultyLevel: 'EASY',
  durationMinutes: 15,
  instructionContent: '',
  mediaUrl: '',
  safetyWarning: '',
  supportsPostureAnalysis: false,
};

function validateForm(form: AdminExerciseForm): AdminExerciseFieldErrors {
  const errors: AdminExerciseFieldErrors = {};
  if (!form.title.trim()) {
    errors.title = 'Vui lòng nhập tên bài tập.';
  } else if (form.title.length > adminExerciseLimits.titleMaxLength) {
    errors.title = `Tên bài tập không được vượt quá ${adminExerciseLimits.titleMaxLength} ký tự.`;
  }

  if (!form.safetyWarning.trim()) {
    errors.safetyWarning = 'Vui lòng nhập hướng dẫn an toàn.';
  } else if (form.safetyWarning.length > adminExerciseLimits.safetyWarningMaxLength) {
    errors.safetyWarning = `Hướng dẫn an toàn không được vượt quá ${adminExerciseLimits.safetyWarningMaxLength} ký tự.`;
  }

  if (!Number.isInteger(form.durationMinutes)
      || form.durationMinutes < adminExerciseLimits.durationMinutesMin
      || form.durationMinutes > adminExerciseLimits.durationMinutesMax) {
    errors.durationMinutes = 'Thời lượng phải là số nguyên từ 1 đến 180 phút.';
  }
  return errors;
}

export default function CreatePregnancyExercisePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<AdminExerciseForm>(initialForm);
  const [isSaving, setIsSaving] = useState(false);
  const [savedExerciseId, setSavedExerciseId] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<AdminExerciseFieldErrors>({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const update = <K extends keyof AdminExerciseForm>(key: K, value: AdminExerciseForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setFieldErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
    setError('');
    setNotice('');
  };

  const focusFirstInvalidField = (errors: AdminExerciseFieldErrors) => {
    const field = (['title', 'durationMinutes', 'safetyWarning'] as AdminExerciseFormField[])
      .find((candidate) => errors[candidate]);
    if (field) {
      requestAnimationFrame(() => document.getElementById(`exercise-${field}`)?.focus());
    }
  };

  const persist = async (previewAfterSave: boolean) => {
    if (isSaving) return;
    const validationErrors = validateForm(form);
    setFieldErrors(validationErrors);
    setError('');
    setNotice('');
    if (Object.keys(validationErrors).length > 0) {
      focusFirstInvalidField(validationErrors);
      return;
    }

    setIsSaving(true);
    try {
      const saved = savedExerciseId
        ? await updateAdminExercise(savedExerciseId, form)
        : await createAdminExercise(form);
      const persistedId = saved?.exerciseId;
      if (!persistedId || (savedExerciseId !== null && persistedId !== savedExerciseId)) {
        throw new Error('Exercise identity missing or mismatched in save response');
      }

      setSavedExerciseId(persistedId);
      if (previewAfterSave) {
        navigate(`/content/exercises/${persistedId}/preview`);
      } else {
        setNotice(`Đã lưu bản nháp. Mã bài tập: ${persistedId}.`);
      }
    } catch (saveError) {
      const requestError = toAdminExerciseRequestError(saveError);
      setFieldErrors(requestError.fieldErrors);
      setError(requestError.message);
      focusFirstInvalidField(requestError.fieldErrors);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-background p-8 pb-28 font-sans text-on-surface">
      <div className="mx-auto max-w-6xl">
        {/* Top Navigation Back Button */}
        <div className="mb-4">
          <button
            type="button"
            onClick={() => navigate('/content/exercises')}
            disabled={isSaving}
            className="inline-flex items-center gap-1.5 py-2 px-4 rounded-full bg-surface border border-outline-variant text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
          >
            <span className="material-symbols-outlined text-base">arrow_back</span>
            Trở lại danh sách bài tập
          </button>
        </div>

        <div className="mb-8">
          <h1 className="m-0 text-[28px] font-bold">Tạo Bài Tập Thai Kỳ Mới</h1>
          <p className="mt-2 text-on-surface-variant text-sm">
            Thiết lập chi tiết bài tập, hướng dẫn an toàn và cấu hình phân tích tư thế AI.
          </p>
        </div>

        {error && (
          <div role="alert" className="mb-6 rounded-2xl border border-error/30 bg-error-container/40 p-4 text-sm text-error">
            {error}
          </div>
        )}
        {notice && (
          <div role="status" aria-live="polite" className="mb-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-700">
            {notice}
          </div>
        )}

        <fieldset
          disabled={isSaving}
          aria-busy={isSaving}
          className="m-0 grid min-w-0 gap-6 border-0 p-0 lg:grid-cols-[2fr_1fr]"
        >
          <div className="space-y-6">
            <section className="rounded-2xl bg-surface p-8 shadow-sm border border-surface-container-highest">
              <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">info</span>
                Thông tin cơ bản
              </h2>
              <div className="space-y-5">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                    Tên Bài Tập *
                  </span>
                  <input
                    id="exercise-title"
                    value={form.title}
                    onChange={(event) => update('title', event.target.value)}
                    aria-invalid={Boolean(fieldErrors.title)}
                    aria-describedby={fieldErrors.title ? 'exercise-title-error' : undefined}
                    placeholder="VD: Yoga thư giãn vùng hông"
                    className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary"
                  />
                  {fieldErrors.title && (
                    <span id="exercise-title-error" className="mt-1.5 block text-sm text-error">
                      {fieldErrors.title}
                    </span>
                  )}
                </label>
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                    Mô tả ngắn
                  </span>
                  <textarea
                    value={form.description}
                    onChange={(event) => update('description', event.target.value)}
                    rows={3}
                    placeholder="Mô tả tóm tắt lợi ích của bài tập..."
                    className="w-full resize-none rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary"
                  />
                </label>
                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">Độ khó *</span>
                    <select
                      value={form.difficultyLevel}
                      onChange={(event) => update('difficultyLevel', event.target.value as DifficultyLevel)}
                      className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary cursor-pointer"
                    >
                      <option value="EASY">Người mới bắt đầu</option>
                      <option value="MEDIUM">Trung bình</option>
                      <option value="HARD">Nâng cao</option>
                    </select>
                  </label>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                      Thời lượng (phút) *
                    </span>
                    <input
                      id="exercise-durationMinutes"
                      value={form.durationMinutes}
                      onChange={(event) => update('durationMinutes', Number(event.target.value))}
                      min={1}
                      max={180}
                      type="number"
                      aria-invalid={Boolean(fieldErrors.durationMinutes)}
                      aria-describedby={fieldErrors.durationMinutes ? 'exercise-duration-error' : undefined}
                      className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary"
                    />
                    {fieldErrors.durationMinutes && (
                      <span id="exercise-duration-error" className="mt-1.5 block text-sm text-error">
                        {fieldErrors.durationMinutes}
                      </span>
                    )}
                  </label>
                </div>
              </div>
            </section>

            <section className="rounded-2xl bg-surface p-8 shadow-sm border border-surface-container-highest">
              <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">list_alt</span>
                Hướng dẫn chi tiết
              </h2>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                  Các bước thực hiện
                </span>
                <textarea
                  value={form.instructionContent}
                  onChange={(event) => update('instructionContent', event.target.value)}
                  rows={6}
                  placeholder="Nhập từng bước một..."
                  className="w-full resize-none rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary"
                />
              </label>
              <label className="mt-5 block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-error">
                  Hướng dẫn an toàn (Cảnh báo)
                </span>
                <textarea
                  id="exercise-safetyWarning"
                  value={form.safetyWarning}
                  onChange={(event) => update('safetyWarning', event.target.value)}
                  rows={3}
                  aria-invalid={Boolean(fieldErrors.safetyWarning)}
                  aria-describedby={fieldErrors.safetyWarning ? 'exercise-safety-error' : undefined}
                  placeholder="Những lưu ý quan trọng để tránh chấn thương..."
                  className="w-full resize-none rounded-xl border-2 border-error/30 bg-error-container/20 px-4 py-3 outline-none focus:border-error"
                />
                {fieldErrors.safetyWarning && (
                  <span id="exercise-safety-error" className="mt-1.5 block text-sm text-error">
                    {fieldErrors.safetyWarning}
                  </span>
                )}
              </label>
            </section>
          </div>

          <aside className="space-y-6">
            <section className="rounded-2xl bg-surface p-6 shadow-sm border border-surface-container-highest">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">pregnant_woman</span>
                Phù hợp cho
              </h2>
              <select
                value={form.trimesterScope}
                onChange={(event) => update('trimesterScope', event.target.value as TrimesterScope)}
                className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary cursor-pointer"
              >
                <option value="FIRST">Tam cá nguyệt 1</option>
                <option value="SECOND">Tam cá nguyệt 2</option>
                <option value="THIRD">Tam cá nguyệt 3</option>
                <option value="ALL">Tất cả giai đoạn</option>
              </select>
            </section>

            <section className="rounded-2xl bg-surface p-6 shadow-sm border border-surface-container-highest">
              <div className="mb-4 flex items-start justify-between gap-3">
                <h2 className="m-0 flex items-center gap-2 text-xl font-bold">
                  <span className="material-symbols-outlined text-primary">accessibility_new</span>
                  Phân tích tư thế AI
                </h2>
                <button
                  type="button"
                  role="switch"
                  aria-checked={form.supportsPostureAnalysis}
                  aria-label="Phân tích tư thế AI"
                  aria-describedby="posture-analysis-help"
                  onClick={() => update('supportsPostureAnalysis', !form.supportsPostureAnalysis)}
                  className={`relative h-11 w-16 shrink-0 rounded-full transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 ${
                    form.supportsPostureAnalysis ? 'bg-primary' : 'bg-outline-variant'
                  }`}
                >
                  <span
                    aria-hidden="true"
                    className={`absolute top-2 h-7 w-7 rounded-full bg-white shadow-sm transition-all ${
                      form.supportsPostureAnalysis ? 'left-7' : 'left-2'
                    }`}
                  />
                </button>
              </div>
              <p id="posture-analysis-help" className="text-sm text-on-surface-variant m-0">
                Chỉ bật khi bài tập đã có cấu hình phân tích tư thế phù hợp và đang hoạt động.
              </p>
            </section>

            <section className="rounded-2xl bg-surface p-6 shadow-sm border border-surface-container-highest">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">image</span>
                Ảnh minh họa
              </h2>
              <input
                value={form.mediaUrl}
                onChange={(event) => update('mediaUrl', event.target.value)}
                placeholder="https://cdn.carebridge.com/exercises/yoga.jpg"
                className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary"
              />
              <p className="mb-0 mt-2 text-xs leading-relaxed text-on-surface-variant">
                Dán URL ảnh đã được lưu trữ; chức năng tải tệp trực tiếp chưa được hỗ trợ.
              </p>
            </section>
          </aside>
        </fieldset>
      </div>

      <footer className="fixed bottom-0 left-0 right-0 z-10 flex flex-wrap items-center justify-between gap-3 border-t border-outline-variant bg-surface px-4 py-4 md:left-64 md:px-8">
        <button
          type="button"
          onClick={() => navigate('/content/exercises')}
          disabled={isSaving}
          className="rounded-full px-6 py-2.5 font-semibold text-on-surface-variant border border-outline-variant hover:bg-surface-container-low transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
        >
          Hủy
        </button>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={() => void persist(false)}
            disabled={isSaving}
            className="min-h-11 rounded-full bg-surface-container-high px-6 py-2.5 font-semibold text-primary hover:bg-surface-container-highest transition-colors cursor-pointer border border-outline-variant disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu bản nháp'}
          </button>
          <button
            type="button"
            onClick={() => void persist(true)}
            disabled={isSaving}
            className="flex min-h-11 items-center gap-2 rounded-full bg-primary px-8 py-2.5 font-semibold text-on-primary disabled:cursor-not-allowed disabled:opacity-60 hover:bg-primary/90 transition-colors cursor-pointer border-0"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu & xem trước'}
            <span aria-hidden="true" className="material-symbols-outlined text-[18px]">visibility</span>
          </button>
        </div>
      </footer>
    </div>
  );
}

