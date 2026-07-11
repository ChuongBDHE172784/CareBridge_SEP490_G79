import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { AdminExercise } from "../models/adminExercise";
import { fetchAdminExercise } from "../services/adminExerciseApi";

export default function ExercisePreviewPage() {
  const navigate = useNavigate();
  const { exerciseId } = useParams();
  const [exercise, setExercise] = useState<AdminExercise | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!exerciseId) {
      setError("Thiếu mã bài tập để xem trước.");
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    fetchAdminExercise(exerciseId)
      .then(setExercise)
      .catch(() => setError("Không thể tải bài tập để xem trước."))
      .finally(() => setIsLoading(false));
  }, [exerciseId]);

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải bản xem trước...
      </div>
    );
  }

  if (error || !exercise) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="mb-4 text-error">{error ?? "Không tìm thấy bài tập."}</p>
        <button
          onClick={() => navigate(-1)}
          className="rounded-full border border-outline-variant px-6 py-2 text-primary"
        >
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background font-sans text-on-surface">
      <header className="flex h-20 shrink-0 items-center justify-between bg-surface px-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <div className="flex items-center gap-4">
          <span className="material-symbols-outlined text-3xl text-primary">
            visibility
          </span>
          <div>
            <h1 className="m-0 text-xl font-bold">Chế độ xem trước</h1>
            <p className="m-0 text-sm text-on-surface-variant">
              Nội dung: {exercise.title}
            </p>
          </div>
        </div>
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 rounded-full bg-primary px-8 py-3 text-sm font-semibold text-on-primary"
        >
          <span className="material-symbols-outlined text-[18px]">
            arrow_back
          </span>
          Quay lại chỉnh sửa
        </button>
      </header>

      <main className="grid flex-1 grid-cols-[1fr_2fr] overflow-hidden bg-surface-container-low">
        <aside className="space-y-6 overflow-y-auto border-r border-outline-variant bg-surface-bright p-8">
          <div className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-4 border-b border-outline-variant pb-2 text-xl font-bold">
              Thông tin bài tập
            </h2>
            <div className="space-y-4 text-sm">
              <div>
                <p className="mb-1 text-xs font-bold uppercase tracking-[0.05em] text-outline">
                  Tiêu đề
                </p>
                <div className="rounded-xl border border-outline-variant bg-surface-container p-3">
                  {exercise.title}
                </div>
              </div>
              <div>
                <p className="mb-1 text-xs font-bold uppercase tracking-[0.05em] text-outline">
                  Thời lượng ước tính
                </p>
                <div className="rounded-xl border border-outline-variant bg-surface-container p-3">
                  {exercise.durationMinutes} phút
                </div>
              </div>
            </div>
          </div>
        </aside>

        <section className="grid place-items-center bg-background/80 backdrop-blur">
          <div className="relative flex h-[760px] w-[360px] flex-col overflow-hidden rounded-[40px] border-[12px] border-inverse-surface bg-white shadow-[0_24px_60px_rgba(45,42,40,0.15)]">
            <div className="absolute left-1/2 top-0 z-10 h-7 w-36 -translate-x-1/2 rounded-b-3xl bg-inverse-surface" />
            <div className="flex h-12 items-end justify-between px-6 pb-2 text-sm font-bold">
              <span>9:41</span>
              <span className="material-symbols-outlined text-[16px]">
                signal_cellular_4_bar
              </span>
            </div>
            {exercise.mediaUrl ? (
              <img
                src={exercise.mediaUrl}
                alt=""
                className="h-48 w-full object-cover"
              />
            ) : (
              <div className="grid h-48 place-items-center bg-surface-container text-outline">
                <span className="material-symbols-outlined text-[40px]">
                  image_not_supported
                </span>
              </div>
            )}
            <div className="flex-1 overflow-y-auto bg-surface-container-lowest p-5">
              <h2 className="m-0 text-xl font-bold">{exercise.title}</h2>
              {exercise.description && (
                <p className="text-sm leading-6 text-on-surface-variant">
                  {exercise.description}
                </p>
              )}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
