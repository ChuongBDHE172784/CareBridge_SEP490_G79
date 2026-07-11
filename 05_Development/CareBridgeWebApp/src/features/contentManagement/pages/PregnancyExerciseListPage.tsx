import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  difficultyLabels,
  statusLabels,
  trimesterLabels,
  type AdminExercise,
  type DifficultyLevel,
  type ExerciseStatus,
  type TrimesterScope,
} from "../models/adminExercise";
import {
  disableAdminExercise,
  fetchAdminExercises,
} from "../services/adminExerciseApi";

export default function PregnancyExerciseListPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<AdminExercise[]>([]);
  const [status, setStatus] = useState<ExerciseStatus | "">("");
  const [trimester, setTrimester] = useState<TrimesterScope | "">("");
  const [difficulty, setDifficulty] = useState<DifficultyLevel | "">("");
  const [total, setTotal] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);
    fetchAdminExercises({
      status: status || undefined,
      trimester: trimester || undefined,
      difficulty: difficulty || undefined,
      page: 0,
      size: 20,
    })
      .then((data) => {
        if (!active) return;
        setItems(data.data);
        setTotal(data.totalElements);
      })
      .catch(() => {
        if (active) {
          setItems([]);
          setTotal(0);
          setError("Không thể tải danh sách bài tập.");
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [status, trimester, difficulty]);

  const handleDisable = async (exerciseId: string) => {
    setItems((current) =>
      current.map((item) =>
        item.exerciseId === exerciseId ? { ...item, status: "ARCHIVED" } : item,
      ),
    );
    try {
      await disableAdminExercise(exerciseId);
    } catch {
      setError("Không thể ẩn bài tập. Vui lòng tải lại dữ liệu.");
    }
  };

  return (
    <div className="min-h-screen bg-background p-8 font-sans text-on-surface">
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <h1 className="m-0 text-[28px] font-bold text-on-surface">
            Danh sách Bài tập
          </h1>
          <p className="mt-1 text-sm text-on-surface-variant">
            Quản lý nội dung bài tập thai kỳ và AI Pose.
          </p>
        </div>
        <button
          onClick={() => navigate("/content/exercises/create")}
          className="flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary shadow-[0_8px_24px_rgba(132,81,67,0.18)] transition hover:bg-surface-tint"
        >
          <span className="material-symbols-outlined text-[20px]">add</span>
          Thêm bài tập
        </button>
      </div>

      <div className="mb-6 flex flex-wrap gap-3 rounded-2xl bg-surface p-4 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        <select
          value={trimester}
          onChange={(event) =>
            setTrimester(event.target.value as TrimesterScope | "")
          }
          className="rounded-xl border-2 border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm outline-none focus:border-primary-container"
        >
          <option value="">Tất cả Tam cá nguyệt</option>
          <option value="FIRST">Tam cá nguyệt 1</option>
          <option value="SECOND">Tam cá nguyệt 2</option>
          <option value="THIRD">Tam cá nguyệt 3</option>
          <option value="ALL">Tất cả giai đoạn</option>
        </select>
        <select
          value={difficulty}
          onChange={(event) =>
            setDifficulty(event.target.value as DifficultyLevel | "")
          }
          className="rounded-xl border-2 border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm outline-none focus:border-primary-container"
        >
          <option value="">Độ khó</option>
          <option value="EASY">Dễ</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="HARD">Nâng cao</option>
        </select>
        <select
          value={status}
          onChange={(event) =>
            setStatus(event.target.value as ExerciseStatus | "")
          }
          className="rounded-xl border-2 border-outline-variant bg-surface-container-lowest px-4 py-2 text-sm outline-none focus:border-primary-container"
        >
          <option value="">Trạng thái</option>
          <option value="PUBLISHED">Đã xuất bản</option>
          <option value="DRAFT">Bản nháp</option>
          <option value="ARCHIVED">Đã lưu trữ</option>
        </select>
      </div>

      <div className="overflow-hidden rounded-2xl bg-surface p-6 shadow-[0_4px_20px_rgba(201,140,123,0.08)]">
        {isLoading ? (
          <div className="py-12 text-center text-outline">
            Đang tải bài tập...
          </div>
        ) : error ? (
          <div className="py-12 text-center text-error">{error}</div>
        ) : items.length === 0 ? (
          <div className="py-12 text-center text-outline">
            Chưa có bài tập nào.
          </div>
        ) : (
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-outline-variant text-[11px] uppercase tracking-[0.05em] text-outline">
                <th className="pb-4">Hình ảnh</th>
                <th className="pb-4">Tiêu đề</th>
                <th className="pb-4">Tam cá nguyệt</th>
                <th className="pb-4">Độ khó</th>
                <th className="pb-4">Thời lượng</th>
                <th className="pb-4">Chế độ AI</th>
                <th className="pb-4">Trạng thái</th>
                <th className="pb-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  key={item.exerciseId}
                  className="border-b border-outline-variant/60 hover:bg-surface-container-low"
                >
                  <td className="py-4">
                    {item.mediaUrl ? (
                      <img
                        src={item.mediaUrl}
                        alt=""
                        className="h-12 w-16 rounded-lg object-cover"
                      />
                    ) : (
                      <div className="grid h-12 w-16 place-items-center rounded-lg bg-surface-container text-outline">
                        <span className="material-symbols-outlined text-[20px]">
                          image_not_supported
                        </span>
                      </div>
                    )}
                  </td>
                  <td className="py-4">
                    <div className="font-semibold">{item.title}</div>
                    <div className="mt-1 max-w-[260px] truncate text-xs text-on-surface-variant">
                      {item.description}
                    </div>
                  </td>
                  <td className="py-4 text-sm text-on-surface-variant">
                    {trimesterLabels[item.trimesterScope]}
                  </td>
                  <td className="py-4 text-sm text-on-surface-variant">
                    {difficultyLabels[item.difficultyLevel]}
                  </td>
                  <td className="py-4 text-sm text-on-surface-variant">
                    {item.durationMinutes} phút
                  </td>
                  <td className="py-4">
                    {item.supportsPostureAnalysis ? (
                      <span className="inline-flex items-center gap-1 rounded-lg bg-surface-container px-2 py-1 text-xs font-semibold text-primary">
                        <span className="material-symbols-outlined text-[15px]">
                          videocam
                        </span>
                        AI Live
                      </span>
                    ) : (
                      <span className="text-sm text-outline">-</span>
                    )}
                  </td>
                  <td className="py-4">
                    <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold text-primary">
                      {statusLabels[item.status]}
                    </span>
                  </td>
                  <td className="py-4 text-right">
                    <button
                      onClick={() =>
                        navigate(`/content/exercises/${item.exerciseId}`)
                      }
                      className="rounded-lg p-1.5 text-on-surface-variant hover:bg-surface-container hover:text-primary"
                      title="Xem chi tiết"
                    >
                      <span className="material-symbols-outlined text-[20px]">
                        visibility
                      </span>
                    </button>
                    <button
                      onClick={() =>
                        navigate(`/content/exercises/${item.exerciseId}/edit`)
                      }
                      className="rounded-lg p-1.5 text-on-surface-variant hover:bg-surface-container hover:text-primary"
                      title="Chỉnh sửa"
                    >
                      <span className="material-symbols-outlined text-[20px]">
                        edit
                      </span>
                    </button>
                    <button
                      onClick={() => handleDisable(item.exerciseId)}
                      className="rounded-lg p-1.5 text-on-surface-variant hover:bg-error-container hover:text-error"
                      title="Ẩn khỏi danh mục"
                    >
                      <span className="material-symbols-outlined text-[20px]">
                        delete
                      </span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="mt-5 flex items-center justify-between border-t border-outline-variant pt-4 text-sm text-on-surface-variant">
          <span>
            Hiển thị 1-{items.length} trong số {total} bài tập
          </span>
          <div className="flex gap-2">
            <button className="grid h-8 w-8 place-items-center rounded-full border border-outline-variant">
              <span className="material-symbols-outlined text-[18px]">
                chevron_left
              </span>
            </button>
            <button className="grid h-8 w-8 place-items-center rounded-full bg-primary-container font-bold text-white">
              1
            </button>
            <button className="grid h-8 w-8 place-items-center rounded-full border border-outline-variant">
              2
            </button>
            <button className="grid h-8 w-8 place-items-center rounded-full border border-outline-variant">
              <span className="material-symbols-outlined text-[18px]">
                chevron_right
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
