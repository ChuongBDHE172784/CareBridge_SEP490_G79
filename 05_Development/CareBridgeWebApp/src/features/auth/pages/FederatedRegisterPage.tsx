import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { federatedAuthenticate } from '../services/authApi';
import { googleIdToken, phoneIdToken } from '../services/firebaseAuth';
import { useAuthStore } from '../../../shared/auth/authStore';

export default function FederatedRegisterPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState<'google' | 'phone' | null>(null);
  const [message, setMessage] = useState('');

  const register = async (provider: 'google' | 'phone') => {
    setLoading(provider);
    setMessage('');
    try {
      const token = provider === 'google'
        ? await googleIdToken()
        : await phoneIdToken(window.prompt('Phone number including country code (for example +84)') ?? '');
      const result = await federatedAuthenticate(token);
      useAuthStore.getState().setTokens(result.accessToken, result.refreshToken);
      navigate('/account/profile', { replace: true });
    } catch (error) {
      if (error instanceof Error && (error.message.includes('popup-closed') || error.message === 'AUTH_CANCELLED')) return;
      setMessage('Unable to create the account. Use your existing sign-in method if this contact is already registered.');
    } finally {
      setLoading(null);
    }
  };

  return (
    <main className="min-h-screen bg-[#F6F1EC] text-[#5A463F] flex items-center justify-center p-6">
      <section className="w-full max-w-md bg-white rounded-[32px] p-8 shadow-[0_12px_32px_rgba(90,70,63,0.06)] flex flex-col gap-4">
        <h1 className="text-2xl font-black">Create your CareBridge account</h1>
        <p>Choose your CareBridge role after your identity is verified.</p>
        <p role="status" aria-live="polite" className={message ? 'rounded-2xl bg-[#F2EAE4] p-4' : 'sr-only'}>{message}</p>
        <button type="button" disabled={loading !== null} onClick={() => register('google')}
          className="h-12 rounded-full bg-[#C98C7B] text-white font-semibold focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20">
          {loading === 'google' ? 'Creating account...' : 'Sign up with Google'}
        </button>
        <button type="button" disabled={loading !== null} onClick={() => register('phone')}
          className="h-12 rounded-full bg-[#F2EAE4] text-[#5A463F] font-semibold focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20">
          {loading === 'phone' ? 'Sending code...' : 'Sign up with phone'}
        </button>
        <div id="firebase-recaptcha" />
      </section>
    </main>
  );
}
