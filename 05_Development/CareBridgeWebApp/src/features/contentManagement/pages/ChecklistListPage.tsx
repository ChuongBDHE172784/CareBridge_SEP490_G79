import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchChecklists } from '../services/contentApi';
import type { ChecklistTemplate, ContentStage } from '../models/content';
import { STAGE_LABELS } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock data fallback                                                 */
/* ------------------------------------------------------------------ */
const MOCK_CHECKLISTS: ChecklistTemplate[] = [
  { id: 'c1', name: 'Chuan bi truoc khi mang thai', stage: 'PRE_PREGNANCY', description: 'Danh sach cac xet nghiem va tiem chung can lam truoc khi mang thai', items: [{ id: 'i1', itemText: 'Kham suc khoe tong quat', order: 1, isRequired: true }, { id: 'i2', itemText: 'Tiem vac xin Rubella', order: 2, isRequired: true }, { id: 'i3', itemText: 'Xet nghiem mau', order: 3, isRequired: true }] },
  { id: 'c2', name: 'Theo doi thai ky thang 1-3', stage: 'PREGNANCY', description: 'Cac moc kham thai va xet nghiem quan trong trong tam ca nguyet dau', items: [{ id: 'i4', itemText: 'Kham thai lan 1', order: 1, isRequired: true }, { id: 'i5', itemText: 'Sieu am 12 tuan', order: 2, isRequired: true }, { id: 'i6', itemText: 'Xet nghiem Double test', order: 3, isRequired: true }, { id: 'i7', itemText: 'Bo sung axit folic', order: 4, isRequired: false }] },
  { id: 'c3', name: 'Chuan bi do so sinh', stage: 'POSTPARTUM', description: 'Danh sach do dung can thiet chuan bi cho be so sinh', items: [{ id: 'i8', itemText: 'Quan ao so sinh (10 bo)', order: 1, isRequired: true }, { id: 'i9', itemText: 'Ta dan va ta vai', order: 2, isRequired: true }, { id: 'i10', itemText: 'Binh sua va phu kien', order: 3, isRequired: false }, { id: 'i11', itemText: 'Non, bao tay bao chan', order: 4, isRequired: false }, { id: 'i12', itemText: 'Nem va chan be', order: 5, isRequired: true }] },
  { id: 'c4', name: 'Cham soc be 0-6 thang', stage: 'BABY_CARE', description: 'Huong dan cham soc be so sinh trong 6 thang dau doi', items: [{ id: 'i13', itemText: 'Cho con bu me', order: 1, isRequired: true }, { id: 'i14', itemText: 'Tiem chung theo lich', order: 2, isRequired: true }, { id: 'i15', itemText: 'Kham suc khoe dinh ky', order: 3, isRequired: true }] },
  { id: 'c5', name: 'Phuc hoi sau sinh', stage: 'POSTPARTUM', description: 'Danh sach theo doi qua trinh phuc hoi suc khoe me sau sinh', items: [{ id: 'i16', itemText: 'Kham lai sau 6 tuan', order: 1, isRequired: true }, { id: 'i17', itemText: 'Tap the duc nhe', order: 2, isRequired: false }] },
  { id: 'c6', name: 'Theo doi thai ky thang 4-6', stage: 'PREGNANCY', description: 'Cac moc kham thai va xet nghiem quan trong trong tam ca nguyet 2', items: [{ id: 'i18', itemText: 'Sieu am hinh thai', order: 1, isRequired: true }, { id: 'i19', itemText: 'Xet nghiem duong huyet', order: 2, isRequired: true }, { id: 'i20', itemText: 'Tiem uon van', order: 3, isRequired: true }] },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function stageBadge(stage: ContentStage): { bg: string; color: string } {
  switch (stage) {
    case 'PRE_PREGNANCY': return { bg: '#F5F5F5', color: '#616161' };
    case 'PREGNANCY': return { bg: '#FFF1EC', color: '#845143' };
    case 'POSTPARTUM': return { bg: '#E6F4EA', color: '#137333' };
    case 'BABY_CARE': return { bg: '#FFE9E3', color: '#C98C7B' };
    default: return { bg: '#F5F5F5', color: '#616161' };
  }
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ChecklistListPage() {
  const navigate = useNavigate();
  const [checklists, setChecklists] = useState<ChecklistTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [stageFilter, setStageFilter] = useState<ContentStage | ''>('');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await fetchChecklists(stageFilter || undefined);
      setChecklists(data);
    } catch {
      // Fallback to mock data
      let filtered = MOCK_CHECKLISTS;
      if (stageFilter) filtered = filtered.filter(c => c.stage === stageFilter);
      setChecklists(filtered);
    } finally {
      setIsLoading(false);
    }
  }, [stageFilter]);

  useEffect(() => { loadData(); }, [loadData]);

  const total = checklists.length;
  const totalPages = Math.ceil(total / pageSize);
  const pagedItems = checklists.slice(page * pageSize, (page + 1) * pageSize);

  return (
    <div style={{ padding: 32, fontFamily: "'Lexend', sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 26, fontWeight: 700, color: '#271812', margin: 0 }}>Quan ly Checklist</h1>
          <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>Tao va quan ly cac checklist huong dan cho me va gia dinh</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <select
            value={stageFilter}
            onChange={e => { setStageFilter(e.target.value as ContentStage | ''); setPage(0); }}
            style={{ padding: '12px 16px', borderRadius: 16, border: '1px solid #d6c2bd', background: '#fff', fontSize: 14, color: '#524440', cursor: 'pointer', fontFamily: "'Lexend', sans-serif" }}
          >
            <option value="">Loc</option>
            <option value="PRE_PREGNANCY">Chuan bi</option>
            <option value="PREGNANCY">Thai ky</option>
            <option value="POSTPARTUM">Sau sinh</option>
            <option value="BABY_CARE">Cham be</option>
          </select>
          <button
            onClick={() => navigate('/content/create?type=CHECKLIST')}
            style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 24px', borderRadius: 9999, background: '#C98C7B', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
            Tao Checklist
          </button>
        </div>
      </div>

      {/* Data table */}
      <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Dang tai...</div>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                  {['TIEU DE', 'GIAI DOAN', 'SO MUC', 'TRANG THAI', 'CAP NHAT LAN CUOI', 'THAO TAC'].map(h => (
                    <th key={h} style={{ padding: '12px 8px', fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {pagedItems.map(cl => {
                  const badge = stageBadge(cl.stage);
                  // Derive published status from whether items exist
                  const isPublished = cl.items.length > 0;
                  return (
                    <tr
                      key={cl.id}
                      style={{ borderBottom: '1px solid #fadcd3' }}
                      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#FFF8F6'; }}
                      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                    >
                      <td style={{ padding: '14px 8px', maxWidth: 320 }}>
                        <div style={{ fontWeight: 600, fontSize: 14, color: '#271812' }}>{cl.name}</div>
                        <div style={{ fontSize: 12, color: '#84736f', marginTop: 2 }}>{cl.description}</div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <span style={{ padding: '4px 14px', borderRadius: 9999, background: badge.bg, color: badge.color, fontSize: 12, fontWeight: 600 }}>
                          {STAGE_LABELS[cl.stage]}
                        </span>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>checklist</span>
                          <span style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>{cl.items.length}</span>
                          <span style={{ fontSize: 12, color: '#84736f' }}>muc</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        {isPublished ? (
                          <span style={{ padding: '4px 14px', borderRadius: 9999, background: '#E6F4EA', color: '#137333', fontSize: 12, fontWeight: 600 }}>Published</span>
                        ) : (
                          <span style={{ padding: '4px 14px', borderRadius: 9999, background: '#F5F5F5', color: '#616161', fontSize: 12, fontWeight: 600 }}>Draft</span>
                        )}
                      </td>
                      <td style={{ padding: '14px 8px', fontSize: 13, color: '#84736f' }}>—</td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button
                            onClick={() => navigate(`/content/${cl.id}`)}
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Xem chi tiet"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>visibility</span>
                          </button>
                          <button
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Chinh sua"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>edit</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {pagedItems.length === 0 && (
                  <tr><td colSpan={6} style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Khong co checklist nao.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 20, paddingTop: 16, borderTop: '1px solid #fadcd3' }}>
              <span style={{ fontSize: 13, color: '#84736f' }}>
                Hien thi {total === 0 ? 0 : page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} tren {total} ket qua
              </span>
              <div style={{ display: 'flex', gap: 4 }}>
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  style={{ width: 36, height: 36, borderRadius: 9999, border: '1px solid #d6c2bd', background: '#fff', cursor: page === 0 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page === 0 ? 0.4 : 1 }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      style={{ width: 36, height: 36, borderRadius: 9999, border: page === p ? 'none' : '1px solid #d6c2bd', background: page === p ? '#C98C7B' : '#fff', color: page === p ? '#fff' : '#524440', fontSize: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  style={{ width: 36, height: 36, borderRadius: 9999, border: '1px solid #d6c2bd', background: '#fff', cursor: page >= totalPages - 1 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page >= totalPages - 1 ? 0.4 : 1 }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
