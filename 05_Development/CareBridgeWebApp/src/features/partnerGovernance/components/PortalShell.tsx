import type { ReactNode } from 'react';

const partnerLinks = [['dashboard', 'Tổng quan'], ['account_circle', 'Hồ sơ'], ['medical_services', 'Dịch vụ'], ['campaign', 'Chiến dịch'], ['trending_up', 'Hiệu quả']];
const adminLinks = [['dashboard', 'Tổng quan'], ['group', 'Người dùng'], ['handshake', 'Đối tác'], ['article', 'Nội dung'], ['security', 'An toàn']];

export function PortalShell({ title, children, admin = false }: { title: string; children: ReactNode; admin?: boolean }) {
  const links = admin ? adminLinks : partnerLinks;
  return <div className="min-h-screen bg-background font-sans text-on-surface">
    <aside className="fixed inset-y-0 hidden w-64 border-r border-outline-variant bg-surface-container-low p-6 lg:block">
      <p className="text-2xl font-bold text-primary">{admin ? 'CareTech Admin' : 'CareBridge'}</p>
      <p className="mt-1 text-xs text-on-surface-variant">{admin ? 'HỆ THỐNG QUẢN LÝ' : 'B2B Partner Portal'}</p>
      <nav className="mt-12 space-y-2">{links.map(([icon, label]) => <button key={label} className="flex w-full items-center gap-3 rounded-full px-4 py-3 text-left text-sm hover:bg-primary-container hover:text-white"><span className="material-symbols-outlined">{icon}</span>{label}</button>)}</nav>
    </aside>
    <main className="lg:ml-64"><header className="flex h-20 items-center justify-between border-b border-outline-variant bg-surface px-6 md:px-10"><h1 className="text-xl font-bold text-primary">{title}</h1><div className="flex items-center gap-4"><span className="material-symbols-outlined">notifications</span><span className="material-symbols-outlined">help</span><span className="h-10 w-10 rounded-full bg-primary-container" /></div></header><div className="mx-auto max-w-[1280px] p-6 md:p-10">{children}</div></main>
  </div>;
}

export function Notice({ children, error = false }: { children: ReactNode; error?: boolean }) { return <div className={`mb-5 flex items-center gap-2 rounded-2xl px-4 py-3 text-sm ${error ? 'bg-error-container text-error' : 'bg-primary-container text-on-primary'}`}><span className="material-symbols-outlined">{error ? 'error' : 'check_circle'}</span>{children}</div>; }
