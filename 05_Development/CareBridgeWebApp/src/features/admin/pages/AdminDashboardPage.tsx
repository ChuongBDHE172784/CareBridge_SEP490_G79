// TODO: Implement from design mockup CB-083 — /build-screen CB-083
export default function AdminDashboardPage() {
  return <PlaceholderPage title="Admin Dashboard" cb="CB-083" />;
}

function PlaceholderPage({ title, cb }: { title: string; cb: string }) {
  return (
    <div style={{ padding: 48, textAlign: 'center', fontFamily: "'Lexend', sans-serif", color: '#524440' }}>
      <p style={{ fontSize: 32, marginBottom: 8 }}>🚧</p>
      <h2 style={{ color: '#845143', marginBottom: 8 }}>{title}</h2>
      <p>Chưa được implement. Chạy <code>/build-screen {cb}</code> để tạo giao diện.</p>
    </div>
  );
}
