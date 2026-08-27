import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  difficultyLabels,
  statusLabels,
  trimesterLabels,
  type AdminExercise,
} from "../models/adminExercise";
import { fetchAdminExercise } from "../services/adminExerciseApi";

export default function PregnancyExerciseDetailPage() {
  const { exerciseId } = useParams();
  const navigate = useNavigate();
  const [exercise, setExercise] = useState<AdminExercise | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!exerciseId) {
      setError("Thiếu mã bài tập.");
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    fetchAdminExercise(exerciseId)
      .then(setExercise)
      .catch(() => setError("Không thể tải chi tiết bài tập."))
      .finally(() => setIsLoading(false));
  }, [exerciseId]);

  const steps = useMemo(
    () =>
      (exercise?.instructionContent || "")
        .split(/\n|\./)
        .map((step) => step.trim())
        .filter(Boolean),
    [exercise?.instructionContent],
  );

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải bài tập...
      </div>
    );
  }

  if (error || !exercise) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="mb-4 text-error">{error ?? "Không tìm thấy bài tập."}</p>
        <button
          onClick={() => navigate("/content/exercises")}
          className="rounded-full border border-outline-variant px-6 py-2 text-primary"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-8 font-sans text-on-surface">
      <section className="mb-6 rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="mb-3 flex gap-2">
              <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold text-primary">
                {statusLabels[exercise.status]}
              </span>
              <span className="rounded-full bg-surface-container px-3 py-1 text-xs text-on-surface-variant">
                ID: {exercise.exerciseId.slice(0, 8)}
              </span>
            </div>
            <h1 className="m-0 text-[32px] font-bold">{exercise.title}</h1>
            {exercise.description && (
              <p className="mt-3 max-w-3xl text-base text-on-surface-variant">
                {exercise.description}
              </p>
            )}
          </div>
          <div className="flex gap-3">
            <button
              onClick={() =>
                navigate(`/content/exercises/${exercise.exerciseId}/preview`)
              }
              className="flex items-center gap-2 rounded-full border-2 border-outline-variant px-5 py-3 text-sm font-semibold text-primary"
            >
              <span className="material-symbols-outlined text-[18px]">
                smartphone
              </span>
              Xem trước
            </button>
            <button
              onClick={() =>
                navigate(`/content/exercises/${exercise.exerciseId}/edit`)
              }
              className="flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary"
            >
              <span className="material-symbols-outlined text-[18px]">
                edit
              </span>
              Chỉnh sửa
            </button>
          </div>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <section className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
          <h2 className="mb-4 text-xl font-bold">Media hướng dẫn</h2>
          <div className="relative aspect-video overflow-hidden rounded-xl bg-surface-container">
            {exercise.mediaUrl ? (
              <img
                src={exercise.mediaUrl}
                alt=""
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="grid h-full place-items-center text-outline">
                <span className="material-symbols-outlined text-[48px]">
                  image_not_supported
                </span>
              </div>
            )}
          </div>
        </section>

        <section className="rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
          <h2 className="mb-5 text-xl font-bold">Chi tiết bài tập</h2>
          {[
            ["timer", "Thời gian", `${exercise.durationMinutes} phút`],
            [
              "fitness_center",
              "Độ khó",
              difficultyLabels[exercise.difficultyLevel],
            ],
            [
              "calendar_month",
              "Tam cá nguyệt",
              trimesterLabels[exercise.trimesterScope],
            ],
            [
              "videocam",
              "AI posture",
              exercise.supportsPostureAnalysis ? "Có hỗ trợ" : "Không hỗ trợ",
            ],
          ].map(([icon, label, value]) => (
            <div
              key={label}
              className="mb-4 flex gap-3 border-b border-outline-variant pb-4 last:border-0"
            >
              <span className="grid h-10 w-10 place-items-center rounded-full bg-surface-container text-primary">
                <span className="material-symbols-outlined">{icon}</span>
              </span>
              <div>
                <p className="m-0 text-xs uppercase tracking-[0.05em] text-outline">
                  {label}
                </p>
                <p className="m-0 mt-1 font-semibold">{value}</p>
              </div>
            </div>
          ))}
        </section>
      </div>

      <section className="mt-6 rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <div className="grid gap-10 lg:grid-cols-2">
          <div>
            <h2 className="mb-5 flex items-center gap-2 text-xl font-bold">
              <span className="material-symbols-outlined text-primary">
                format_list_numbered
              </span>
              Hướng dẫn thực hiện
            </h2>
            {steps.length === 0 ? (
              <p className="text-sm text-on-surface-variant">
                Chưa có hướng dẫn thực hiện.
              </p>
            ) : (
              <div className="space-y-4">
                {steps.map((step, index) => (
                  <div key={`${index}-${step}`} className="flex gap-3">
                    <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-primary text-sm font-bold text-white">
                      {index + 1}
                    </span>
                    <p className="m-0 text-sm leading-6 text-on-surface-variant">
                      {step}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div>
            <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
              <span className="material-symbols-outlined text-primary">
                warning
              </span>
              Lưu ý an toàn
            </h2>
            <div className="rounded-xl border-l-4 border-primary bg-surface-container-low p-4 text-sm leading-6 text-on-surface-variant">
              {exercise.safetyWarning || "Chưa có lưu ý an toàn."}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
