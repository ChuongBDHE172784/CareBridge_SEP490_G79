import { getAuth, signInWithCustomToken, signOut } from 'firebase/auth';
import { collection, getFirestore, limit, onSnapshot, orderBy, query, type Unsubscribe } from 'firebase/firestore';
import {
  fetchFirebaseCustomToken,
  isTerminalFirebaseSignalingError,
} from '../../../features/directChat/services/firebaseTokenApi';
import { getFirebaseWebApp, isFirebaseConfigured } from './firebaseApp';
import { parseConversationEventSignal, type ConversationEventSignal } from './conversationEventSignal';

let firebaseSignInTail: Promise<void> = Promise.resolve();

function serializeFirebaseSignIn<T>(operation: () => Promise<T>): Promise<T> {
  let resolveResult!: (value: T) => void;
  let rejectResult!: (reason: unknown) => void;
  const result = new Promise<T>((resolve, reject) => {
    resolveResult = resolve;
    rejectResult = reject;
  });
  firebaseSignInTail = firebaseSignInTail.catch(() => undefined).then(async () => {
    try {
      resolveResult(await operation());
    } catch (error) {
      rejectResult(error);
    }
  });
  return result;
}

/** Signs in with the backend-issued token, then listens to the caller's Firestore inbox. */
export class ConversationSignalingPort {
  private unsubscribe: Unsubscribe | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private tokenAbortController: AbortController | null = null;
  private disposed = false;
  private onEvent: ((signal: ConversationEventSignal) => void) | null = null;
  private sessionGeneration = 0;
  private attemptGeneration = 0;

  async connect(onEvent: (signal: ConversationEventSignal) => void): Promise<void> {
    const sessionGeneration = ++this.sessionGeneration;
    this.attemptGeneration += 1;
    this.disposed = false;
    this.onEvent = onEvent;
    this.clearConnectionResources();
    if (!isFirebaseConfigured()) {
      // Degrade gracefully — chat delivery is never gated on Firebase (BR-DCC-007-adjacent).
      return;
    }
    await this.establishListener(sessionGeneration);
  }

  private async establishListener(sessionGeneration: number): Promise<void> {
    if (!this.isCurrentSession(sessionGeneration)) return;
    const attemptGeneration = ++this.attemptGeneration;
    const tokenAbortController = new AbortController();
    this.tokenAbortController = tokenAbortController;
    try {
      const capability = await fetchFirebaseCustomToken(tokenAbortController.signal);
      if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return;
      if (!capability.firestoreSignalingEnabled) {
        this.finishTerminalAttempt(sessionGeneration, attemptGeneration);
        return;
      }

      const token = capability.firebaseCustomToken;
      if (typeof token !== 'string' || token.trim().length === 0) {
        this.finishTerminalAttempt(sessionGeneration, attemptGeneration);
        return;
      }

      const app = getFirebaseWebApp();
      const credential = await serializeFirebaseSignIn(async () => {
        if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return null;
        const auth = getAuth(app);
        const signedIn = await signInWithCustomToken(auth, token);
        if (this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return signedIn;
        if (auth.currentUser?.uid === signedIn.user.uid) await signOut(auth);
        return null;
      });
      if (!credential || !this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return;
      const inbox = collection(getFirestore(app), 'userConversationEvents', credential.user.uid, 'events');
      const latestSignal = query(inbox, orderBy('occurredAt', 'desc'), limit(1));
      const unsubscribe = onSnapshot(
        latestSignal,
        (snapshot) => {
          if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return;
          for (const change of snapshot.docChanges()) {
            if (change.type !== 'added') continue;
            const signal = parseConversationEventSignal(change.doc.data());
            if (signal) this.onEvent?.(signal);
          }
        },
        (error) => this.handleFailure(error, sessionGeneration, attemptGeneration)
      );
      if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration)) {
        unsubscribe();
        return;
      }
      this.unsubscribe = unsubscribe;
    } catch (error) {
      this.handleFailure(error, sessionGeneration, attemptGeneration);
    } finally {
      if (this.tokenAbortController === tokenAbortController) this.tokenAbortController = null;
    }
  }

  private handleFailure(
    error: unknown,
    sessionGeneration: number,
    attemptGeneration: number
  ): void {
    if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration)) return;
    if (isTerminalFirebaseSignalingError(error)) {
      this.finishTerminalAttempt(sessionGeneration, attemptGeneration);
      return;
    }
    this.scheduleReconnect(sessionGeneration, attemptGeneration);
  }

  private scheduleReconnect(sessionGeneration: number, attemptGeneration: number): void {
    if (!this.isCurrentAttempt(sessionGeneration, attemptGeneration) || this.reconnectTimer) return;
    this.attemptGeneration += 1;
    this.unsubscribe?.();
    this.unsubscribe = null;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (!this.isCurrentSession(sessionGeneration)) return;
      void this.establishListener(sessionGeneration);
    }, 5000);
  }

  private finishTerminalAttempt(sessionGeneration: number, attemptGeneration: number): void {
    if (this.isCurrentAttempt(sessionGeneration, attemptGeneration)) {
      this.attemptGeneration += 1;
    }
  }

  private isCurrentSession(sessionGeneration: number): boolean {
    return !this.disposed
      && this.onEvent !== null
      && sessionGeneration === this.sessionGeneration;
  }

  private isCurrentAttempt(sessionGeneration: number, attemptGeneration: number): boolean {
    return this.isCurrentSession(sessionGeneration)
      && attemptGeneration === this.attemptGeneration;
  }

  private clearConnectionResources(): void {
    this.tokenAbortController?.abort();
    this.tokenAbortController = null;
    this.unsubscribe?.();
    this.unsubscribe = null;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
  }

  dispose(): void {
    this.disposed = true;
    this.sessionGeneration += 1;
    this.attemptGeneration += 1;
    this.clearConnectionResources();
    this.onEvent = null;
  }
}
