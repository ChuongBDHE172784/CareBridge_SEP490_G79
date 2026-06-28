// TODO: Implement from design mockup CB-083 — /build-screen CB-083
export default function AdminDashboardPage() {
  return <PlaceholderPage title="Admin Dashboard" cb="CB-083" />;
}

function PlaceholderPage({ title, cb }: { title: string; cb: string }) {
  return (
    <div className="p-12 text-center font-sans text-on-surface-variant">
      <p className="text-[32px] mb-2">🚧</p>
      <h2 className="text-primary mb-2">{title}</h2>
      <p>
        Chưa được implement. Chạy <code>/build-screen {cb}</code> để tạo giao diện.
      </p>
    </div>
  );
}
