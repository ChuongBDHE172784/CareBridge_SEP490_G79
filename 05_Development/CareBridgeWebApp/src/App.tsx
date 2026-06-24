import { useState } from 'react';
import ModerationQueuePage from './features/moderation/pages/ModerationQueuePage';
import ManageTopicsPage from './features/community/pages/ManageTopicsPage';
import CreateContentPage from './features/contentManagement/pages/CreateContentPage';
import CreatePartnerProfilePage from './features/partnerGovernance/pages/CreatePartnerProfilePage';

type Tab = 'moderation' | 'topics' | 'content' | 'partner';

const tabs: { id: Tab; label: string }[] = [
  { id: 'moderation', label: 'Moderation Queue' },
  { id: 'topics', label: 'Manage Topics' },
  { id: 'content', label: 'Create Content' },
  { id: 'partner', label: 'Create Partner' },
];

const navStyle: React.CSSProperties = {
  display: 'flex', gap: 4, padding: '12px 24px', background: '#fff8f0',
  borderBottom: '1px solid #ffe4c4', boxShadow: '0 2px 12px rgba(255,180,120,0.15)',
  fontFamily: 'system-ui, sans-serif',
};

const tabBtn = (active: boolean): React.CSSProperties => ({
  padding: '9px 20px', borderRadius: 12, border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: 600,
  background: active ? 'linear-gradient(135deg, #ff9a56, #ff6b35)' : 'transparent',
  color: active ? 'white' : '#a0522d',
  transition: 'all 0.15s',
});

const brandStyle: React.CSSProperties = {
  fontSize: 18, fontWeight: 800, color: '#ff6b35', marginRight: 24, display: 'flex', alignItems: 'center',
};

function App() {
  const [active, setActive] = useState<Tab>('moderation');

  return (
    <div style={{ minHeight: '100vh', background: '#fef9f5' }}>
      <nav style={navStyle}>
        <span style={brandStyle}>CareBridge Admin</span>
        {tabs.map((t) => (
          <button key={t.id} style={tabBtn(active === t.id)} onClick={() => setActive(t.id)}>
            {t.label}
          </button>
        ))}
      </nav>
      <main>
        {active === 'moderation' && <ModerationQueuePage />}
        {active === 'topics' && <ManageTopicsPage />}
        {active === 'content' && <CreateContentPage />}
        {active === 'partner' && <CreatePartnerProfilePage />}
      </main>
    </div>
  );
}

export default App;
