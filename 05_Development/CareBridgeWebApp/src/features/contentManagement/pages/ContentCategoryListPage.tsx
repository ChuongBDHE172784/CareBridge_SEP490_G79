import { useEffect, useMemo, useState } from 'react';
import {
  createContentCategory,
  fetchContentCategories,
  updateContentCategory,
} from '../services/contentApi';
import type { CommunityTopic } from '../models/content';

export default function ContentCategoryListPage() {
  const [items, setItems] = useState<CommunityTopic[]>([]);
  const [search, setSearch] = useState('');
  const [name, setName] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchContentCategories(true)
      .then(setItems)
      .catch(() => setError('Không thể tải danh mục. Vui lòng thử lại.'))
      .finally(() => setIsLoading(false));
  }, []);

  const shownItems = useMemo(
    () => items.filter((item) => item.name.toLowerCase().includes(search.trim().toLowerCase())),
    [items, search],
  );

  async function addCategory() {
    if (!name.trim()) return;
    setIsSaving(true);
    setError(null);
    try {
      const created = await createContentCategory({ name: name.trim(), icon: 'label' });
      setItems((current) => [...current, created]);
      setName('');
      setIsCreateOpen(false);
    } catch {
      setError('Không thể tạo danh mục. Vui lòng kiểm tra tên danh mục và thử lại.');
    } finally {
      setIsSaving(false);
    }
  }

  async function toggleCategory(item: CommunityTopic) {
    setError(null);
    try {
      const updated = await updateContentCategory(item.id, { isHidden: !item.isHidden });
      setItems((current) => current.map((entry) => (entry.id === item.id ? updated : entry)));
    } catch {
      setError('Không thể cập nhật trạng thái danh mục. Vui lòng thử lại.');
    }
  }

  return (
    <div className="p-8 font-sans text-on-surface">
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="m-0 text-[26px] font-bold">Quản lý danh mục</h1>
          <p className="mt-1 text-sm text-on-surface-variant">
            Tạo, cập nhật và ẩn các danh mục dùng cho nội dung CareBridge.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center gap-2 rounded-full bg-primary-container px-6 py-3 text-sm font-semibold text-on-primary"
        >
          <span className="material-symbols-outlined text-lg">add</span>
          Thêm danh mục
        </button>
      </div>

      {error && <div className="mb-5 rounded-xl bg-error-container px-4 py-3 text-sm text-error">{error}</div>}

      <section className="rounded-2xl bg-surface p-6 shadow-md">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div className="relative w-full max-w-sm">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Tìm theo tên danh mục..."
              className="w-full rounded-2xl border border-outline-variant bg-surface py-2.5 pl-10 pr-4 text-sm outline-none focus:border-primary"
            />
          </div>
          <span className="text-sm text-on-surface-variant">{shownItems.length} danh mục</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[680px] border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['DANH MỤC', 'MÔ TẢ', 'THỨ TỰ', 'TRẠNG THÁI', 'HÀNH ĐỘNG'].map((label) => (
                  <th key={label} className="px-2 py-3 text-[11px] font-semibold tracking-[0.05em] text-outline">
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={5} className="py-12 text-center text-outline">Đang tải danh mục...</td></tr>
              ) : shownItems.length === 0 ? (
                <tr><td colSpan={5} className="py-12 text-center text-outline">Không tìm thấy danh mục phù hợp.</td></tr>
              ) : shownItems.map((item) => (
                <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                  <td className="px-2 py-3.5">
                    <div className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-primary">folder</span>
                      <span className="font-semibold text-sm">{item.name}</span>
                    </div>
                  </td>
                  <td className="max-w-sm px-2 py-3.5 text-[13px] text-on-surface-variant">{item.description || '—'}</td>
                  <td className="px-2 py-3.5 text-sm text-on-surface-variant">{item.sortOrder}</td>
                  <td className="px-2 py-3.5">
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${item.isHidden ? 'bg-surface-container-high text-on-surface-variant' : 'bg-[#E6F4EA] text-[#137333]'}`}>
                      {item.isHidden ? 'Đã ẩn' : 'Đang hiển thị'}
                    </span>
                  </td>
                  <td className="px-2 py-3.5">
                    <button
                      type="button"
                      aria-label={item.isHidden ? `Hiện ${item.name}` : `Ẩn ${item.name}`}
                      onClick={() => toggleCategory(item)}
                      className={`relative h-6 w-11 rounded-full transition-colors ${item.isHidden ? 'bg-outline' : 'bg-primary'}`}
                    >
                      <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-transform ${item.isHidden ? 'left-0.5' : 'left-5'}`} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {isCreateOpen && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-scrim/30 p-4">
          <form
            onSubmit={(event) => { event.preventDefault(); void addCategory(); }}
            className="w-full max-w-md rounded-2xl bg-surface p-7 shadow-xl"
          >
            <h2 className="text-xl font-bold">Thêm danh mục</h2>
            <p className="mt-1 text-sm text-on-surface-variant">Danh mục sẽ dùng chung cho nội dung trong hệ thống.</p>
            <label className="mt-5 block text-sm font-semibold">Tên danh mục</label>
            <input
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Ví dụ: Dinh dưỡng thai kỳ"
              className="mt-2 w-full rounded-xl border border-outline-variant p-3 outline-none focus:border-primary"
            />
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setIsCreateOpen(false)} className="rounded-full px-5 py-3 text-sm font-semibold text-on-surface-variant">Hủy</button>
              <button disabled={isSaving} className="rounded-full bg-primary px-5 py-3 text-sm font-semibold text-on-primary disabled:opacity-60">
                {isSaving ? 'Đang lưu...' : 'Lưu danh mục'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
