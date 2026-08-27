import { useEffect, useState } from 'react';
import apiClient from '../../../shared/api/apiClient';

type Baby = { id: string; nickname: string; isActive?: boolean };
type Counts = { journal: number; growth: number; milestones: number; vaccinations: number };

export default function BabyCareHubPage() {
  const [babies, setBabies] = useState<Baby[]>([]);
  const [activeBaby, setActiveBaby] = useState<Baby | null>(null);
  const [counts, setCounts] = useState<Counts | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadCounts = async (babyId: string) => {
    const [journal, growth, milestones, vaccinations] = await Promise.all([
      apiClient.get(`/api/v1/babies/${babyId}/daily-logs`),
      apiClient.get(`/api/v1/babies/${babyId}/growth-measurements?page=0&size=20`),
      apiClient.get(`/api/v1/babies/${babyId}/milestones`),
      apiClient.get(`/api/v1/vaccination/babies/${babyId}/records`),
    ]);
    setCounts({
      journal: journal.data?.data?.length ?? 0,
      growth: growth.data?.data?.content?.length ?? 0,
      milestones: milestones.data?.data?.length ?? 0,
      vaccinations: vaccinations.data?.data?.length ?? 0,
    });
  };

  useEffect(() => {
    apiClient.get('/api/v1/babies')
      .then((response) => {
        const rows = (response.data?.data ?? []) as Baby[];
        setBabies(rows);
        const selected = rows.find((baby) => baby.isActive) ?? rows[0];
        setActiveBaby(selected ?? null);
        if (selected) return loadCounts(selected.id);
        return undefined;
      })
      .catch(() => setError('Unable to load baby-care data.'));
  }, []);

  const selectBaby = async (nickname: string) => {
    const selected = babies.find((baby) => baby.nickname === nickname);
    if (!selected) return;
    setActiveBaby(selected);
    setError(null);
    try {
      await apiClient.patch(`/api/v1/babies/${selected.id}/active`, {});
      await loadCounts(selected.id);
    } catch {
      setError('Unable to refresh this baby-care view.');
    }
  };

  return (
    <main className="min-h-screen bg-[#F6F1EC] p-6 font-sans text-[#5A463F] md:p-10">
      <section className="mx-auto max-w-6xl space-y-8">
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#9C857C]">CareBridge</p>
            <h1 className="text-3xl font-black md:text-4xl">Baby care hub</h1>
          </div>
          <label className="flex items-center gap-3 rounded-full bg-white px-5 py-3 shadow-[0_12px_32px_rgba(90,70,63,0.06)]">
            <span className="font-semibold">Active baby</span>
            <select
              aria-label="Switch baby"
              value={activeBaby?.nickname ?? ''}
              onChange={(event) => selectBaby(event.target.value)}
              className="rounded-xl border border-[#E8DDD6] bg-[#F6F1EC] px-3 py-2 font-bold focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20"
            >
              {babies.map((baby) => <option key={baby.id}>{baby.nickname}</option>)}
            </select>
          </label>
        </header>

        {error && <p role="alert" className="rounded-2xl bg-red-50 p-4 text-red-800">{error}</p>}
        <p data-testid="active-baby-name" className="text-lg font-bold">Showing data for {activeBaby?.nickname ?? '—'}</p>
        <div className="grid gap-6 md:grid-cols-2">
          {[
            ['baby-care-journal', 'Journal', 'Recent observations and daily notes'],
            ['baby-care-growth', 'Growth', 'Recorded measurements and history'],
            ['baby-care-milestones', 'Milestones', 'Observed development milestones'],
            ['baby-care-vaccinations', 'Vaccinations', 'Recorded doses and schedule'],
          ].map(([testId, title, description]) => (
            <article key={testId} data-testid={testId} className="rounded-[32px] border border-[#E8DDD6]/50 bg-white p-7 shadow-[0_12px_32px_rgba(90,70,63,0.06)]">
              <h2 className="text-2xl font-black">{title}</h2>
              <p className="mt-2 text-base text-[#9C857C]">{description}</p>
              <p className="mt-6 rounded-full bg-[#F2EAE4] px-4 py-2 text-sm font-bold">{activeBaby?.nickname ?? '—'}</p>
              {counts && <p className="mt-2 text-sm font-bold">{counts[testId.replace('baby-care-', '') as keyof Counts] ?? 0} records</p>}
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
