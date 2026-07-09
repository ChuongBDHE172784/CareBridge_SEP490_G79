export default function ContentApprovalQueuePage() {
  return (
    <div className="p-8 font-sans">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="m-0 text-2xl font-bold text-on-surface">
            Hàng đợi phê duyệt
          </h1>
          <p className="mt-1 text-sm text-outline">
            Danh sách nội dung đang chờ kiểm duyệt cuối cùng.
          </p>
        </div>
        <div className="flex gap-2.5">
          <button
            disabled
            className="flex items-center gap-1.5 rounded-full border border-outline-variant bg-transparent px-5 py-2.5 text-sm font-semibold text-on-surface-variant opacity-50"
          >
            <span className="material-symbols-outlined text-lg">
              filter_list
            </span>
            Lọc
          </button>
          <button
            disabled
            className="flex items-center gap-1.5 rounded-full border border-outline-variant bg-transparent px-5 py-2.5 text-sm font-semibold text-on-surface-variant opacity-50"
          >
            <span className="material-symbols-outlined text-lg">sort</span>
            Sắp xếp
          </button>
        </div>
      </div>

      <div className="rounded-2xl bg-surface p-10 text-center shadow-md">
        <span className="material-symbols-outlined text-5xl text-outline">
          inbox
        </span>
        <h2 className="mt-4 text-xl font-bold text-on-surface">
          Chưa có dữ liệu phê duyệt
        </h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-on-surface-variant">
          Backend hiện chưa cung cấp endpoint liệt kê nội dung ở trạng thái chờ
          duyệt, nên màn hình không hiển thị dữ liệu mẫu cố định.
        </p>
      </div>
    </div>
  );
}
