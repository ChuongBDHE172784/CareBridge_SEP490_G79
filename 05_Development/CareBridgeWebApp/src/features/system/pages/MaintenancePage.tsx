import { Construction, RefreshCw } from 'lucide-react';

export default function MaintenancePage() {
  return (
    <main className="grid min-h-screen place-items-center bg-surface p-6 font-sans text-on-surface">
      <section className="w-full max-w-lg rounded-3xl border border-outline-variant bg-surface-container-low p-8 text-center shadow-xl">
        <div className="mx-auto mb-5 grid h-16 w-16 place-items-center rounded-full bg-primary/15 text-primary">
          <Construction aria-hidden="true" size={30} />
        </div>
        <h1 className="m-0 text-2xl font-bold">CareBridge đang bảo trì</h1>
        <p className="mx-auto mt-3 max-w-md text-sm leading-relaxed text-on-surface-variant">
          Hệ thống đang tạm dừng các chức năng thông thường để bảo trì. Phiên đăng nhập của bạn vẫn được giữ nguyên; vui lòng thử lại sau.
        </p>
        <button
          type="button"
          onClick={() => window.location.replace('/')}
          className="mt-7 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 text-sm font-semibold text-on-primary hover:bg-primary/90"
        >
          <RefreshCw aria-hidden="true" size={17} />
          Kiểm tra lại
        </button>
      </section>
    </main>
  );
}
