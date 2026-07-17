import { getApp, getApps, initializeApp } from 'firebase/app';
import {
  connectAuthEmulator,
  getAuth,
  GoogleAuthProvider,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  signInWithPopup,
  type Auth,
} from 'firebase/auth';

let authInstance: Auth | undefined;

function configuredAuth() {
  if (authInstance) return authInstance;

  const apiKey = import.meta.env.VITE_FIREBASE_API_KEY;
  if (!apiKey) throw new Error('Firebase web configuration is missing');
  const app = getApps().length
    ? getApp()
    : initializeApp({
        apiKey,
        authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
        projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
        appId: import.meta.env.VITE_FIREBASE_APP_ID,
      });
  const auth = getAuth(app);
  const emulatorUrl = import.meta.env.VITE_FIREBASE_AUTH_EMULATOR_URL;
  if (emulatorUrl) {
    if (!import.meta.env.DEV) {
      throw new Error('Firebase Auth Emulator is only allowed in development mode');
    }
    connectAuthEmulator(auth, emulatorUrl, { disableWarnings: true });
  }

  authInstance = auth;
  return auth;
}

export async function googleIdToken(): Promise<string> {
  const auth = configuredAuth();
  const credential = await signInWithPopup(auth, new GoogleAuthProvider());
  return credential.user.getIdToken();
}

export async function phoneIdToken(phoneNumber: string): Promise<string> {
  const auth = configuredAuth();
  const verifier = new RecaptchaVerifier(auth, 'firebase-recaptcha', { size: 'invisible' });
  try {
    const confirmation = await signInWithPhoneNumber(auth, phoneNumber, verifier);
    const otp = window.prompt('Enter the SMS verification code');
    if (!otp) throw new Error('AUTH_CANCELLED');
    const credential = await confirmation.confirm(otp);
    return credential.user.getIdToken();
  } finally {
    verifier.clear();
  }
}
