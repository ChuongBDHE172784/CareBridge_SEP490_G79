import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  analysisModeLabels,
  feedbackLevelLabels,
  type AdminPostureConfig,
} from "../models/postureConfig";
import { fetchPostureConfigVersions } from "../services/postureConfigApi";

export default function PostureConfigDetailPage() {
  const { exerciseId } = useParams();
  const navigate = useNavigate();
  const [config, setConfig] = useState<AdminPostureConfig | null>(null);
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
    fetchPostureConfigVersions(exerciseId)
      .then((items) =>
        setConfig(
          items.find((item) => item.status === "ACTIVE") ?? items[0] ?? null,
        ),
      )
      .catch(() => setError("Không thể tải cấu hình tư thế."))
      .finally(() => setIsLoading(false));
  }, [exerciseId]);

  const parsedConfig = useMemo(() => {
    if (!config?.configJson) return null;
    try {
      return JSON.stringify(JSON.parse(config.configJson), null, 2);
    } catch {
      return config.configJson;
    }
  }, [config?.configJson]);

  if (isLoading) {
    return (
      <div className="py-12 text-center font-sans text-outline">
        Đang tải cấu hình...
      </div>
    );
  }

  if (error || !config) {
    return (
      <div className="py-12 text-center font-sans">
        <p className="mb-4 text-error">
          {error ?? "Chưa có cấu hình cho bài tập này."}
        </p>
        <button
          onClick={() => navigate("/posture-configs")}
          className="rounded-full border border-outline-variant px-6 py-2 text-primary"
        >
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-8 font-sans text-on-surface">
      <section className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-[24px] bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">
              analytics
            </span>
            <span className="text-xs font-bold uppercase tracking-[0.12em] text-primary">
              Chi tiết cấu hình - CB-197
            </span>
          </div>
          <h1 className="m-0 text-[32px] font-bold">Cấu hình tư thế</h1>
          <p className="mt-2 max-w-3xl text-on-surface-variant">
            Exercise ID: {config.exerciseId}
          </p>
        </div>
        <button
          onClick={() => navigate(`/posture-configs/${config.exerciseId}/edit`)}
          className="flex items-center gap-2 rounded-full bg-primary px-6 py-3 font-semibold text-on-primary"
        >
          <span className="material-symbols-outlined text-[18px]">edit</span>
          Chỉnh sửa
        </button>
      </section>

      <div className="grid gap-6 lg:grid-cols-[1fr_2fr]">
        <section className="rounded-[24px] bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
          <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
            <span className="material-symbols-outlined text-primary">info</span>
            Thông tin chung
          </h2>
          {[
            ["Trạng thái", config.status],
            ["Phiên bản", config.ruleOrModelVersion ?? "-"],
            ["Chế độ phân tích", analysisModeLabels[config.analysisMode]],
            [
              "Mức phản hồi",
              config.feedbackLevel
                ? feedbackLevelLabels[config.feedbackLevel]
                : "-",
            ],
            [
              "Ngưỡng tin cậy",
              `${Math.round(config.confidenceThreshold * 100)}%`,
            ],
            ["Tạo bởi", config.configuredBy ?? "-"],
          ].map(([label, value]) => (
            <div
              key={label}
              className="flex justify-between border-b border-outline-variant/50 py-3 text-sm last:border-0"
            >
              <span className="text-on-surface-variant">{label}</span>
              <span className="font-semibold">{value}</span>
            </div>
          ))}
        </section>

        <section className="rounded-[24px] bg-surface p-8 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
          <h2 className="mb-6 flex items-center gap-2 text-xl font-bold">
            <span className="material-symbols-outlined text-primary">rule</span>
            Cấu hình JSON
          </h2>
          {parsedConfig ? (
            <pre className="overflow-auto rounded-xl bg-surface-container-low p-4 text-sm text-on-surface">
              {parsedConfig}
            </pre>
          ) : (
            <p className="text-sm text-on-surface-variant">
              Chưa có cấu hình JSON.
            </p>
          )}
        </section>
      </div>
    </div>
  );
}
