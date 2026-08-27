import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  type AnalysisMode,
  type PostureConfigCreateForm,
  type PostureConfigVersionForm,
  type PostureFeedbackLevel,
} from "../models/postureConfig";
import {
  createPostureConfig,
  createPostureConfigVersion,
  fetchPostureConfigVersions,
} from "../services/postureConfigApi";

const initialForm: PostureConfigVersionForm = {
  analysisMode: "VIDEO_BATCH",
  ruleOrModelVersion: "",
  confidenceThreshold: 0.85,
  feedbackLevel: "STANDARD",
  configJson: "",
};

export default function EditPostureConfigPage() {
  const { exerciseId } = useParams();
  const isCreate = !exerciseId;
  const navigate = useNavigate();
  const [targetExerciseId, setTargetExerciseId] = useState(exerciseId ?? "");
  const [form, setForm] = useState<PostureConfigVersionForm>(initialForm);
  const [isLoading, setIsLoading] = useState(!isCreate);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isCreate || !exerciseId) return;
    setIsLoading(true);
    setError(null);
    fetchPostureConfigVersions(exerciseId)
      .then((items) => {
        const current =
          items.find((item) => item.status === "ACTIVE") ?? items[0];
        if (!current) {
          setError("Chưa có cấu hình tư thế cho bài tập này.");
          return;
        }
        setForm({
          analysisMode: current.analysisMode,
          ruleOrModelVersion: current.ruleOrModelVersion ?? "",
          confidenceThreshold: current.confidenceThreshold,
          feedbackLevel: current.feedbackLevel ?? "STANDARD",
          configJson: current.configJson ?? "",
        });
      })
      .catch(() => setError("Không thể tải cấu hình tư thế."))
      .finally(() => setIsLoading(false));
  }, [exerciseId, isCreate]);

  const update = <K extends keyof PostureConfigVersionForm>(
    key: K,
    value: PostureConfigVersionForm[K],
  ) => setForm((current) => ({ ...current, [key]: value }));

  const handleSubmit = async () => {
    if (!targetExerciseId.trim()) {
      setError("Vui lòng nhập Exercise ID thật.");
      return;
    }
    if (isCreate) {
      await createPostureConfig({
        exerciseId: targetExerciseId.trim(),
        ...form,
      } as PostureConfigCreateForm);
    } else {
      await createPostureConfigVersion(targetExerciseId.trim(), form);
    }
    navigate(`/admin/posture-configs/${targetExerciseId.trim()}`);
  };

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải cấu hình...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-8 font-sans text-on-surface">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <button
            onClick={() => navigate("/admin/posture-configs")}
            className="mb-3 flex items-center gap-2 text-sm text-on-surface-variant hover:text-primary"
          >
            <span className="material-symbols-outlined text-[18px]">
              arrow_back
            </span>
            Quay lại danh sách cấu hình
          </button>
          <h1 className="m-0 text-[32px] font-bold">
            {isCreate
              ? "Thêm cấu hình phân tích tư thế"
              : "Chỉnh sửa cấu hình phân tích tư thế"}
          </h1>
        </div>
      </div>

      {error && (
        <div className="mb-4 rounded-2xl bg-error-container p-4 text-sm text-error">
          {error}
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[2fr_1fr]">
        <section className="rounded-[24px] bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
          <h2 className="mb-6 flex items-center gap-2 border-b border-outline-variant pb-4 text-2xl font-bold">
            <span className="material-symbols-outlined text-primary-container">
              tune
            </span>
            Thông số mô hình
          </h2>
          <div className="space-y-6">
            <label className="block">
              <span className="mb-2 block font-semibold text-on-surface-variant">
                Exercise ID
              </span>
              <input
                value={targetExerciseId}
                disabled={!isCreate}
                onChange={(event) => setTargetExerciseId(event.target.value)}
                className="w-full rounded-xl border-2 border-outline-variant bg-surface p-3 outline-none focus:border-primary-container disabled:bg-surface-container-low"
              />
            </label>
            <div className="grid gap-6 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block font-semibold text-on-surface-variant">
                  Chế độ phân tích
                </span>
                <select
                  value={form.analysisMode}
                  onChange={(event) =>
                    update("analysisMode", event.target.value as AnalysisMode)
                  }
                  className="w-full rounded-xl border-2 border-outline-variant bg-surface p-3 outline-none focus:border-primary-container"
                >
                  <option value="REAL_TIME">Thời gian thực</option>
                  <option value="VIDEO_BATCH">Phân tích video tải lên</option>
                  <option value="HYBRID">Hỗn hợp</option>
                  <option value="MODEL_BASED">Pose Landmark</option>
                  <option value="RULE_BASED">Quy tắc an toàn</option>
                </select>
              </label>
              <label className="block">
                <span className="mb-2 block font-semibold text-on-surface-variant">
                  Phiên bản mô hình/quy tắc
                </span>
                <input
                  value={form.ruleOrModelVersion}
                  onChange={(event) =>
                    update("ruleOrModelVersion", event.target.value)
                  }
                  className="w-full rounded-xl border-2 border-outline-variant bg-surface p-3 outline-none focus:border-primary-container"
                />
              </label>
            </div>
            <div>
              <div className="mb-2 flex justify-between">
                <span className="font-semibold text-on-surface-variant">
                  Ngưỡng tin cậy
                </span>
                <span className="font-bold text-primary">
                  {Math.round(form.confidenceThreshold * 100)}%
                </span>
              </div>
              <input
                value={Math.round(form.confidenceThreshold * 100)}
                onChange={(event) =>
                  update(
                    "confidenceThreshold",
                    Number(event.target.value) / 100,
                  )
                }
                min={50}
                max={95}
                type="range"
                className="w-full accent-primary-container"
              />
            </div>
            <label className="block">
              <span className="mb-2 block font-semibold text-on-surface-variant">
                Mức phản hồi
              </span>
              <select
                value={form.feedbackLevel}
                onChange={(event) =>
                  update(
                    "feedbackLevel",
                    event.target.value as PostureFeedbackLevel,
                  )
                }
                className="w-full rounded-xl border-2 border-outline-variant bg-surface p-3 outline-none focus:border-primary-container"
              >
                <option value="BASIC">Cơ bản</option>
                <option value="DETAILED">Chi tiết</option>
                <option value="STRICT">Nghiêm ngặt</option>
                <option value="STANDARD">Tiêu chuẩn</option>
                <option value="LENIENT">Linh hoạt</option>
              </select>
            </label>
            <label className="block">
              <span className="mb-2 block font-semibold text-on-surface-variant">
                Config JSON
              </span>
              <textarea
                value={form.configJson}
                onChange={(event) => update("configJson", event.target.value)}
                rows={8}
                className="w-full resize-none rounded-3xl border-2 border-outline-variant bg-surface p-4 font-mono text-sm outline-none focus:border-primary-container"
              />
            </label>
          </div>
        </section>

        <aside className="space-y-6">
          <section className="rounded-[24px] bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
            <h2 className="mb-5 flex items-center gap-2 border-b border-outline-variant pb-3 text-xl font-bold">
              <span className="material-symbols-outlined text-primary-container">
                save
              </span>
              Lưu thay đổi
            </h2>
            <p className="text-sm text-on-surface-variant">
              Dữ liệu sẽ được gửi lên backend. Màn hình không tự tạo exerciseId
              hoặc thông số mẫu.
            </p>
            <button
              onClick={handleSubmit}
              className="mt-6 flex w-full items-center justify-center gap-2 rounded-full bg-primary-container px-8 py-3 font-semibold text-on-primary shadow-lg"
            >
              <span className="material-symbols-outlined">save</span>
              Lưu cấu hình
            </button>
          </section>
        </aside>
      </div>
    </div>
  );
}
