import { getApp, getApps, initializeApp } from 'firebase/app';
import {
  connectAuthEmulator,
  getAuth,
  GoogleAuthProvider,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  signInWithPopup,
  type Auth,
  type ConfirmationResult,
  type User,
} from 'firebase/auth';

let authInstance: Auth | undefined;
let phoneConfirmation: ConfirmationResult | undefined;
let phoneRecaptcha: RecaptchaVerifier | undefined;
let verifiedPhoneIdToken: string | undefined;
let verifiedPhoneUser: User | undefined;
let phoneVerificationGeneration = 0;

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

function clearRecaptcha(verifier = phoneRecaptcha) {
  verifier?.clear();
  if (phoneRecaptcha === verifier) phoneRecaptcha = undefined;
}

/**
 * Starts Firebase Phone Authentication. The SMS code is deliberately handled
 * by the page so it can be entered in an accessible inline form instead of a
 * browser prompt.
 */
export async function sendPhoneVerificationCode(phoneNumber: string): Promise<void> {
  const auth = configuredAuth();
  const generation = ++phoneVerificationGeneration;
  clearRecaptcha();
  const verifier = new RecaptchaVerifier(auth, 'firebase-recaptcha', { size: 'invisible' });
  phoneRecaptcha = verifier;
  try {
    const nextConfirmation = await signInWithPhoneNumber(auth, phoneNumber, verifier);
    clearRecaptcha(verifier);
    if (generation !== phoneVerificationGeneration) {
      throw new Error('PHONE_VERIFICATION_SUPERSEDED');
    }
    phoneConfirmation = nextConfirmation;
    verifiedPhoneIdToken = undefined;
    verifiedPhoneUser = undefined;
  } catch (error) {
    clearRecaptcha(verifier);
    throw error;
  }
}

export async function confirmPhoneVerificationCode(code: string): Promise<string> {
  if (verifiedPhoneIdToken) {
    // Firebase ID tokens are short-lived. The backend exchange may be retried
    // after a transient failure, so refresh the signed-in phone user instead of
    // replaying a potentially stale cached proof.
    const generation = phoneVerificationGeneration;
    const user = verifiedPhoneUser;
    if (user) {
      const refreshedToken = await user.getIdToken(true);
      if (generation !== phoneVerificationGeneration || verifiedPhoneUser !== user) {
        throw new Error('PHONE_VERIFICATION_SUPERSEDED');
      }
      if (refreshedToken) verifiedPhoneIdToken = refreshedToken;
    }
    if (verifiedPhoneIdToken) return verifiedPhoneIdToken;
  }
  if (!phoneConfirmation) throw new Error('PHONE_VERIFICATION_NOT_STARTED');
  const generation = phoneVerificationGeneration;
  const confirmation = phoneConfirmation;
  const credential = await confirmation.confirm(code);
  if (generation !== phoneVerificationGeneration || phoneConfirmation !== confirmation) {
    throw new Error('PHONE_VERIFICATION_SUPERSEDED');
  }
  const idToken = await credential.user.getIdToken(true);
  if (generation !== phoneVerificationGeneration || phoneConfirmation !== confirmation) {
    throw new Error('PHONE_VERIFICATION_SUPERSEDED');
  }
  verifiedPhoneIdToken = idToken;
  verifiedPhoneUser = credential.user;
  phoneConfirmation = undefined;
  clearRecaptcha();
  return idToken;
}

export function clearPhoneVerification(): void {
  phoneVerificationGeneration += 1;
  phoneConfirmation = undefined;
  verifiedPhoneIdToken = undefined;
  verifiedPhoneUser = undefined;
  clearRecaptcha();
}
