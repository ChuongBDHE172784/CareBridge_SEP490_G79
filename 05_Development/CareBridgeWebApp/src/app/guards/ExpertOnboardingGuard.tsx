import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getExpertOnboarding } from '../../features/expert/services/expertApi';

/** Server-owned gate: direct URLs cannot bypass mandatory expert verification. */
export default function ExpertOnboardingGuard() {
  const location = useLocation();
  const [complete, setComplete] = useState<boolean | null>(null);

  useEffect(() => {
    let active = true;
    getExpertOnboarding()
      .then((state) => { if (active) setComplete(state.nextStep === 'COMPLETE'); })
      .catch(() => { if (active) setComplete(false); });
    return () => { active = false; };
  }, []);

  if (complete === null) {
    return <div className="flex min-h-[320px] items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" /></div>;
  }
  if (!complete) return <Navigate to="/expert/onboarding" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}
