import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { AdminExerciseForm, DifficultyLevel, TrimesterScope } from '../models/adminExercise';
import { createAdminExercise } from '../services/adminExerciseApi';

const initialForm: AdminExerciseForm = {
  title: '',
  description: '',
  trimesterScope: 'ALL',
  difficultyLevel: 'EASY',
  durationMinutes: 15,
  instructionContent: '',
  mediaUrl: '',
  safetyWarning: '',
  supportsPostureAnalysis: true,
};

export default function CreatePregnancyExercisePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<AdminExerciseForm>(initialForm);
  const [isSaving, setIsSaving] = useState(false);

  const update = <K extends keyof AdminExerciseForm>(key: K, value: AdminExerciseForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const handleSubmit = async () => {
    setIsSaving(true);
    try {
      const created = await createAdminExercise(form);
      navigate(`/content/exercises/${created.exerciseId}/preview`);
    } catch {
      navigate('/content/exercises/preview');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-background p-8 pb-28 font-sans text-on-surface">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8">
          <h1 className="m-0 text-[28px] font-bold">Tạo Bài Tập Thai Kỳ Mới</h1>
          <p className="mt-2 text-on-surface-variant">
            Thiết lập chi tiết bài tập, hướng dẫn an toàn và cấu hình phân tích tư thế AI.
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
          <div className="space-y-6">
            <section className="rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
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
                    value={form.title}
                    onChange={(event) => update('title', event.target.value)}
                    placeholder="VD: Yoga thư giãn vùng hông"
                    className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
                  />
                </label>
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                    Mô tả ngắn *
                  </span>
                  <textarea
                    value={form.description}
                    onChange={(event) => update('description', event.target.value)}
                    rows={3}
                    placeholder="Mô tả tóm tắt lợi ích của bài tập..."
                    className="w-full resize-none rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
                  />
                </label>
                <div className="grid gap-4 sm:grid-cols-2">
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">Độ khó *</span>
                    <select
                      value={form.difficultyLevel}
                      onChange={(event) => update('difficultyLevel', event.target.value as DifficultyLevel)}
                      className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
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
                      value={form.durationMinutes}
                      onChange={(event) => update('durationMinutes', Number(event.target.value))}
                      min={1}
                      max={180}
                      type="number"
                      className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
                    />
                  </label>
                </div>
              </div>
            </section>

            <section className="rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
              <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">list_alt</span>
                Hướng dẫn chi tiết
              </h2>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-outline">
                  Các bước thực hiện
                </span>
                <div className="overflow-hidden rounded-xl border-2 border-outline-variant">
                  <div className="flex gap-2 bg-surface-container px-3 py-2 text-outline">
                    {['format_bold', 'format_italic', 'format_list_bulleted', 'format_list_numbered'].map((icon) => (
                      <span key={icon} className="material-symbols-outlined text-[18px]">{icon}</span>
                    ))}
                  </div>
                  <textarea
                    value={form.instructionContent}
                    onChange={(event) => update('instructionContent', event.target.value)}
                    rows={6}
                    placeholder="Nhập từng bước một..."
                    className="w-full resize-none border-0 bg-white px-4 py-3 outline-none"
                  />
                </div>
              </label>
              <label className="mt-5 block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.05em] text-error">
                  Hướng dẫn an toàn (Cảnh báo)
                </span>
                <textarea
                  value={form.safetyWarning}
                  onChange={(event) => update('safetyWarning', event.target.value)}
                  rows={3}
                  placeholder="Những lưu ý quan trọng để tránh chấn thương..."
                  className="w-full resize-none rounded-xl border-2 border-error/30 bg-error-container/20 px-4 py-3 outline-none focus:border-error"
                />
              </label>
            </section>
          </div>

          <aside className="space-y-6">
            <section className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">pregnant_woman</span>
                Phù hợp cho
              </h2>
              <select
                value={form.trimesterScope}
                onChange={(event) => update('trimesterScope', event.target.value as TrimesterScope)}
                className="w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
              >
                <option value="FIRST">Tam cá nguyệt 1</option>
                <option value="SECOND">Tam cá nguyệt 2</option>
                <option value="THIRD">Tam cá nguyệt 3</option>
                <option value="ALL">Tất cả giai đoạn</option>
              </select>
            </section>

            <section className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
              <div className="mb-4 flex items-start justify-between gap-3">
                <h2 className="m-0 flex items-center gap-2 text-xl font-bold">
                  <span className="material-symbols-outlined text-primary">accessibility_new</span>
                  Phân tích tư thế AI
                </h2>
                <button
                  onClick={() => update('supportsPostureAnalysis', !form.supportsPostureAnalysis)}
                  className={`relative h-7 w-14 rounded-full transition ${
                    form.supportsPostureAnalysis ? 'bg-primary' : 'bg-outline-variant'
                  }`}
                >
                  <span
                    className={`absolute top-1 h-5 w-5 rounded-full bg-white transition ${
                      form.supportsPostureAnalysis ? 'left-8' : 'left-1'
                    }`}
                  />
                </button>
              </div>
              <p className="text-sm text-on-surface-variant">
                Bật tính năng theo dõi và chấm điểm tư thế qua camera trong lúc tập.
              </p>
            </section>

            <section className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
                <span className="material-symbols-outlined text-primary">video_file</span>
                Video mẫu
              </h2>
              <input
                value={form.mediaUrl}
                onChange={(event) => update('mediaUrl', event.target.value)}
                placeholder="https://video-cdn.carebridge.com/ex/yoga.mp4"
                className="mb-4 w-full rounded-xl border-2 border-outline-variant bg-white px-4 py-3 outline-none focus:border-primary-container"
              />
              <div className="grid h-32 place-items-center rounded-xl border-2 border-dashed border-outline-variant bg-surface-container-low text-center text-sm text-on-surface-variant">
                <div>
                  <span className="material-symbols-outlined text-3xl text-primary">cloud_upload</span>
                  <p className="m-0">Kéo thả hoặc click để tải lên</p>
                </div>
              </div>
            </section>
          </aside>
        </div>
      </div>

      <footer className="fixed bottom-0 left-64 right-0 flex items-center justify-between border-t border-outline-variant bg-surface px-8 py-4">
        <button onClick={() => navigate('/content/exercises')} className="rounded-full px-6 py-3 font-semibold text-on-surface">
          Hủy
        </button>
        <div className="flex gap-3">
          <button className="rounded-full bg-surface-container px-6 py-3 font-semibold text-primary">
            Lưu bản nháp
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSaving}
            className="flex items-center gap-2 rounded-full bg-primary px-8 py-3 font-semibold text-on-primary disabled:opacity-60"
          >
            Xem trước & Gửi duyệt
            <span className="material-symbols-outlined text-[18px]">send</span>
          </button>
        </div>
      </footer>
    </div>
  );
}

