import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  type AdminExercise,
  type AdminExerciseForm,
  type DifficultyLevel,
  type TrimesterScope,
} from "../models/adminExercise";
import {
  fetchAdminExercise,
  updateAdminExercise,
} from "../services/adminExerciseApi";

function toForm(exercise: AdminExercise): AdminExerciseForm {
  return {
    title: exercise.title,
    description: exercise.description ?? "",
    trimesterScope: exercise.trimesterScope,
    difficultyLevel: exercise.difficultyLevel,
    durationMinutes: exercise.durationMinutes,
    instructionContent: exercise.instructionContent ?? "",
    mediaUrl: exercise.mediaUrl ?? "",
    safetyWarning: exercise.safetyWarning ?? "",
    supportsPostureAnalysis: exercise.supportsPostureAnalysis,
  };
}

export default function EditPregnancyExercisePage() {
  const { exerciseId } = useParams();
  const navigate = useNavigate();
  const [exercise, setExercise] = useState<AdminExercise | null>(null);
  const [form, setForm] = useState<AdminExerciseForm | null>(null);
  const [changeNote, setChangeNote] = useState("");
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
      .then((data) => {
        setExercise(data);
        setForm(toForm(data));
      })
      .catch(() => setError("Không thể tải bài tập để chỉnh sửa."))
      .finally(() => setIsLoading(false));
  }, [exerciseId]);

  const update = <K extends keyof AdminExerciseForm>(
    key: K,
    value: AdminExerciseForm[K],
  ) => {
    setForm((current) => (current ? { ...current, [key]: value } : current));
  };

  const handleUpdate = async () => {
    if (!exercise || !form) return;
    await updateAdminExercise(exercise.exerciseId, form);
    navigate(`/content/exercises/${exercise.exerciseId}`);
  };

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải bài tập...
      </div>
    );
  }

  if (error || !exercise || !form) {
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
      <div className="mb-8 flex items-start justify-between gap-6">
        <div>
          <h1 className="m-0 text-[28px] font-bold">{exercise.title}</h1>
          <div className="mt-3 flex flex-wrap gap-3 text-xs">
            <span className="rounded-full bg-surface-container px-3 py-1 text-on-surface-variant">
              Phiên bản: v{exercise.versionNo}
            </span>
            <span className="px-3 py-1 text-on-surface-variant">
              Cập nhật cuối:{" "}
              {exercise.updatedAt
                ? new Date(exercise.updatedAt).toLocaleString("vi-VN")
                : "-"}
            </span>
          </div>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => navigate(-1)}
            className="rounded-full border-2 border-outline-variant px-6 py-3 font-semibold"
          >
            Hủy
          </button>
          <button
            onClick={handleUpdate}
            className="rounded-full bg-primary px-6 py-3 font-semibold text-on-primary"
          >
            Cập nhật phiên bản
          </button>
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[2fr_1fr]">
        <div className="space-y-6">
          <section className="rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-6 text-xl font-bold">Thông tin cơ bản</h2>
            <div className="space-y-5">
              <input
                value={form.title}
                onChange={(event) => update("title", event.target.value)}
                className="w-full rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
              />
              <textarea
                value={form.description}
                onChange={(event) => update("description", event.target.value)}
                rows={3}
                className="w-full resize-none rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
              />
              <div className="grid gap-5 sm:grid-cols-2">
                <select
                  value={form.trimesterScope}
                  onChange={(event) =>
                    update(
                      "trimesterScope",
                      event.target.value as TrimesterScope,
                    )
                  }
                  className="rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
                >
                  <option value="FIRST">3 tháng đầu</option>
                  <option value="SECOND">3 tháng giữa</option>
                  <option value="THIRD">3 tháng cuối</option>
                  <option value="ALL">Tất cả</option>
                </select>
                <input
                  value={form.durationMinutes}
                  onChange={(event) =>
                    update("durationMinutes", Number(event.target.value))
                  }
                  type="number"
                  className="rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
                />
              </div>
              <select
                value={form.difficultyLevel}
                onChange={(event) =>
                  update(
                    "difficultyLevel",
                    event.target.value as DifficultyLevel,
                  )
                }
                className="w-full rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
              >
                <option value="EASY">Dễ</option>
                <option value="MEDIUM">Trung bình</option>
                <option value="HARD">Nâng cao</option>
              </select>
            </div>
          </section>

          <section className="rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
              <span className="material-symbols-outlined text-primary">
                format_list_numbered
              </span>
              Các bước thực hiện
            </h2>
            <textarea
              value={form.instructionContent}
              onChange={(event) =>
                update("instructionContent", event.target.value)
              }
              rows={7}
              className="w-full resize-none rounded-xl border-2 border-outline-variant bg-surface-bright p-4 outline-none focus:border-primary-container"
            />
          </section>
        </div>

        <aside className="space-y-6">
          <section className="rounded-2xl bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
              <span className="material-symbols-outlined text-primary">
                perm_media
              </span>
              Đa phương tiện
            </h2>
            <div className="mb-5 aspect-video overflow-hidden rounded-xl border-2 border-outline-variant bg-surface-container">
              {form.mediaUrl ? (
                <img
                  src={form.mediaUrl}
                  alt=""
                  className="h-full w-full object-cover"
                />
              ) : null}
            </div>
            <input
              value={form.mediaUrl}
              onChange={(event) => update("mediaUrl", event.target.value)}
              className="w-full rounded-xl border-2 border-outline-variant p-3 outline-none focus:border-primary-container"
            />
          </section>

          <section className="rounded-2xl border border-outline-variant bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
              <span className="material-symbols-outlined text-primary">
                commit
              </span>
              Ghi chú phiên bản
            </h2>
            <textarea
              value={changeNote}
              onChange={(event) => setChangeNote(event.target.value)}
              rows={4}
              className="w-full resize-none rounded-xl border-2 border-outline-variant bg-surface-bright p-3 outline-none focus:border-primary-container"
            />
          </section>
        </aside>
      </div>
    </div>
  );
}
